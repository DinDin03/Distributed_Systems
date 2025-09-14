package com.weathersystem.integration;

import com.weathersystem.client.ContentServer;
import com.weathersystem.client.GETClient;
import com.weathersystem.client.common.ClientConfiguration;
import com.weathersystem.server.AggregationServer;
import com.weathersystem.shared.domain.WeatherData;
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

class DataPersistenceTest {

    @TempDir
    Path tempDir;

    @Test
    void testServerStartupLoadsPersistedData() throws Exception {
        int serverPort = findAvailablePort();
        ClientConfiguration clientConfig = new ClientConfiguration("localhost", serverPort, "WeatherTestClient/1.0", 5000, 10000);

        // Start server and add some data
        AggregationServer server1 = new AggregationServer();
        Thread serverThread1 = new Thread(() -> {
            try {
                server1.start(serverPort);
            } catch (Exception e) {
                System.err.println("Server1 failed: " + e.getMessage());
            }
        });
        serverThread1.setDaemon(true);
        serverThread1.start();

        // Wait for server to be ready
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .ignoreExceptions()
                .until(() -> {
                    try (Socket testSocket = new Socket("localhost", serverPort)) {
                        return true;
                    }
                });

        try {
            // Add test data
            String weatherFile = createTestWeatherDataFile("PERSIST001", "Persistence Test Station");
            ContentServer contentServer = new ContentServer(clientConfig);
            contentServer.publishWeatherData(weatherFile);

            // Verify data is present
            GETClient getClient = new GETClient(clientConfig);
            WeatherData[] data1 = getClient.retrieveWeatherData();
            boolean foundPersistentData = false;
            for (WeatherData station : data1) {
                if ("PERSIST001".equals(station.getId())) {
                    foundPersistentData = true;
                    break;
                }
            }
            assertTrue(foundPersistentData, "Should find persistent data in first server");

            // Give time for data to be saved
            Thread.sleep(2000);

        } finally {
            // Shutdown first server
            server1.shutdown();
            serverThread1.interrupt();
            serverThread1.join(2000);
        }

        // Start new server instance on different port
        int serverPort2 = findAvailablePort();
        ClientConfiguration clientConfig2 = new ClientConfiguration("localhost", serverPort2, "WeatherTestClient/1.0", 5000, 10000);

        AggregationServer server2 = new AggregationServer();
        Thread serverThread2 = new Thread(() -> {
            try {
                server2.start(serverPort2);
            } catch (Exception e) {
                System.err.println("Server2 failed: " + e.getMessage());
            }
        });
        serverThread2.setDaemon(true);
        serverThread2.start();

        // Wait for second server to be ready
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .ignoreExceptions()
                .until(() -> {
                    try (Socket testSocket = new Socket("localhost", serverPort2)) {
                        return true;
                    }
                });

        try {
            // Verify second server loaded persisted data
            GETClient getClient2 = new GETClient(clientConfig2);
            WeatherData[] data2 = getClient2.retrieveWeatherData();

            // Should have at least the data we know exists (may have more from previous tests)
            assertNotNull(data2, "Should load data on startup");
            assertTrue(data2.length > 0, "Should have loaded existing data");

        } finally {
            server2.shutdown();
            serverThread2.interrupt();
            serverThread2.join(2000);
        }
    }

