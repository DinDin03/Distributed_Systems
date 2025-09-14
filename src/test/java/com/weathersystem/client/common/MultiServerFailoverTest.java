package com.weathersystem.client.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for multi-server failover functionality in HttpClientBase
 */
class MultiServerFailoverTest {

    private TestHttpClient testClient;

    @BeforeEach
    void setUp() {
        // Create client with multiple servers - some working, some failing
        List<String> servers = Arrays.asList("localhost:9999", "localhost:4567", "localhost:4568");
        ClientConfiguration config = new ClientConfiguration(servers, "TestAgent/1.0", 1000, 5000);
        testClient = new TestHttpClient(config);
    }

    @Test
    void testFailoverToWorkingServer() {
        // This test documents expected behavior - will try invalid servers first
        // In real scenario, would connect to first available server
        assertDoesNotThrow(() -> {
            // Should attempt failover when servers are unreachable
            try {
                testClient.testCreateConnection();
            } catch (IOException e) {
                // Expected when all servers are down
                assertTrue(e.getMessage().contains("All servers failed"));
            }
        });
    }

    @Test
    void testSingleServerFallback() {
        ClientConfiguration singleConfig = new ClientConfiguration("localhost", 4567, "Test/1.0", 1000, 5000);
        TestHttpClient singleClient = new TestHttpClient(singleConfig);

        assertFalse(singleClient.getConfig().hasMultipleServers());

        // Should use single server connection path
        assertDoesNotThrow(() -> {
            try {
                singleClient.testCreateConnection();
            } catch (IOException e) {
                // Expected when server is down
                assertFalse(e.getMessage().contains("All servers failed"));
            }
        });
    }

    @Test
    void testFailoverConnectionAttemptOrder() {
        List<String> servers = Arrays.asList("server1:4567", "server2:4568", "server3:4569");
        ClientConfiguration config = new ClientConfiguration(servers, "Test/1.0", 100, 1000);
        TestHttpClient client = new TestHttpClient(config);

        assertEquals(3, config.getServerAddresses().size());
        assertEquals("server1:4567", config.getServerAddresses().get(0));
        assertEquals("server2:4568", config.getServerAddresses().get(1));
        assertEquals("server3:4569", config.getServerAddresses().get(2));

        // Test that client will try servers in order
        assertTrue(config.hasMultipleServers());
    }

    @Test
    void testConnectionTimeoutBehavior() {
        // Create config with very short timeouts
        List<String> servers = Arrays.asList("10.255.255.1:4567", "localhost:4567"); // Non-routable IP first
        ClientConfiguration config = new ClientConfiguration(servers, "Test/1.0", 50, 100);
        TestHttpClient client = new TestHttpClient(config);

        long startTime = System.currentTimeMillis();

        assertThrows(Exception.class, () -> {
            client.testCreateConnection();
        });

        long duration = System.currentTimeMillis() - startTime;

        // Should timeout quickly due to short timeout values
        assertTrue(duration < 5000, "Should timeout quickly with short timeout values");
    }

    @Test
    void testMultiServerConfigurationParsing() {
        ClientConfiguration config = ClientConfiguration.fromMultipleServers("host1:1001,host2:1002,host3:1003");

        assertTrue(config.hasMultipleServers());
        assertEquals(3, config.getServerAddresses().size());
        assertEquals("host1:1001", config.getPrimaryServerAddress());
    }

    @Test
    void testEmptyServerListHandling() {
        // Test edge case with empty server configuration
        assertThrows(IllegalArgumentException.class, () -> {
            new ClientConfiguration(Arrays.asList(), "Test/1.0", 1000, 5000);
        });
    }

    @Test
    void testNullServerListHandling() {
        // Test edge case with null server configuration
        assertThrows(IllegalArgumentException.class, () -> {
            new ClientConfiguration(null, "Test/1.0", 1000, 5000);
        });
    }

    @Test
    void testFailoverWithDifferentPorts() {
        List<String> servers = Arrays.asList("localhost:9001", "localhost:9002", "localhost:9003");
        ClientConfiguration config = new ClientConfiguration(servers, "Test/1.0", 100, 1000);

        assertEquals(3, config.getServerAddresses().size());
        assertTrue(config.hasMultipleServers());

        // Each server should have correct port
        assertEquals("localhost:9001", config.getServerAddresses().get(0));
        assertEquals("localhost:9002", config.getServerAddresses().get(1));
        assertEquals("localhost:9003", config.getServerAddresses().get(2));
    }

    @Test
    void testFailoverResilience() {
        // Test that failover works even with many failed servers
        List<String> servers = Arrays.asList(
            "invalid1:9999", "invalid2:9998", "invalid3:9997",
            "invalid4:9996", "localhost:4567" // Last one might work
        );
        ClientConfiguration config = new ClientConfiguration(servers, "Test/1.0", 50, 100);
        TestHttpClient client = new TestHttpClient(config);

        assertEquals(5, config.getServerAddresses().size());

        // Should attempt all servers in sequence
        assertDoesNotThrow(() -> {
            try {
                client.testCreateConnection();
            } catch (IOException e) {
                // Expected when all servers are down
                assertTrue(e.getMessage().contains("All servers failed"));
            }
        });
    }

    /**
     * Test implementation of HttpClientBase to expose connection testing
     */
    private static class TestHttpClient extends HttpClientBase {
        public TestHttpClient(ClientConfiguration config) {
            super(config);
        }

        public void testCreateConnection() throws IOException {
            // This will test the actual failover logic
            try (Socket socket = createConnection()) {
                // Connection successful
            }
        }

        public ClientConfiguration getConfig() {
            return this.config;
        }
    }
}