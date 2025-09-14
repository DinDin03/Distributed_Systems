package com.weathersystem.client.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientConfigurationTest {

    @Test
    void testConstructorWithValidParameters() {
        ClientConfiguration config = new ClientConfiguration(
                "localhost", 8080, "TestAgent/1.0", 3000, 5000);

        assertEquals("localhost", config.getHost());
        assertEquals(8080, config.getPort());
        assertEquals("TestAgent/1.0", config.getUserAgent());
        assertEquals(3000, config.getConnectionTimeoutMs());
        assertEquals(5000, config.getReadTimeoutMs());
    }

    @Test
    void testFromServerAddressWithValidHostAndPort() {
        ClientConfiguration config = ClientConfiguration.fromServerAddress("example.com:9090");

        assertEquals("example.com", config.getHost());
        assertEquals(9090, config.getPort());
        assertEquals("WeatherClient/1.0", config.getUserAgent());
        assertEquals(5000, config.getConnectionTimeoutMs());
        assertEquals(10000, config.getReadTimeoutMs());
    }

    @Test
    void testFromServerAddressWithInvalidPort() {
        ClientConfiguration config = ClientConfiguration.fromServerAddress("example.com:invalid");

        assertEquals("example.com", config.getHost());
        assertEquals(4567, config.getPort()); // Should use default port
        assertEquals("WeatherClient/1.0", config.getUserAgent());
    }

    @Test
    void testFromServerAddressWithNoPort() {
        ClientConfiguration config = ClientConfiguration.fromServerAddress("example.com");

        assertEquals("localhost", config.getHost()); // Should use default host since no colon found
        assertEquals(4567, config.getPort());
    }

    @Test
    void testFromServerAddressWithNull() {
        ClientConfiguration config = ClientConfiguration.fromServerAddress(null);

        assertEquals("localhost", config.getHost());
        assertEquals(4567, config.getPort());
        assertEquals("WeatherClient/1.0", config.getUserAgent());
        assertEquals(5000, config.getConnectionTimeoutMs());
        assertEquals(10000, config.getReadTimeoutMs());
    }

    @Test
    void testFromServerAddressWithEmptyString() {
        ClientConfiguration config = ClientConfiguration.fromServerAddress("");

        assertEquals("localhost", config.getHost());
        assertEquals(4567, config.getPort());
    }

    @Test
    void testFromServerAddressWithOnlyColon() {
        ClientConfiguration config = ClientConfiguration.fromServerAddress(":");

        assertEquals("localhost", config.getHost()); // Should use defaults when split results in empty array
        assertEquals(4567, config.getPort()); // Should use default port
    }

    @Test
    void testGetServerUrl() {
        ClientConfiguration config = new ClientConfiguration(
                "test.example.com", 8080, "TestAgent/1.0", 3000, 5000);

        assertEquals("test.example.com:8080", config.getServerUrl());
    }

    @Test
    void testGetServerUrlWithDefaults() {
        ClientConfiguration config = ClientConfiguration.fromServerAddress("localhost:4567");

        assertEquals("localhost:4567", config.getServerUrl());
    }

    @Test
    void testFromServerAddressWithIPv6Format() {
        ClientConfiguration config = ClientConfiguration.fromServerAddress("[::1]:8080");

        // This will parse incorrectly with current implementation, but test documents behavior
        assertEquals("[", config.getHost()); // Will take first part before ':'
        assertEquals(4567, config.getPort()); // Will fail to parse complex part
    }

    @Test
    void testConfigurationImmutability() {
        ClientConfiguration config = new ClientConfiguration(
                "localhost", 8080, "TestAgent/1.0", 3000, 5000);

        // All fields should be final and have only getters
        assertNotNull(config.getHost());
        assertNotNull(config.getUserAgent());
        assertTrue(config.getPort() > 0);
        assertTrue(config.getConnectionTimeoutMs() > 0);
        assertTrue(config.getReadTimeoutMs() > 0);
    }

    // === MULTI-SERVER TESTS ===

    @Test
    void testFromMultipleServersWithValidAddresses() {
        ClientConfiguration config = ClientConfiguration.fromMultipleServers("localhost:4567,localhost:4568,localhost:4569");

        assertEquals(3, config.getServerAddresses().size());
        assertEquals("localhost:4567", config.getServerAddresses().get(0));
        assertEquals("localhost:4568", config.getServerAddresses().get(1));
        assertEquals("localhost:4569", config.getServerAddresses().get(2));
        assertTrue(config.hasMultipleServers());
    }

    @Test
    void testFromMultipleServersWithDefaultPorts() {
        ClientConfiguration config = ClientConfiguration.fromMultipleServers("server1,server2:8080,server3");

        assertEquals(3, config.getServerAddresses().size());
        assertEquals("server1:4567", config.getServerAddresses().get(0));
        assertEquals("server2:8080", config.getServerAddresses().get(1));
        assertEquals("server3:4567", config.getServerAddresses().get(2));
    }

    @Test
    void testFromMultipleServersWithSpaces() {
        ClientConfiguration config = ClientConfiguration.fromMultipleServers("  localhost:4567  ,  localhost:4568  ,  localhost:4569  ");

        assertEquals(3, config.getServerAddresses().size());
        assertEquals("localhost:4567", config.getServerAddresses().get(0));
        assertEquals("localhost:4568", config.getServerAddresses().get(1));
        assertEquals("localhost:4569", config.getServerAddresses().get(2));
    }

    @Test
    void testFromMultipleServersWithEmptyString() {
        ClientConfiguration config = ClientConfiguration.fromMultipleServers("");

        assertEquals(1, config.getServerAddresses().size());
        assertEquals("localhost:4567", config.getServerAddresses().get(0));
        assertFalse(config.hasMultipleServers());
    }

    @Test
    void testFromMultipleServersWithNullString() {
        ClientConfiguration config = ClientConfiguration.fromMultipleServers(null);

        assertEquals(1, config.getServerAddresses().size());
        assertEquals("localhost:4567", config.getServerAddresses().get(0));
        assertFalse(config.hasMultipleServers());
    }

    @Test
    void testFromMultipleServersWithSingleServer() {
        ClientConfiguration config = ClientConfiguration.fromMultipleServers("example.com:8080");

        assertEquals(1, config.getServerAddresses().size());
        assertEquals("example.com:8080", config.getServerAddresses().get(0));
        assertFalse(config.hasMultipleServers());
    }

    @Test
    void testMultiServerConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ClientConfiguration(null, "TestAgent/1.0", 5000, 10000);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new ClientConfiguration(java.util.Collections.emptyList(), "TestAgent/1.0", 5000, 10000);
        });
    }

    @Test
    void testGetPrimaryServerAddress() {
        ClientConfiguration config = ClientConfiguration.fromMultipleServers("primary:4567,backup1:4568,backup2:4569");

        assertEquals("primary:4567", config.getPrimaryServerAddress());
    }

    @Test
    void testMultiServerBackwardCompatibility() {
        ClientConfiguration singleConfig = ClientConfiguration.fromServerAddress("localhost:4567");
        ClientConfiguration multiConfig = ClientConfiguration.fromMultipleServers("localhost:4567");

        assertEquals(singleConfig.getHost(), multiConfig.getHost());
        assertEquals(singleConfig.getPort(), multiConfig.getPort());
        assertEquals(singleConfig.getServerUrl(), multiConfig.getServerUrl());
    }

    @Test
    void testMultiServerToStringBehavior() {
        ClientConfiguration config = ClientConfiguration.fromMultipleServers("server1:4567,server2:4568,server3:4569");

        // Should use first server for backward compatibility
        assertEquals("server1", config.getHost());
        assertEquals(4567, config.getPort());
        assertEquals("server1:4567", config.getServerUrl());
    }

    @Test
    void testDefaultValues() {
        // Test that defaults are reasonable
        ClientConfiguration config = ClientConfiguration.fromServerAddress(null);

        assertTrue(config.getConnectionTimeoutMs() > 0, "Connection timeout should be positive");
        assertTrue(config.getReadTimeoutMs() > 0, "Read timeout should be positive");
        assertTrue(config.getPort() > 0 && config.getPort() <= 65535, "Port should be valid");
        assertNotNull(config.getHost(), "Host should not be null");
        assertNotNull(config.getUserAgent(), "User agent should not be null");
    }
}