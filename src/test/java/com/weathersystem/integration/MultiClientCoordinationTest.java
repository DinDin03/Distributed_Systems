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
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class MultiClientCoordinationTest {

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
    void testMultipleContentServersSendingSimultaneously() throws Exception {
        final int numContentServers = 5;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(numContentServers);
        final AtomicInteger successCount = new AtomicInteger(0);
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Create test data files for each content server
        List<String> weatherFiles = new ArrayList<>();
        for (int i = 0; i < numContentServers; i++) {
            weatherFiles.add(createTestWeatherDataFile(i));
        }

        // Start multiple content servers
        for (int i = 0; i < numContentServers; i++) {
            final int serverId = i;
            final String weatherFile = weatherFiles.get(i);

            Thread contentServerThread = new Thread(() -> {
                try {
                    ContentServer contentServer = new ContentServer(clientConfig);

                    // Wait for start signal
                    startLatch.await();

                    // Send weather data
                    contentServer.publishWeatherData(weatherFile);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    completionLatch.countDown();
                }
            });
            contentServerThread.setDaemon(true);
            contentServerThread.start();
        }

        // Start all content servers simultaneously
        startLatch.countDown();

        // Wait for all to complete
        assertTrue(completionLatch.await(30, TimeUnit.SECONDS),
                  "All content servers should complete within timeout");

        // Verify results
        if (!exceptions.isEmpty()) {
            System.out.println("Exceptions occurred: " + exceptions.size());
            for (Exception e : exceptions) {
                e.printStackTrace();
            }
        }

        assertTrue(successCount.get() >= numContentServers * 0.8,
                  "At least 80% of content servers should succeed");

        // Verify server has latest data
        GETClient getClient = new GETClient(clientConfig);
        WeatherData[] finalData = getClient.retrieveWeatherData();
        assertNotNull(finalData, "Should receive data from server");
        assertTrue(finalData.length > 0, "Server should have data from content servers");
    }

    @Test
    void testMultipleGETClientsRetrievingSimultaneously() throws Exception {
        // First, publish some data
        String weatherFile = createTestWeatherDataFile(0);
        ContentServer contentServer = new ContentServer(clientConfig);
        contentServer.publishWeatherData(weatherFile);

        final int numGetClients = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(numGetClients);
        final AtomicInteger successCount = new AtomicInteger(0);
        final List<WeatherData[]> retrievedDataList = Collections.synchronizedList(new ArrayList<>());
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Start multiple GET clients
        for (int i = 0; i < numGetClients; i++) {
            Thread getClientThread = new Thread(() -> {
                try {
                    GETClient getClient = new GETClient(clientConfig);

                    // Wait for start signal
                    startLatch.await();

                    // Retrieve weather data
                    WeatherData[] data = getClient.retrieveWeatherData();
                    retrievedDataList.add(data);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    completionLatch.countDown();
                }
            });
            getClientThread.setDaemon(true);
            getClientThread.start();
        }

        // Start all GET clients simultaneously
        startLatch.countDown();

        // Wait for all to complete
        assertTrue(completionLatch.await(20, TimeUnit.SECONDS),
                  "All GET clients should complete within timeout");

        // Verify results
        if (!exceptions.isEmpty()) {
            System.out.println("Exceptions occurred: " + exceptions.size());
            for (Exception e : exceptions) {
                e.printStackTrace();
            }
        }

        assertEquals(numGetClients, successCount.get(), "All GET clients should succeed");

        // Verify all clients received consistent data
        int expectedStations = retrievedDataList.get(0).length; // Use first result as baseline
        for (WeatherData[] data : retrievedDataList) {
            assertNotNull(data, "All clients should receive non-null data");
            assertEquals(expectedStations, data.length, "All clients should receive same amount of data");
            assertTrue(data.length > 0, "Should have at least one station");
        }

        // Verify all clients received identical data
        WeatherData[] baseline = retrievedDataList.get(0);
        for (WeatherData[] data : retrievedDataList) {
            for (int i = 0; i < baseline.length; i++) {
                assertEquals(baseline[i].getId(), data[i].getId(), "All clients should receive consistent station IDs");
            }
        }
    }

    @Test
    void testConcurrentPUTAndGETOperations() throws Exception {
        final int numOperations = 20;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(numOperations);
        final AtomicInteger putSuccessCount = new AtomicInteger(0);
        final AtomicInteger getSuccessCount = new AtomicInteger(0);
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Create mixed PUT and GET operations
        for (int i = 0; i < numOperations; i++) {
            final int operationId = i;

            Thread operationThread = new Thread(() -> {
                try {
                    // Wait for start signal
                    startLatch.await();

                    if (operationId % 2 == 0) {
                        // PUT operation
                        String weatherFile = createTestWeatherDataFile(operationId);
                        ContentServer contentServer = new ContentServer(clientConfig);
                        contentServer.publishWeatherData(weatherFile);
                        putSuccessCount.incrementAndGet();
                    } else {
                        // GET operation
                        GETClient getClient = new GETClient(clientConfig);
                        WeatherData[] data = getClient.retrieveWeatherData();
                        assertNotNull(data, "GET should return non-null data");
                        getSuccessCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    completionLatch.countDown();
                }
            });
            operationThread.setDaemon(true);
            operationThread.start();
        }

        // Start all operations simultaneously
        startLatch.countDown();

        // Wait for all to complete
        assertTrue(completionLatch.await(30, TimeUnit.SECONDS),
                  "All operations should complete within timeout");

        // Verify results
        if (!exceptions.isEmpty()) {
            System.out.println("Exceptions occurred: " + exceptions.size());
            for (Exception e : exceptions) {
                e.printStackTrace();
            }
        }

        assertTrue(putSuccessCount.get() >= (numOperations / 2) * 0.8,
                  "Most PUT operations should succeed");
        assertTrue(getSuccessCount.get() >= (numOperations / 2) * 0.8,
                  "Most GET operations should succeed");
    }

    @Test
    void testDataConsistencyUnderConcurrentUpdates() throws Exception {
        final int numUpdaters = 5;
        final int updatesPerUpdater = 3;
        final CountDownLatch completionLatch = new CountDownLatch(numUpdaters);
        final AtomicReference<String> lastUpdateId = new AtomicReference<>();

        // Start multiple updaters
        for (int i = 0; i < numUpdaters; i++) {
            final int updaterId = i;

            Thread updaterThread = new Thread(() -> {
                try {
                    ContentServer contentServer = new ContentServer(clientConfig);

                    for (int j = 0; j < updatesPerUpdater; j++) {
                        String weatherFile = createTestWeatherDataFile(updaterId * 100 + j);
                        contentServer.publishWeatherData(weatherFile);
                        lastUpdateId.set("TEST" + String.format("%03d", updaterId * 100 + j));

                        // Small delay between updates
                        Thread.sleep(100);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    completionLatch.countDown();
                }
            });
            updaterThread.setDaemon(true);
            updaterThread.start();
        }

        // Wait for all updaters to complete
        assertTrue(completionLatch.await(20, TimeUnit.SECONDS),
                  "All updaters should complete within timeout");

        // Give server time to process all updates
        Thread.sleep(1000);

        // Verify final state is consistent
        GETClient getClient = new GETClient(clientConfig);
        WeatherData[] finalData = getClient.retrieveWeatherData();

        assertNotNull(finalData, "Should receive final data");
        assertTrue(finalData.length > 0, "Should have data from stations");

        // Verify the data includes expected updates (may have pre-existing data too)
        boolean foundTestData = false;
        for (WeatherData station : finalData) {
            if (station.getId().startsWith("TEST")) {
                foundTestData = true;
                break;
            }
        }
        assertTrue(foundTestData, "Should find at least one TEST station from updates");
    }

    @Test
    void testServerHandlesClientDisconnections() throws Exception {
        final int numClients = 5;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch connectionLatch = new CountDownLatch(numClients);
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Start clients that will disconnect abruptly
        for (int i = 0; i < numClients; i++) {
            final int clientId = i;

            Thread clientThread = new Thread(() -> {
                try {
                    GETClient getClient = new GETClient(clientConfig);

                    // Wait for start signal
                    startLatch.await();

                    if (clientId % 2 == 0) {
                        // Normal operation
                        getClient.retrieveWeatherData();
                    } else {
                        // Simulate abrupt disconnection by creating connection and closing immediately
                        Socket socket = new Socket("localhost", serverPort);
                        socket.close(); // Abrupt disconnection
                    }

                } catch (Exception e) {
                    // Expected for abrupt disconnections
                    exceptions.add(e);
                } finally {
                    connectionLatch.countDown();
                }
            });
            clientThread.setDaemon(true);
            clientThread.start();
        }

        // Start all clients
        startLatch.countDown();

        // Wait for all operations to complete
        assertTrue(connectionLatch.await(15, TimeUnit.SECONDS),
                  "All client operations should complete within timeout");

        // Server should continue to work after client disconnections
        GETClient getClient = new GETClient(clientConfig);
        assertDoesNotThrow(() -> {
            getClient.retrieveWeatherData();
        }, "Server should continue working after client disconnections");
    }

    @Test
    void testSequentialOperationsFromMultipleClients() throws Exception {
        final int numClients = 3;
        final int operationsPerClient = 3;

        // Perform operations sequentially from multiple clients
        for (int clientId = 0; clientId < numClients; clientId++) {
            ContentServer contentServer = new ContentServer(clientConfig);
            GETClient getClient = new GETClient(clientConfig);

            for (int opId = 0; opId < operationsPerClient; opId++) {
                // PUT operation
                String weatherFile = createTestWeatherDataFile(clientId * 100 + opId);
                contentServer.publishWeatherData(weatherFile);

                // GET operation to verify
                WeatherData[] data = getClient.retrieveWeatherData();
                assertNotNull(data, "Should receive data after PUT");
                assertTrue(data.length > 0, "Should have at least one station");

                // Verify that our test data is present
                String expectedId = "TEST" + String.format("%03d", clientId * 100 + opId);
                boolean foundExpected = false;
                for (WeatherData station : data) {
                    if (expectedId.equals(station.getId())) {
                        foundExpected = true;
                        break;
                    }
                }
                assertTrue(foundExpected, "Should find data from latest PUT: " + expectedId);
            }
        }
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private String createTestWeatherDataFile(int id) throws IOException {
        String weatherContent =
                "id:TEST" + String.format("%03d", id) + "\n" +
                "name:Test Station " + id + "\n" +
                "state:TEST\n" +
                "time_zone:UTC\n" +
                "lat:-35." + (id % 10) + "\n" +
                "lon:138." + (id % 10) + "\n" +
                "local_date_time:15/04:00pm\n" +
                "local_date_time_full:20230715160000\n" +
                "air_temp:" + (20.0 + id) + "\n" +
                "apparent_t:" + (18.5 + id) + "\n" +
                "cloud:Clear\n" +
                "dewpt:10.0\n" +
                "press:1013.0\n" +
                "rel_hum:60\n" +
                "wind_dir:N\n" +
                "wind_spd_kmh:10\n" +
                "wind_spd_kt:5\n";

        Path weatherFile = tempDir.resolve("test_weather_" + id + ".txt");
        Files.writeString(weatherFile, weatherContent);
        return weatherFile.toString();
    }
}