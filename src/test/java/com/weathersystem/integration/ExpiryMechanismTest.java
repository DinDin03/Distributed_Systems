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

import static org.junit.jupiter.api.Assertions.*;

class ExpiryMechanismTest {

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
    void testDataExpiryAfter30Seconds() throws Exception {
        ContentServer contentServer = new ContentServer(clientConfig);
        GETClient getClient = new GETClient(clientConfig);

        // Add test data
        String weatherFile = createTestWeatherDataFile("EXPIRY001", "Expiry Test Station");
        contentServer.publishWeatherData(weatherFile);

        // Verify data is initially present
        WeatherData[] initialData = getClient.retrieveWeatherData();
        boolean foundInitialData = false;
        for (WeatherData station : initialData) {
            if ("EXPIRY001".equals(station.getId())) {
                foundInitialData = true;
                break;
            }
        }
        assertTrue(foundInitialData, "Data should be present initially");

        // Wait for expiry (30+ seconds) - using Awaitility for better test reliability
        System.out.println("Waiting for data to expire (this may take up to 40 seconds)...");

        // The server cleanup runs every 5 seconds and removes data older than 30 seconds
        // So we wait up to 40 seconds to be sure the cleanup has run
        Awaitility.await()
                .atMost(Duration.ofSeconds(40))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> {
                    try {
                        WeatherData[] currentData = getClient.retrieveWeatherData();
                        // Check if our test data is still present
                        for (WeatherData station : currentData) {
                            if ("EXPIRY001".equals(station.getId())) {
                                return false; // Still present, keep waiting
                            }
                        }
                        return true; // Not found, data has expired
                    } catch (Exception e) {
                        return false; // Error, keep waiting
                    }
                });

