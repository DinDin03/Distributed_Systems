package com.weathersystem.integration;

import com.weathersystem.client.ContentServer;
import com.weathersystem.client.GETClient;
import com.weathersystem.client.common.ClientConfiguration;
import com.weathersystem.server.AggregationServer;
import com.weathersystem.shared.domain.WeatherData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.awaitility.Awaitility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class LamportClockOrderingTest {

    @TempDir
    Path tempDir;

    private AggregationServer server;
    private Thread serverThread;
    private int serverPort;
    private ClientConfiguration clientConfig;

    @BeforeEach
    void setUp() throws IOException {
        // Find available port
        serverPort = findAvailablePort();

        // Clear any existing data files to ensure clean test state
        clearExistingDataFiles();

        // Initialize server
        server = new AggregationServer();

        // Start server in background thread
        serverThread = new Thread(() -> {
            try {
                server.start(serverPort);
            } catch (Exception e) {
                System.err.println("Server failed to start: " + e.getMessage());
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Wait for server to be ready
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .ignoreExceptions()
                .until(() -> {
                    try (Socket testSocket = new Socket("localhost", serverPort)) {
                        return true;
                    }
                });

        // Create client configuration
        clientConfig = new ClientConfiguration("localhost", serverPort, "WeatherTestClient/1.0", 5000, 10000);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (server != null) {
            server.shutdown();
        }

        if (serverThread != null) {
            serverThread.interrupt();
            serverThread.join(2000);
        }
    }

    @Test
    void testLamportClockUpdateBetweenClientAndServer() throws Exception {
        GETClient getClient = new GETClient(clientConfig);

        // Initial client Lamport time should be 0
        long initialTime = getClient.getLamportTime();
        assertEquals(0, initialTime, "Client should start with Lamport time 0");

        // Perform GET operation
        getClient.retrieveWeatherData();

        // Client Lamport time should have increased
        long finalTime = getClient.getLamportTime();
        assertTrue(finalTime > initialTime, "Client Lamport time should increase after server communication");
        assertTrue(finalTime > 0, "Client Lamport time should be positive after operation");

        System.out.println("Client Lamport time: " + initialTime + " -> " + finalTime);
    }

    @Test
    void testLamportClockOrderingWithMultipleOperations() throws Exception {
        ContentServer contentServer = new ContentServer(clientConfig);
        GETClient getClient = new GETClient(clientConfig);

        List<Long> lamportTimes = new ArrayList<>();

        // Perform multiple sequential operations and track Lamport times
        for (int i = 0; i < 5; i++) {
            if (i % 2 == 0) {
                // PUT operation - track ContentServer's clock
                long beforeTime = contentServer.getLamportTime();
                System.out.println("Before PUT operation " + i + ": " + beforeTime);
                
                String weatherFile = createTestWeatherDataFile("ORDER" + i, "Order Test " + i);
                contentServer.publishWeatherData(weatherFile);
                
                long afterTime = contentServer.getLamportTime();
                System.out.println("After PUT operation " + i + ": " + afterTime);
                lamportTimes.add(afterTime);
                
                System.out.println("PUT Operation " + i + ": " + beforeTime + " -> " + afterTime);
                assertTrue(afterTime > beforeTime, "Lamport time should increase with each PUT operation. Before: " + beforeTime + ", After: " + afterTime);
            } else {
                // GET operation - track GETClient's clock
                long beforeTime = getClient.getLamportTime();
                System.out.println("Before GET operation " + i + ": " + beforeTime);
                
                getClient.retrieveWeatherData();
                
                long afterTime = getClient.getLamportTime();
                System.out.println("After GET operation " + i + ": " + afterTime);
                lamportTimes.add(afterTime);
                
                System.out.println("GET Operation " + i + ": " + beforeTime + " -> " + afterTime);
                assertTrue(afterTime > beforeTime, "Lamport time should increase with each GET operation. Before: " + beforeTime + ", After: " + afterTime);
            }
        }

        // Verify Lamport times are monotonically increasing
        for (int i = 1; i < lamportTimes.size(); i++) {
            assertTrue(lamportTimes.get(i) > lamportTimes.get(i - 1),
                      "Lamport times should be monotonically increasing");
        }
    }

    @Test
    void testConcurrentOperationsLamportOrdering() throws Exception {
        final int numClients = 5;
        final int operationsPerClient = 3;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(numClients);
        final List<Long> allLamportTimes = Collections.synchronizedList(new ArrayList<>());
        final AtomicInteger operationCounter = new AtomicInteger(0);

        // Start multiple clients performing operations concurrently
        for (int clientId = 0; clientId < numClients; clientId++) {
            final int cId = clientId;
            Thread clientThread = new Thread(() -> {
                try {
                    ContentServer contentServer = new ContentServer(clientConfig);
                    GETClient getClient = new GETClient(clientConfig);

                    // Wait for start signal
                    startLatch.await();

                    for (int opId = 0; opId < operationsPerClient; opId++) {
                        if (opId % 2 == 0) {
                            // PUT operation - use ContentServer's clock
                            long beforeTime = contentServer.getLamportTime();
                            int operationId = operationCounter.getAndIncrement();
                            String weatherFile = createTestWeatherDataFile("CONCURRENT" + operationId,
                                                                         "Concurrent Client " + cId + " Op " + opId);
                            contentServer.publishWeatherData(weatherFile);
                            long afterTime = contentServer.getLamportTime();
                            allLamportTimes.add(afterTime);
                        } else {
                            // GET operation - use GETClient's clock
                            long beforeTime = getClient.getLamportTime();
                            getClient.retrieveWeatherData();
                            long afterTime = getClient.getLamportTime();
                            allLamportTimes.add(afterTime);
                        }

                        // Small delay to allow interleaving
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    completionLatch.countDown();
                }
            });
            clientThread.setDaemon(true);
            clientThread.start();
        }

        // Start all clients
        startLatch.countDown();

        // Wait for completion
        assertTrue(completionLatch.await(30, TimeUnit.SECONDS),
                  "All clients should complete within timeout");

        // Analyze Lamport time distribution
        System.out.println("Collected " + allLamportTimes.size() + " Lamport times from concurrent operations");

        // All times should be positive and show reasonable progression
        for (Long time : allLamportTimes) {
            System.out.println("Lamport time: " + time);
            assertTrue(time > 0, "All Lamport times should be positive, but got: " + time);
        }

        // The maximum time should be reasonable (not excessively high)
        Long maxTime = Collections.max(allLamportTimes);
        assertTrue(maxTime < 1000, "Maximum Lamport time should be reasonable: " + maxTime);
    }

    @Test
    void testLamportClockConsistencyAcrossRequests() throws Exception {
        GETClient getClient = new GETClient(clientConfig);

        // Perform multiple GET operations to observe Lamport clock progression
        List<Long> clientTimes = new ArrayList<>();
        List<Long> serverTimes = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            long beforeTime = getClient.getLamportTime();

            // Perform operation
            WeatherData[] data = getClient.retrieveWeatherData();

            long afterTime = getClient.getLamportTime();

            clientTimes.add(afterTime);

            System.out.println("Request " + i + ": Client " + beforeTime + " -> " + afterTime);

            // Small delay between requests
            Thread.sleep(100);
        }

        // Verify client times are monotonically increasing
        for (int i = 1; i < clientTimes.size(); i++) {
            assertTrue(clientTimes.get(i) > clientTimes.get(i - 1),
                      "Client Lamport times should be monotonically increasing");
        }

        System.out.println("Final client Lamport time: " + clientTimes.get(clientTimes.size() - 1));
    }

    @Test
    void testLamportOrderingWithDelayedRequests() throws Exception {
        ContentServer contentServer = new ContentServer(clientConfig);
        GETClient getClient = new GETClient(clientConfig);

        // Send requests with varying delays to test ordering under different timing conditions
        List<Long> timestamps = new ArrayList<>();

        // First batch - quick succession
        for (int i = 0; i < 3; i++) {
            getClient.retrieveWeatherData();
            timestamps.add(getClient.getLamportTime());
            Thread.sleep(50);
        }

        // Longer delay
        Thread.sleep(1000);

        // Second batch - with PUT operations
        for (int i = 0; i < 3; i++) {
            String weatherFile = createTestWeatherDataFile("DELAYED" + i, "Delayed Test " + i);
            contentServer.publishWeatherData(weatherFile);
            timestamps.add(getClient.getLamportTime());
            Thread.sleep(200);
        }

        // Final GET operations
        for (int i = 0; i < 2; i++) {
            getClient.retrieveWeatherData();
            timestamps.add(getClient.getLamportTime());
            Thread.sleep(100);
        }

        // Verify all timestamps are in increasing order
        for (int i = 1; i < timestamps.size(); i++) {
            assertTrue(timestamps.get(i) >= timestamps.get(i - 1),
                      "Timestamps should be non-decreasing even with delays");
        }

        System.out.println("Timestamp progression with delays: " + timestamps);
    }

    @Test
    void testLamportClockSynchronizationBetweenMultipleClients() throws Exception {
        final int numClients = 3;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(numClients);
        final AtomicLong maxObservedTime = new AtomicLong(0);
        final List<Long> finalTimes = Collections.synchronizedList(new ArrayList<>());

        // Create multiple clients that will interact with the server
        for (int clientId = 0; clientId < numClients; clientId++) {
            final int cId = clientId;
            Thread clientThread = new Thread(() -> {
                try {
                    GETClient getClient = new GETClient(clientConfig);

                    // Wait for start signal
                    startLatch.await();

                    // Each client performs several operations
                    for (int i = 0; i < 5; i++) {
                        getClient.retrieveWeatherData();
                        long currentTime = getClient.getLamportTime();
                        maxObservedTime.updateAndGet(max -> Math.max(max, currentTime));

                        Thread.sleep(100 + (cId * 50)); // Staggered timing
                    }

                    finalTimes.add(getClient.getLamportTime());
                    System.out.println("Client " + cId + " final time: " + getClient.getLamportTime());

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    completionLatch.countDown();
                }
            });
            clientThread.setDaemon(true);
            clientThread.start();
        }

        // Start all clients
        startLatch.countDown();

        // Wait for completion
        assertTrue(completionLatch.await(20, TimeUnit.SECONDS),
                  "All clients should complete within timeout");

        // Verify that all clients have reasonable final times
        for (Long finalTime : finalTimes) {
            assertTrue(finalTime > 0, "All clients should have positive final times");
            assertTrue(finalTime <= maxObservedTime.get() * 2,
                      "Final times should be within reasonable bounds");
        }

        System.out.println("Max observed time across all clients: " + maxObservedTime.get());
        System.out.println("Final times: " + finalTimes);
    }

    @Test
    void testLamportClockResetBehavior() throws Exception {
        // Test that each new client starts with Lamport time 0
        for (int i = 0; i < 5; i++) {
            GETClient newClient = new GETClient(clientConfig);
            long initialTime = newClient.getLamportTime();
            assertEquals(0, initialTime, "Each new client should start with Lamport time 0");

            // Perform one operation
            newClient.retrieveWeatherData();
            long afterTime = newClient.getLamportTime();
            assertTrue(afterTime > 0, "Time should increase after operation");

            System.out.println("New client " + i + " times: " + initialTime + " -> " + afterTime);
        }
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private void clearExistingDataFiles() {
        try {
            // Clear the main data file
            Path dataFile = Path.of("data/weather.json");
            if (Files.exists(dataFile)) {
                Files.delete(dataFile);
            }
            
            // Clear the backup file
            Path backupFile = Path.of("data/weather.json.backup");
            if (Files.exists(backupFile)) {
                Files.delete(backupFile);
            }
        } catch (IOException e) {
            System.out.println("Warning: Could not clear existing data files: " + e.getMessage());
        }
    }

    private String createTestWeatherDataFile(String id, String name) throws IOException {
        String weatherContent =
                "id:" + id + "\n" +
                "name:" + name + "\n" +
                "state:TEST\n" +
                "time_zone:UTC\n" +
                "lat:-35.0\n" +
                "lon:138.0\n" +
                "local_date_time:15/04:00pm\n" +
                "local_date_time_full:20230715160000\n" +
                "air_temp:22.0\n" +
                "apparent_t:20.0\n" +
                "cloud:Clear\n" +
                "dewpt:11.0\n" +
                "press:1014.0\n" +
                "rel_hum:58\n" +
                "wind_dir:E\n" +
                "wind_spd_kmh:12\n" +
                "wind_spd_kt:6\n";

        Path weatherFile = tempDir.resolve("lamport_" + id + ".txt");
        Files.writeString(weatherFile, weatherContent);
        return weatherFile.toString();
    }
}