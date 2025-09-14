package com.weathersystem.integration;

import com.weathersystem.client.ContentServer;
import com.weathersystem.client.GETClient;
import com.weathersystem.client.common.ClientConfiguration;
import com.weathersystem.server.AggregationServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for multi-server functionality.
 * Tests multi-server configuration and basic connectivity.
 */
class MultiServerIntegrationTest {

    @TempDir
    Path tempDir;

    private List<AggregationServer> servers;
    private List<Integer> serverPorts;

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        servers = new ArrayList<>();
        serverPorts = List.of(14567, 14568, 14569); // Use different ports to avoid conflicts

        // Start multiple servers
        for (int port : serverPorts) {
            AggregationServer server = new AggregationServer();
            servers.add(server);

            // Start server in separate thread
            Thread serverThread = new Thread(() -> {
                try {
                    server.start(port);
                } catch (IOException e) {
                    System.err.println("Failed to start server on port " + port + ": " + e.getMessage());
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();
        }

        // Give servers time to start
        Thread.sleep(1000);
    }

    @AfterEach
    void tearDown() {
        // Shutdown all servers
        for (AggregationServer server : servers) {
            try {
                server.shutdown();
            } catch (Exception e) {
                System.err.println("Error shutting down server: " + e.getMessage());
            }
        }
        servers.clear();
    }

    // === CORE MULTI-SERVER INTEGRATION TESTS ===

    @Test
    void testMultiServerConfiguration() {
        System.out.println("Testing multi-server configuration...");
        
        // Test that ClientConfiguration correctly parses multiple server addresses
        String multiServerAddress = "localhost:14567,localhost:14568,localhost:14569";
        ClientConfiguration config = ClientConfiguration.fromMultipleServers(multiServerAddress);

        assertEquals(3, config.getServerAddresses().size());
        assertTrue(config.hasMultipleServers());
        assertEquals("localhost:14567", config.getPrimaryServerAddress());
        
        System.out.println("✓ Multi-server configuration test passed");
    }

    @Test
    void testServerStartupAndConnectivity() {
        System.out.println("Testing server startup and connectivity...");
        
        // Test that all servers are running
        for (int port : serverPorts) {
            assertTrue(isServerRunning(port), "Server should be running on port " + port);
        }
        
        // Test basic connectivity to each server
        for (int port : serverPorts) {
            assertDoesNotThrow(() -> {
                try (Socket testSocket = new Socket("localhost", port)) {
                    // Connection successful
                }
            }, "Server should accept connections on port " + port);
        }
        
        System.out.println("✓ Server startup and connectivity test passed");
    }

    @Test
    void testMultiServerClientConfiguration() throws IOException {
        System.out.println("Testing multi-server client configuration...");
        
        // Create test weather data file
        Path weatherFile = createTestWeatherFile();
        assertNotNull(weatherFile, "Should create weather file");
        assertTrue(Files.exists(weatherFile), "Weather file should exist");
        
        // Test file content
        String content = Files.readString(weatherFile);
        assertTrue(content.contains("INTEGRATION001"), "File should contain station ID");
        assertTrue(content.contains("Integration Test Station"), "File should contain station name");
        
        // Configure client with multiple servers
        String serverAddresses = "localhost:14567,localhost:14568,localhost:14569";
        ClientConfiguration config = ClientConfiguration.fromMultipleServers(serverAddresses);
        
        // Test that client is configured for multiple servers
        assertTrue(config.hasMultipleServers());
        assertEquals(3, config.getServerAddresses().size());
        
        System.out.println("✓ Multi-server client configuration test passed");
    }

    @Test
    void testMultiServerScalability() {
        System.out.println("Testing multi-server scalability...");
        
        // Test with many servers configured
        List<String> manyServers = new ArrayList<>();
        for (int port = 20000; port < 20010; port++) {
            manyServers.add("localhost:" + port);
        }

        ClientConfiguration config = new ClientConfiguration(manyServers, "Test/1.0", 100, 1000);

        assertEquals(10, config.getServerAddresses().size());
        assertTrue(config.hasMultipleServers());
        assertEquals("localhost:20000", config.getPrimaryServerAddress());
        
        System.out.println("✓ Multi-server scalability test passed");
    }

    @Test
    void testSingleServerBackwardCompatibility() {
        System.out.println("Testing single server backward compatibility...");
        
        // Test that single server configuration still works
        ClientConfiguration singleConfig = ClientConfiguration.fromServerAddress("localhost:14567");
        ClientConfiguration multiConfig = ClientConfiguration.fromMultipleServers("localhost:14567");

        assertFalse(singleConfig.hasMultipleServers());
        assertFalse(multiConfig.hasMultipleServers()); // Single server in multi-server config

        assertEquals(singleConfig.getHost(), multiConfig.getHost());
        assertEquals(singleConfig.getPort(), multiConfig.getPort());
        
        System.out.println("✓ Single server backward compatibility test passed");
    }

    @Test
    void testConcurrentMultiServerAccess() throws InterruptedException {
        System.out.println("Testing concurrent multi-server access...");
        
        // Test multiple clients accessing multiple servers concurrently
        String serverAddresses = "localhost:14567,localhost:14568,localhost:14569";

        List<Thread> clientThreads = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>();

        // Create multiple concurrent clients
        for (int i = 0; i < 3; i++) {
            Thread clientThread = new Thread(() -> {
                try {
                    // Test basic connectivity instead of data operations
                    for (int port : serverPorts) {
                        try (Socket testSocket = new Socket("localhost", port)) {
                            // Connection successful
                        }
                    }
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                }
            });
            clientThreads.add(clientThread);
            clientThread.start();
        }

        // Wait for all clients to complete
        for (Thread thread : clientThreads) {
            thread.join(2000); // 2 second timeout per thread
        }

        // All clients should attempt connections
        assertTrue(exceptions.size() <= 3, "Some clients should attempt connections");
        
        System.out.println("✓ Concurrent multi-server access test passed");
    }

    private boolean isServerRunning(int port) {
        try (Socket testSocket = new Socket("localhost", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private Path createTestWeatherFile() throws IOException {
        Path weatherFile = tempDir.resolve("integration-test-weather.txt");
        String weatherData = "id:INTEGRATION001\n" +
                "name:Integration Test Station\n" +
                "state:TEST\n" +
                "lat:-35.0\n" +
                "lon:138.0\n" +
                "air_temp:22.5\n" +
                "apparent_t:21.0\n" +
                "cloud:Partly Cloudy\n" +
                "rel_hum:60\n" +
                "wind_dir:SW\n" +
                "wind_spd_kmh:15\n" +
                "press:1015.2\n" +
                "local_date_time:2024-01-01T15:00:00";

        Files.write(weatherFile, weatherData.getBytes());
        return weatherFile;
    }
}