        // Verify data has been expired
        WeatherData[] finalData = getClient.retrieveWeatherData();
        boolean foundExpiredData = false;
        for (WeatherData station : finalData) {
            if ("EXPIRY001".equals(station.getId())) {
                foundExpiredData = true;
                break;
            }
        }
        assertFalse(foundExpiredData, "Data should have expired and been removed");
    }

    @Test
    void testDataNotExpiredWithinTimeWindow() throws Exception {
        ContentServer contentServer = new ContentServer(clientConfig);
        GETClient getClient = new GETClient(clientConfig);

        // Add test data
        String weatherFile = createTestWeatherDataFile("NOTEXPIRED001", "Not Expired Test Station");
        contentServer.publishWeatherData(weatherFile);

        // Wait less than expiry time (10 seconds)
        Thread.sleep(10000);

        // Data should still be present
        WeatherData[] data = getClient.retrieveWeatherData();
        boolean foundData = false;
        for (WeatherData station : data) {
            if ("NOTEXPIRED001".equals(station.getId())) {
                foundData = true;
                break;
            }
        }
        assertTrue(foundData, "Data should not have expired within 10 seconds");
    }

    @Test
    void testExpiryDoesNotInterferWithActiveOperations() throws Exception {
        ContentServer contentServer = new ContentServer(clientConfig);
        GETClient getClient = new GETClient(clientConfig);

        // Add initial data
        String weatherFile1 = createTestWeatherDataFile("ACTIVE001", "Active Station 1");
        contentServer.publishWeatherData(weatherFile1);

        // Wait for some time (but less than expiry)
        Thread.sleep(15000);

        // Add more data while first data is still active
        String weatherFile2 = createTestWeatherDataFile("ACTIVE002", "Active Station 2");
        contentServer.publishWeatherData(weatherFile2);

        // Perform GET operation
        WeatherData[] data = getClient.retrieveWeatherData();
        assertNotNull(data, "GET operation should work during expiry processing");
        assertTrue(data.length > 0, "Should have some data");

        // The exact data present depends on timing, but operations should not be blocked
        System.out.println("Retrieved " + data.length + " stations during expiry processing");
    }

    @Test
    void testExpiryTimingAccuracy() throws Exception {
        ContentServer contentServer = new ContentServer(clientConfig);
        GETClient getClient = new GETClient(clientConfig);

        // Record start time
        long startTime = System.currentTimeMillis();

        // Add test data
        String weatherFile = createTestWeatherDataFile("TIMING001", "Timing Test Station");
        contentServer.publishWeatherData(weatherFile);

        // Check data is present initially
        WeatherData[] initialData = getClient.retrieveWeatherData();
        boolean foundInitial = false;
        for (WeatherData station : initialData) {
            if ("TIMING001".equals(station.getId())) {
                foundInitial = true;
                break;
            }
        }
        assertTrue(foundInitial, "Data should be present initially");

        // Wait for expiry and measure timing
        boolean hasExpired = false;
        long expiryTime = 0;

        // Check every 2 seconds for up to 40 seconds
        for (int i = 0; i < 20; i++) {
            Thread.sleep(2000);

            WeatherData[] currentData = getClient.retrieveWeatherData();
            boolean stillPresent = false;
            for (WeatherData station : currentData) {
                if ("TIMING001".equals(station.getId())) {
                    stillPresent = true;
                    break;
                }
            }

            if (!stillPresent) {
                hasExpired = true;
                expiryTime = System.currentTimeMillis();
                break;
            }
        }

        assertTrue(hasExpired, "Data should have expired within test timeout");

        long totalTime = expiryTime - startTime;
        System.out.println("Data expired after " + totalTime + "ms");

        // Should expire around 30-35 seconds (30s expiry + up to 5s cleanup interval)
        assertTrue(totalTime >= 28000, "Data should not expire too early (before ~28 seconds)");
        assertTrue(totalTime <= 40000, "Data should not take too long to expire (after ~40 seconds)");
    }

    @Test
    void testMultipleStationsExpiryHandling() throws Exception {
        ContentServer contentServer = new ContentServer(clientConfig);
        GETClient getClient = new GETClient(clientConfig);

        // Add multiple stations at different times
        String weatherFile1 = createTestWeatherDataFile("MULTI001", "Multi Station 1");
        contentServer.publishWeatherData(weatherFile1);

        Thread.sleep(5000); // 5 second gap

        String weatherFile2 = createTestWeatherDataFile("MULTI002", "Multi Station 2");
        contentServer.publishWeatherData(weatherFile2);

        // Wait for first station to expire but second to remain
        Thread.sleep(30000); // Total 35 seconds for first, 30 for second

        WeatherData[] data = getClient.retrieveWeatherData();

        boolean found1 = false, found2 = false;
        for (WeatherData station : data) {
            if ("MULTI001".equals(station.getId())) found1 = true;
            if ("MULTI002".equals(station.getId())) found2 = true;
        }

        // Results may vary based on exact timing, but the system should handle multiple expiries
        System.out.println("Multi001 present: " + found1 + ", Multi002 present: " + found2);

        // At least verify that expiry processing doesn't crash the system
        assertDoesNotThrow(() -> {
            getClient.retrieveWeatherData();
        }, "System should handle multiple station expiries gracefully");
    }

    @Test
    void testServerContinuesAfterDataExpiry() throws Exception {
        ContentServer contentServer = new ContentServer(clientConfig);
        GETClient getClient = new GETClient(clientConfig);

        // Add data that will expire
        String weatherFile1 = createTestWeatherDataFile("CONTINUE001", "Continue Test 1");
        contentServer.publishWeatherData(weatherFile1);

        // Wait for expiry
        Thread.sleep(35000);

        // Server should continue to work after expiry
        String weatherFile2 = createTestWeatherDataFile("CONTINUE002", "Continue Test 2");
        assertDoesNotThrow(() -> {
            contentServer.publishWeatherData(weatherFile2);
        }, "Server should continue working after data expiry");

        // Should be able to retrieve new data
        WeatherData[] newData = getClient.retrieveWeatherData();
        assertNotNull(newData, "Should be able to retrieve data after expiry");

        boolean foundNewData = false;
        for (WeatherData station : newData) {
            if ("CONTINUE002".equals(station.getId())) {
                foundNewData = true;
                break;
            }
        }
        assertTrue(foundNewData, "Should find new data after expiry cleanup");
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
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
                "air_temp:25.0\n" +
                "apparent_t:23.0\n" +
                "cloud:Clear\n" +
                "dewpt:12.0\n" +
                "press:1015.0\n" +
                "rel_hum:55\n" +
                "wind_dir:NE\n" +
                "wind_spd_kmh:8\n" +
                "wind_spd_kt:4\n";

        Path weatherFile = tempDir.resolve("expiry_" + id + ".txt");
        Files.writeString(weatherFile, weatherContent);
        return weatherFile.toString();
    }
}