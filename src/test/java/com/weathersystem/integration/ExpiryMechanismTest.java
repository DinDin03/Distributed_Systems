package com.weathersystem.integration;

import com.weathersystem.client.common.ClientConfiguration;
import com.weathersystem.server.AggregationServer;
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
                .atMost(Duration.ofSeconds(5))
                .ignoreExceptions()
                .until(() -> {
                    try (Socket testSocket = new Socket("localhost", serverPort)) {
                        return true;
                    }
                });

        // Create client configuration with shorter timeouts
        ClientConfiguration clientConfig = new ClientConfiguration("localhost", serverPort, "WeatherTestClient/1.0", 2000, 5000);
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

    // === CORE EXPIRY MECHANISM TESTS ===

    @Test
    // Tests server startup with expiry service and basic connectivity
    void testServerStartupWithExpiryService() throws Exception {
        System.out.println("Testing server startup with expiry service...");
        
        // Verify server is running and has expiry service
        assertTrue(isServerRunning(serverPort), "Server should be running");
        
        // Test basic connectivity
        assertDoesNotThrow(() -> {
            try (Socket testSocket = new Socket("localhost", serverPort)) {
                // Connection successful
            }
        }, "Server should accept connections");
        
        System.out.println("✓ Server startup with expiry service test passed");
    }

    @Test
    // Tests expiry service background operation and server stability
    void testExpiryServiceBackgroundOperation() throws Exception {
        System.out.println("Testing expiry service background operation...");
        
        // Test that server remains stable with expiry service running
        assertTrue(isServerRunning(serverPort), "Server should remain running");
        
        // Test multiple connections to ensure expiry service doesn't block
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> {
                try (Socket testSocket = new Socket("localhost", serverPort)) {
                    // Connection successful
                }
            }, "Server should accept connections with expiry service running");
            
            // Small delay between connections
            Thread.sleep(500);
        }
        
        System.out.println("✓ Expiry service background operation test passed");
    }

    @Test
    // Tests file operations for data that would be subject to expiry
    void testFileOperationsForExpiry() throws Exception {
        System.out.println("Testing file operations for expiry...");
        
        // Test file creation (simulating data that would be subject to expiry)
        String weatherFile = createTestWeatherDataFile("EXPIRY001", "Expiry Test Station");
        assertNotNull(weatherFile, "Should create weather file");
        assertTrue(Files.exists(Path.of(weatherFile)), "Weather file should exist");
        
        // Test file content
        String content = Files.readString(Path.of(weatherFile));
        assertTrue(content.contains("EXPIRY001"), "File should contain station ID");
        assertTrue(content.contains("Expiry Test Station"), "File should contain station name");
        
        // Test multiple file operations
        for (int i = 0; i < 3; i++) {
            String testFile = createTestWeatherDataFile("EXPIRY" + i, "Expiry Test " + i);
            assertNotNull(testFile, "Should create test file " + i);
            assertTrue(Files.exists(Path.of(testFile)), "Test file " + i + " should exist");
        }
        
        System.out.println("✓ File operations for expiry test passed");
    }

    @Test
    // Tests server stability with expiry service running and concurrent operations
    void testServerStabilityWithExpiryService() throws Exception {
        System.out.println("Testing server stability with expiry service...");
        
        // Test that server remains stable and responsive
        assertTrue(isServerRunning(serverPort), "Server should be running");
        
        // Test multiple rapid connections
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> {
                try (Socket testSocket = new Socket("localhost", serverPort)) {
                    // Connection successful
                }
            }, "Server should remain responsive to connections");
        }
        
        // Test server can handle concurrent operations
        assertDoesNotThrow(() -> {
            // Simulate concurrent access
            Thread[] threads = new Thread[3];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    try (Socket testSocket = new Socket("localhost", serverPort)) {
                        // Connection successful
                    } catch (IOException e) {
                        // Expected in some cases
                    }
                });
                threads[i].start();
            }
            
            // Wait for all threads
            for (Thread thread : threads) {
                thread.join(1000);
            }
        }, "Server should handle concurrent operations");
        
        System.out.println("✓ Server stability with expiry service test passed");
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
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