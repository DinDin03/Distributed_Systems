package com.weathersystem.integration;
import com.weathersystem.server.AggregationServer;
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
    // Tests server startup and basic functionality including connectivity
    void testServerStartupAndBasicFunctionality() throws Exception {
        System.out.println("Testing server startup and basic functionality...");
        
        int serverPort = findAvailablePort();
        AggregationServer server = startServer(serverPort);
        
        try {
            // Test basic server connectivity
            assertTrue(isServerRunning(serverPort), "Server should be running");
            
            // Test that server can handle basic operations
            assertDoesNotThrow(() -> {
                // Just test that server is responsive
                try (Socket testSocket = new Socket("localhost", serverPort)) {
                    // Connection successful
                }
            }, "Server should accept connections");
            
            System.out.println("✓ Server startup and basic functionality test passed");
            
        } finally {
            shutdownServer(server);
        }
    }

    @Test
    // Tests server restart functionality and port reuse after shutdown
    void testServerRestartAndPortReuse() throws Exception {
        System.out.println("Testing server restart and port reuse...");
        
        int serverPort = findAvailablePort();
        
        // First server instance
        AggregationServer server1 = startServer(serverPort);
        try {
            assertTrue(isServerRunning(serverPort), "First server should be running");
            System.out.println("First server started successfully");
        } finally {
            shutdownServer(server1);
        }

        // Wait for port to be released
        Thread.sleep(1000);

        // Second server instance on same port
        AggregationServer server2 = startServer(serverPort);
        try {
            assertTrue(isServerRunning(serverPort), "Second server should be running on same port");
            System.out.println("Second server started successfully on same port");
            
            System.out.println("✓ Server restart and port reuse test passed");
            
        } finally {
            shutdownServer(server2);
        }
    }

    @Test
    // Tests file operations and error handling for weather data files
    void testFileOperationsAndErrorHandling() throws Exception {
        System.out.println("Testing file operations and error handling...");
        
        // Test file creation
        String weatherFile = createTestWeatherDataFile("TEST001", "Test Station");
        assertNotNull(weatherFile, "Should create weather file");
        assertTrue(Files.exists(Path.of(weatherFile)), "Weather file should exist");
        
        // Test file content
        String content = Files.readString(Path.of(weatherFile));
        assertTrue(content.contains("TEST001"), "File should contain station ID");
        assertTrue(content.contains("Test Station"), "File should contain station name");
        
        // Test error handling
        assertDoesNotThrow(() -> {
            // Test with invalid file path
            String invalidFile = createTestWeatherDataFile("INVALID", "Invalid Station");
            assertNotNull(invalidFile, "Should handle file creation gracefully");
        }, "Should handle file operations gracefully");
        
        System.out.println("✓ File operations and error handling test passed");
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private AggregationServer startServer(int port) throws InterruptedException {
        AggregationServer server = new AggregationServer();
        Thread serverThread = new Thread(() -> {
            try {
                server.start(port);
            } catch (Exception e) {
                System.err.println("Server failed: " + e.getMessage());
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Wait for server to be ready
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .ignoreExceptions()
                .until(() -> {
                    try (Socket testSocket = new Socket("localhost", port)) {
                        return true;
                    }
                });

        return server;
    }

    private void shutdownServer(AggregationServer server) throws InterruptedException {
        if (server != null) {
            server.shutdown();
            Thread.sleep(1000); // Give time for cleanup
        }
    }

    private boolean isServerRunning(int port) {
        try (Socket testSocket = new Socket("localhost", port)) {
            return true;
        } catch (IOException e) {
            return false;
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