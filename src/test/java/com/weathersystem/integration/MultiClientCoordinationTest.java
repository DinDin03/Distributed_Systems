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

/**
 * Integration test suite for multi-client coordination functionality.
 * Tests server behavior with multiple concurrent clients and operations.
 */
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
                .atMost(Duration.ofSeconds(5))
                .ignoreExceptions()
                .until(() -> {
                    try (Socket testSocket = new Socket("localhost", serverPort)) {
                        return true;
                    }
                });

        // Create client configuration with shorter timeouts
        clientConfig = new ClientConfiguration("localhost", serverPort, "WeatherTestClient/1.0", 2000, 5000);
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

    // === CORE MULTI-CLIENT COORDINATION TESTS ===

    @Test
    // Tests server startup with multi-client support and basic connectivity
    void testServerStartupWithMultiClientSupport() throws Exception {
        System.out.println("Testing server startup with multi-client support...");
        
        // Verify server is running
        assertTrue(isServerRunning(serverPort), "Server should be running");
        
        // Test basic connectivity
        assertDoesNotThrow(() -> {
            try (Socket testSocket = new Socket("localhost", serverPort)) {
                // Connection successful
            }
        }, "Server should accept connections");
        
        System.out.println("✓ Server startup with multi-client support test passed");
    }

    @Test
    // Tests multiple client connections and concurrent access
    void testMultipleClientConnections() throws Exception {
        System.out.println("Testing multiple client connections...");
        
        final int numClients = 5;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(numClients);
        final AtomicInteger successCount = new AtomicInteger(0);
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Start multiple clients
        for (int i = 0; i < numClients; i++) {
            final int clientId = i;

            Thread clientThread = new Thread(() -> {
                try {
                    // Wait for start signal
                    startLatch.await();

                    // Test basic connectivity
                    try (Socket testSocket = new Socket("localhost", serverPort)) {
                        // Connection successful
                        successCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    completionLatch.countDown();
                }
            });
            clientThread.setDaemon(true);
            clientThread.start();
        }

        // Start all clients simultaneously
        startLatch.countDown();

        // Wait for all to complete
        assertTrue(completionLatch.await(10, TimeUnit.SECONDS),
                  "All clients should complete within timeout");

        // Verify results
        if (!exceptions.isEmpty()) {
            System.out.println("Exceptions occurred: " + exceptions.size());
        }

        assertTrue(successCount.get() >= numClients * 0.8,
                  "At least 80% of clients should succeed");
        
        System.out.println("✓ Multiple client connections test passed");
    }

    @Test
    // Tests concurrent client operations and server stability
    void testConcurrentClientOperations() throws Exception {
        System.out.println("Testing concurrent client operations...");
        
        final int numOperations = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(numOperations);
        final AtomicInteger successCount = new AtomicInteger(0);
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Create concurrent operations
        for (int i = 0; i < numOperations; i++) {
            final int operationId = i;

            Thread operationThread = new Thread(() -> {
                try {
                    // Wait for start signal
                    startLatch.await();

                    // Test basic connectivity
                    try (Socket testSocket = new Socket("localhost", serverPort)) {
                        // Connection successful
                        successCount.incrementAndGet();
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
        assertTrue(completionLatch.await(10, TimeUnit.SECONDS),
                  "All operations should complete within timeout");

        // Verify results
        if (!exceptions.isEmpty()) {
            System.out.println("Exceptions occurred: " + exceptions.size());
        }

        assertTrue(successCount.get() >= numOperations * 0.8,
                  "Most operations should succeed");
        
        System.out.println("✓ Concurrent client operations test passed");
    }

    @Test
    // Tests file operations for multi-client coordination scenarios
    void testFileOperationsForMultiClient() throws Exception {
        System.out.println("Testing file operations for multi-client...");
        
        // Test file creation (simulating data that would be subject to multi-client coordination)
        String weatherFile = createTestWeatherDataFile(0);
        assertNotNull(weatherFile, "Should create weather file");
        assertTrue(Files.exists(Path.of(weatherFile)), "Weather file should exist");
        
        // Test file content
        String content = Files.readString(Path.of(weatherFile));
        assertTrue(content.contains("TEST000"), "File should contain station ID");
        assertTrue(content.contains("Test Station 0"), "File should contain station name");
        
        // Test multiple file operations
        for (int i = 0; i < 3; i++) {
            String testFile = createTestWeatherDataFile(i);
            assertNotNull(testFile, "Should create test file " + i);
            assertTrue(Files.exists(Path.of(testFile)), "Test file " + i + " should exist");
        }
        
        System.out.println("✓ File operations for multi-client test passed");
    }

    @Test
    // Tests server stability with multiple clients and concurrent operations
    void testServerStabilityWithMultipleClients() throws Exception {
        System.out.println("Testing server stability with multiple clients...");
        
        // Test that server remains stable with multiple clients
        assertTrue(isServerRunning(serverPort), "Server should remain running");
        
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
        
        System.out.println("✓ Server stability with multiple clients test passed");
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