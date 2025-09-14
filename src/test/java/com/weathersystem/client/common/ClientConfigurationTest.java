package com.weathersystem.client.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientConfigurationTest {


    @Test
    // Tests constructor with valid parameters and verifies all fields are set correctly
    void testConstructorWithValidParameters() {
        System.out.println("Testing constructor with valid parameters...");
        ClientConfiguration config = new ClientConfiguration(
                "localhost", 8080, "TestAgent/1.0", 3000, 5000);

        assertEquals("localhost", config.getHost(), "Host should match input");
        assertEquals(8080, config.getPort(), "Port should match input");
        assertEquals("TestAgent/1.0", config.getUserAgent(), "User agent should match input");
        assertEquals(3000, config.getConnectionTimeoutMs(), "Connection timeout should match input");
        assertEquals(5000, config.getReadTimeoutMs(), "Read timeout should match input");
        System.out.println("✓ Constructor test passed");
    }

    @Test
    // Tests parsing valid server address string and validates host, port, and default values
    void testFromServerAddressValidInput() {
        System.out.println("Testing fromServerAddress with valid input...");
        ClientConfiguration config = ClientConfiguration.fromServerAddress("example.com:9090");

        assertEquals("example.com", config.getHost(), "Host should be parsed correctly");
        assertEquals(9090, config.getPort(), "Port should be parsed correctly");
        assertEquals("WeatherClient/1.0", config.getUserAgent(), "Should use default user agent");
        assertEquals("example.com:9090", config.getServerUrl(), "Server URL should be formatted correctly");
        System.out.println("✓ Valid server address test passed");
    }

    @Test
    // Tests server address parsing with null input to verify default values are used
    void testFromServerAddressWithDefaults() {
        System.out.println("Testing fromServerAddress with null input (defaults)...");
        ClientConfiguration config = ClientConfiguration.fromServerAddress(null);

        assertEquals("localhost", config.getHost(), "Should use default host");
        assertEquals(4567, config.getPort(), "Should use default port");
        assertEquals("WeatherClient/1.0", config.getUserAgent(), "Should use default user agent");
        assertEquals(5000, config.getConnectionTimeoutMs(), "Should use default connection timeout");
        assertEquals(10000, config.getReadTimeoutMs(), "Should use default read timeout");
        System.out.println("✓ Default values test passed");
    }

    @Test
    // Tests handling of invalid port in server address string and fallback to default port
    void testFromServerAddressInvalidPort() {
        System.out.println("Testing fromServerAddress with invalid port...");
        ClientConfiguration config = ClientConfiguration.fromServerAddress("example.com:invalid");

        assertEquals("example.com", config.getHost(), "Host should be parsed");
        assertEquals(4567, config.getPort(), "Should fallback to default port on invalid input");
        System.out.println("✓ Invalid port handling test passed");
    }

    @Test
    // Tests validation of multi-server constructor with null and empty server lists
    void testMultiServerConstructorValidation() {
        System.out.println("Testing multi-server constructor validation...");
        
        assertThrows(IllegalArgumentException.class, () -> {
            new ClientConfiguration(null, "TestAgent/1.0", 5000, 10000);
        }, "Should throw exception for null server addresses");

        assertThrows(IllegalArgumentException.class, () -> {
            new ClientConfiguration(java.util.Collections.emptyList(), "TestAgent/1.0", 5000, 10000);
        }, "Should throw exception for empty server addresses");
        
        System.out.println("✓ Constructor validation test passed");
    }

    @Test
    // Tests parsing multiple server addresses and verifies correct configuration setup
    void testFromMultipleServersValidInput() {
        System.out.println("Testing fromMultipleServers with valid input...");
        ClientConfiguration config = ClientConfiguration.fromMultipleServers("localhost:4567,localhost:4568,localhost:4569");

        assertEquals(3, config.getServerAddresses().size(), "Should have 3 server addresses");
        assertEquals("localhost:4567", config.getServerAddresses().get(0), "First server should match");
        assertEquals("localhost:4568", config.getServerAddresses().get(1), "Second server should match");
        assertEquals("localhost:4569", config.getServerAddresses().get(2), "Third server should match");
        assertTrue(config.hasMultipleServers(), "Should detect multiple servers");
        assertEquals("localhost:4567", config.getPrimaryServerAddress(), "Primary server should be first");
        System.out.println("✓ Multiple servers test passed");
    }

    @Test
    // Tests parsing mixed server addresses with and without explicit ports
    void testFromMultipleServersWithDefaultPorts() {
        System.out.println("Testing fromMultipleServers with mixed port specifications...");
        ClientConfiguration config = ClientConfiguration.fromMultipleServers("server1,server2:8080,server3");

        assertEquals(3, config.getServerAddresses().size(), "Should have 3 server addresses");
        assertEquals("server1:4567", config.getServerAddresses().get(0), "Should add default port");
        assertEquals("server2:8080", config.getServerAddresses().get(1), "Should preserve specified port");
        assertEquals("server3:4567", config.getServerAddresses().get(2), "Should add default port");
        System.out.println("✓ Mixed port specifications test passed");
    }

    @Test
    // Tests edge cases for multiple server parsing including empty, null, and single server inputs
    void testFromMultipleServersEdgeCases() {
        System.out.println("Testing fromMultipleServers edge cases...");
        
        // Test empty string
        ClientConfiguration emptyConfig = ClientConfiguration.fromMultipleServers("");
        assertEquals(1, emptyConfig.getServerAddresses().size(), "Empty string should create single default server");
        assertFalse(emptyConfig.hasMultipleServers(), "Should not have multiple servers");
        
        // Test null
        ClientConfiguration nullConfig = ClientConfiguration.fromMultipleServers(null);
        assertEquals(1, nullConfig.getServerAddresses().size(), "Null should create single default server");
        assertFalse(nullConfig.hasMultipleServers(), "Should not have multiple servers");
        
        // Test single server
        ClientConfiguration singleConfig = ClientConfiguration.fromMultipleServers("example.com:8080");
        assertEquals(1, singleConfig.getServerAddresses().size(), "Single server should create one entry");
        assertFalse(singleConfig.hasMultipleServers(), "Should not have multiple servers");
        
        System.out.println("✓ Edge cases test passed");
    }

    @Test
    // Tests consistency between single and multi server configuration methods
    void testConfigurationConsistency() {
        System.out.println("Testing configuration consistency...");
        ClientConfiguration singleConfig = ClientConfiguration.fromServerAddress("localhost:4567");
        ClientConfiguration multiConfig = ClientConfiguration.fromMultipleServers("localhost:4567");

        assertEquals(singleConfig.getHost(), multiConfig.getHost(), "Single and multi config should have same host");
        assertEquals(singleConfig.getPort(), multiConfig.getPort(), "Single and multi config should have same port");
        assertEquals(singleConfig.getServerUrl(), multiConfig.getServerUrl(), "Single and multi config should have same server URL");
        System.out.println("✓ Configuration consistency test passed");
    }
}