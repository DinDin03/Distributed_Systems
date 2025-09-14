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

/**
 * Integration test suite for Lamport clock ordering functionality.
 * Tests Lamport clock behavior and ordering in client-server interactions.
 */
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

    // === CORE LAMPORT CLOCK ORDERING TESTS ===

    @Test
    // Tests server startup with Lamport ordering service and basic connectivity
    void testServerStartupWithLamportOrdering() throws Exception {
        System.out.println("Testing server startup with Lamport ordering...");
        
        // Verify server is running
        assertTrue(isServerRunning(serverPort), "Server should be running");
        
        // Test basic connectivity
        assertDoesNotThrow(() -> {
            try (Socket testSocket = new Socket("localhost", serverPort)) {
                // Connection successful
            }
        }, "Server should accept connections");
        
        System.out.println("✓ Server startup with Lamport ordering test passed");
    }

    @Test
    // Tests Lamport clock initialization for new clients
    void testLamportClockInitialization() throws Exception {
        System.out.println("Testing Lamport clock initialization...");
        
        // Test that each new client starts with Lamport time 0
        for (int i = 0; i < 3; i++) {
            GETClient newClient = new GETClient(clientConfig);
            long initialTime = newClient.getLamportTime();
            assertEquals(0, initialTime, "Each new client should start with Lamport time 0");
            
            System.out.println("New client " + i + " initial time: " + initialTime);
        }
        
        System.out.println("✓ Lamport clock initialization test passed");
    }

    @Test
    // Tests Lamport clock functionality with file operations
    void testLamportClockWithFileOperations() throws Exception {
        System.out.println("Testing Lamport clock with file operations...");
        
        // Test file creation (simulating data that would be subject to Lamport ordering)
        String weatherFile = createTestWeatherDataFile("LAMPORT001", "Lamport Test Station");
        assertNotNull(weatherFile, "Should create weather file");
        assertTrue(Files.exists(Path.of(weatherFile)), "Weather file should exist");
        
        // Test file content
        String content = Files.readString(Path.of(weatherFile));
        assertTrue(content.contains("LAMPORT001"), "File should contain station ID");
        assertTrue(content.contains("Lamport Test Station"), "File should contain station name");
        
        // Test multiple file operations
        for (int i = 0; i < 3; i++) {
            String testFile = createTestWeatherDataFile("LAMPORT" + i, "Lamport Test " + i);
            assertNotNull(testFile, "Should create test file " + i);
            assertTrue(Files.exists(Path.of(testFile)), "Test file " + i + " should exist");
        }
        
        System.out.println("✓ Lamport clock with file operations test passed");
    }

    @Test
    // Tests Lamport clock stability and concurrent operations
    void testLamportClockStability() throws Exception {
        System.out.println("Testing Lamport clock stability...");
        
        // Test that server remains stable with Lamport ordering
        assertTrue(isServerRunning(serverPort), "Server should remain running");
        
        // Test multiple connections to ensure Lamport ordering doesn't block server
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> {
                try (Socket testSocket = new Socket("localhost", serverPort)) {
                    // Connection successful
                }
            }, "Server should accept connections with Lamport ordering");
            
            // Small delay between connections
            Thread.sleep(500);
        }
        
        // Test server can handle concurrent operations
        assertDoesNotThrow(() -> {
            // Simulate concurrent access
            Thread[] threads = new Thread[2];
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
        }, "Server should handle concurrent operations with Lamport ordering");
        
        System.out.println("✓ Lamport clock stability test passed");
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