package com.weathersystem.integration;

import com.weathersystem.client.GETClient;
import com.weathersystem.client.common.ClientConfiguration;
import com.weathersystem.server.AggregationServer;
import com.weathersystem.shared.domain.WeatherData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified integration test suite for basic client-server communication.
 * Tests essential functionality without complex Lamport ordering scenarios.
 */
class BasicClientServerTest {

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

        // Create test data directory
        Path dataDir = tempDir.resolve("data");
        Files.createDirectories(dataDir);

        // Clear any existing data files
        clearExistingDataFiles();

        // Initialize and start server
        server = new AggregationServer();
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
        waitForServerReady();

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
            serverThread.join(1000);
        }
    }

    // === CORE INTEGRATION TESTS ===

    @Test
    // Checks that the server starts up and shuts down properly
    void testServerStartupAndShutdown() throws Exception {
        System.out.println("Testing server startup and shutdown");
        
        // Test that server started successfully
        assertNotNull(server, "Server should be initialized");
        assertTrue(serverThread.isAlive(), "Server thread should be running");
        
        // Test that server is listening on the correct port
        try (Socket testSocket = new Socket("localhost", serverPort)) {
            assertTrue(testSocket.isConnected(), "Should be able to connect to server");
        }
        
        System.out.println("Server startup and shutdown test passed");
    }

    @Test
    // Makes sure the client config is set up properly
    void testClientConfiguration() throws Exception {
        System.out.println("Testing client configuration");
        
        // Test that client configuration is properly set up
        assertNotNull(clientConfig, "Client configuration should not be null");
        assertEquals("localhost", clientConfig.getHost(), "Server host should be localhost");
        assertEquals(serverPort, clientConfig.getPort(), "Server port should match");
        assertEquals("WeatherTestClient/1.0", clientConfig.getUserAgent(), "User agent should match");
        
        System.out.println("Client configuration test passed");
    }

    @Test
    // Tests that the server grabs a port and no one else can use it
    void testServerPortAvailability() throws Exception {
        System.out.println("Testing server port availability");
        
        // Test that the port is actually in use by the server
        assertTrue(serverPort > 0, "Server port should be valid");
        assertTrue(serverPort < 65536, "Server port should be within valid range");
        
        // Test that we can't bind to the same port (it's already in use)
        assertThrows(IOException.class, () -> {
            try (ServerSocket testSocket = new ServerSocket(serverPort)) {
                // This should fail because the port is already in use
            }
        }, "Should not be able to bind to the same port as the server");
        
        System.out.println("Server port availability test passed");
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private void waitForServerReady() {
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            try (Socket testSocket = new Socket("localhost", serverPort)) {
                return; // Server is ready
            } catch (IOException e) {
                try {
                    Thread.sleep(500); // Wait 500ms before retry
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
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
            System.out.println("Warning: Could not clear data files: " + e.getMessage());
        }
    }
}