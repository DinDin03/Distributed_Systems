package com.weathersystem.client.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiServerFailoverTest {

    private TestHttpClient testClient;

    @BeforeEach
    void setUp() {
        // Create client with multiple servers for failover testing
        List<String> servers = Arrays.asList("localhost:9999", "localhost:4567", "localhost:4568");
        ClientConfiguration config = new ClientConfiguration(servers, "TestAgent/1.0", 1000, 5000);
        testClient = new TestHttpClient(config);
    }

    @Test
    // Tests multi-server configuration setup and validates server address parsing
    void testMultiServerConfiguration() {
        System.out.println("Testing multi-server configuration setup...");
        List<String> servers = Arrays.asList("server1:4567", "server2:4568", "server3:4569");
        ClientConfiguration config = new ClientConfiguration(servers, "Test/1.0", 100, 1000);

        assertEquals(3, config.getServerAddresses().size(), "Should have 3 server addresses");
        assertEquals("server1:4567", config.getServerAddresses().get(0), "First server should match");
        assertEquals("server2:4568", config.getServerAddresses().get(1), "Second server should match");
        assertEquals("server3:4569", config.getServerAddresses().get(2), "Third server should match");
        assertTrue(config.hasMultipleServers(), "Should detect multiple servers");
        assertEquals("server1:4567", config.getPrimaryServerAddress(), "Primary server should be first");
        System.out.println("✓ Multi-server configuration test passed");
    }

    @Test
    // Tests failover behavior when primary server is unreachable
    void testFailoverBehavior() {
        System.out.println("Testing failover behavior with unreachable servers...");
        // Test that failover attempts all servers when connection fails
        assertDoesNotThrow(() -> {
            try {
                testClient.testCreateConnection();
            } catch (IOException e) {
                // Expected when all servers are unreachable
                assertTrue(e.getMessage().contains("All servers failed") || 
                          e.getMessage().contains("Connection refused") ||
                          e.getMessage().contains("timeout"),
                          "Should indicate all servers failed: " + e.getMessage());
            }
        });
        System.out.println("✓ Failover behavior test passed");
    }

    @Test
    // Tests single server configuration and verifies it does not use failover logic
    void testSingleServerConfiguration() {
        System.out.println("Testing single server configuration...");
        ClientConfiguration singleConfig = new ClientConfiguration("localhost", 4567, "Test/1.0", 1000, 5000);
        TestHttpClient singleClient = new TestHttpClient(singleConfig);

        assertFalse(singleClient.getConfig().hasMultipleServers(), "Should not have multiple servers");
        assertEquals(1, singleClient.getConfig().getServerAddresses().size(), "Should have single server address");
        
        // Test single server connection attempt
        assertDoesNotThrow(() -> {
            try {
                singleClient.testCreateConnection();
            } catch (IOException e) {
                // Expected when server is down - should not mention "all servers failed"
                assertFalse(e.getMessage().contains("All servers failed"), 
                           "Single server should not mention 'all servers failed'");
            }
        });
        System.out.println("✓ Single server configuration test passed");
    }

    @Test
    // Tests validation of invalid configuration parameters including null and empty server lists
    void testInvalidConfigurationHandling() {
        System.out.println("Testing invalid configuration handling...");
        
        // Test empty server list
        assertThrows(IllegalArgumentException.class, () -> {
            new ClientConfiguration(List.of(), "Test/1.0", 1000, 5000);
        }, "Should throw exception for empty server list");

        // Test null server list
        assertThrows(IllegalArgumentException.class, () -> {
            new ClientConfiguration(null, "Test/1.0", 1000, 5000);
        }, "Should throw exception for null server list");
        
        System.out.println("✓ Invalid configuration handling test passed");
    }

    @Test
    // Tests connection timeout behavior with short timeout values
    void testConnectionTimeoutBehavior() {
        System.out.println("Testing connection timeout behavior...");
        // Create config with very short timeouts to test timeout handling
        List<String> servers = Arrays.asList("10.255.255.1:4567", "localhost:4567");
        ClientConfiguration config = new ClientConfiguration(servers, "Test/1.0", 50, 100);
        TestHttpClient client = new TestHttpClient(config);

        long startTime = System.currentTimeMillis();

        assertThrows(Exception.class, client::testCreateConnection, "Should throw exception when connection fails");

        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration < 5000, "Should timeout quickly with short timeout values: " + duration + "ms");
        System.out.println("✓ Connection timeout behavior test passed");
    }

    @Test
    // Tests failover resilience with multiple failed servers in sequence
    void testFailoverResilience() {
        System.out.println("Testing failover resilience with multiple failed servers...");
        // Test that failover works even with many failed servers
        List<String> servers = Arrays.asList(
            "invalid1:9999", "invalid2:9998", "invalid3:9997",
            "invalid4:9996", "localhost:4567" // Last one might work
        );
        ClientConfiguration config = new ClientConfiguration(servers, "Test/1.0", 50, 100);
        TestHttpClient client = new TestHttpClient(config);

        assertEquals(5, config.getServerAddresses().size(), "Should have 5 server addresses");
        assertTrue(config.hasMultipleServers(), "Should detect multiple servers");

        // Should attempt all servers in sequence
        assertDoesNotThrow(() -> {
            try {
                client.testCreateConnection();
            } catch (IOException e) {
                // Expected when all servers are down
                assertTrue(e.getMessage().contains("All servers failed") || 
                          e.getMessage().contains("Connection refused") ||
                          e.getMessage().contains("timeout"),
                          "Should indicate all servers failed: " + e.getMessage());
            }
        });
        System.out.println("✓ Failover resilience test passed");
    }

    private static class TestHttpClient extends HttpClientBase {
        public TestHttpClient(ClientConfiguration config) {
            super(config);
        }

        public void testCreateConnection() throws IOException {
            // This will test the actual failover logic
            try (Socket socket = createConnection()) {
            }
        }

        public ClientConfiguration getConfig() {
            return this.config;
        }
    }
}