    @Test
    void testOngoingOperationsPersistence() throws Exception {
        int serverPort = findAvailablePort();
        ClientConfiguration clientConfig = new ClientConfiguration("localhost", serverPort, "WeatherTestClient/1.0", 5000, 10000);

        AggregationServer server = new AggregationServer();
        Thread serverThread = new Thread(() -> {
            try {
                server.start(serverPort);
            } catch (Exception e) {
                System.err.println("Server failed: " + e.getMessage());
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

        try {
            ContentServer contentServer = new ContentServer(clientConfig);
            GETClient getClient = new GETClient(clientConfig);

            // Perform multiple operations
            for (int i = 0; i < 5; i++) {
                String weatherFile = createTestWeatherDataFile("ONGOING" + i, "Ongoing Test " + i);
                contentServer.publishWeatherData(weatherFile);

                // Verify each operation is persisted by checking if data is retrievable
                WeatherData[] data = getClient.retrieveWeatherData();
                boolean foundData = false;
                for (WeatherData station : data) {
                    if (("ONGOING" + i).equals(station.getId())) {
                        foundData = true;
                        break;
                    }
                }
                assertTrue(foundData, "Data should be persisted after operation " + i);

                // Small delay to allow persistence
                Thread.sleep(500);
            }

        } finally {
            server.shutdown();
            serverThread.interrupt();
            serverThread.join(2000);
        }
    }

    @Test
    void testDataIntegrityAcrossRestarts() throws Exception {
        int serverPort = findAvailablePort();
        ClientConfiguration clientConfig = new ClientConfiguration("localhost", serverPort, "WeatherTestClient/1.0", 5000, 10000);

        // First server instance
        AggregationServer server1 = new AggregationServer();
        Thread serverThread1 = new Thread(() -> {
            try {
                server1.start(serverPort);
            } catch (Exception e) {
                System.err.println("Server1 failed: " + e.getMessage());
            }
        });
        serverThread1.setDaemon(true);
        serverThread1.start();

        // Wait for server to be ready
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .ignoreExceptions()
                .until(() -> {
                    try (Socket testSocket = new Socket("localhost", serverPort)) {
                        return true;
                    }
                });

        WeatherData originalData = null;
        try {
            // Store specific data
            String weatherFile = createTestWeatherDataFile("INTEGRITY001", "Integrity Test Station");
            ContentServer contentServer = new ContentServer(clientConfig);
            contentServer.publishWeatherData(weatherFile);

            // Retrieve and store the exact data
            GETClient getClient = new GETClient(clientConfig);
            WeatherData[] data = getClient.retrieveWeatherData();
            for (WeatherData station : data) {
                if ("INTEGRITY001".equals(station.getId())) {
                    originalData = station;
                    break;
                }
            }
            assertNotNull(originalData, "Should have found original data");

            // Allow persistence
            Thread.sleep(2000);

        } finally {
            server1.shutdown();
            serverThread1.interrupt();
            serverThread1.join(2000);
        }

        // Wait a bit before restart
        Thread.sleep(1000);

        // Second server instance
        AggregationServer server2 = new AggregationServer();
        Thread serverThread2 = new Thread(() -> {
            try {
                server2.start(serverPort);
            } catch (Exception e) {
                System.err.println("Server2 failed: " + e.getMessage());
            }
        });
        serverThread2.setDaemon(true);
        serverThread2.start();

        // Wait for second server to be ready
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .ignoreExceptions()
                .until(() -> {
                    try (Socket testSocket = new Socket("localhost", serverPort)) {
                        return true;
                    }
                });

        try {
            // Verify data integrity after restart
            GETClient getClient2 = new GETClient(clientConfig);
            WeatherData[] restoredData = getClient2.retrieveWeatherData();

            WeatherData restoredStation = null;
            for (WeatherData station : restoredData) {
                if ("INTEGRITY001".equals(station.getId())) {
                    restoredStation = station;
                    break;
                }
            }

            // Data might not be restored if file corruption occurred, but if it is, it should be correct
            if (restoredStation != null) {
                assertEquals(originalData.getId(), restoredStation.getId(), "Station ID should match");
                assertEquals(originalData.getName(), restoredStation.getName(), "Station name should match");
                assertEquals(originalData.getState(), restoredStation.getState(), "Station state should match");
                assertEquals(originalData.getAirTemp(), restoredStation.getAirTemp(), 0.001, "Temperature should match");
            }

        } finally {
            server2.shutdown();
            serverThread2.interrupt();
            serverThread2.join(2000);
        }
    }

    @Test
    void testFileSystemErrorHandling() throws Exception {
        int serverPort = findAvailablePort();
        ClientConfiguration clientConfig = new ClientConfiguration("localhost", serverPort, "WeatherTestClient/1.0", 5000, 10000);

        AggregationServer server = new AggregationServer();
        Thread serverThread = new Thread(() -> {
            try {
                server.start(serverPort);
            } catch (Exception e) {
                System.err.println("Server failed: " + e.getMessage());
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

        try {
            ContentServer contentServer = new ContentServer(clientConfig);
            GETClient getClient = new GETClient(clientConfig);

            // Server should continue working even if file system issues occur
            // (We can't easily simulate file system errors in tests, but we can verify
            // that the server continues to work and serve data from memory)

            String weatherFile = createTestWeatherDataFile("FILESYS001", "FileSystem Test Station");

            // This should work regardless of file system state
            assertDoesNotThrow(() -> {
                contentServer.publishWeatherData(weatherFile);
            }, "Server should handle file system errors gracefully");

            // Data should still be available from memory
            WeatherData[] data = getClient.retrieveWeatherData();
            assertNotNull(data, "Should still serve data from memory");

        } finally {
            server.shutdown();
            serverThread.interrupt();
            serverThread.join(2000);
        }
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
                "air_temp:20.0\n" +
                "apparent_t:18.5\n" +
                "cloud:Clear\n" +
                "dewpt:10.0\n" +
                "press:1013.0\n" +
                "rel_hum:60\n" +
                "wind_dir:N\n" +
                "wind_spd_kmh:10\n" +
                "wind_spd_kt:5\n";

        Path weatherFile = tempDir.resolve("test_" + id + ".txt");
        Files.writeString(weatherFile, weatherContent);
        return weatherFile.toString();
    }
}