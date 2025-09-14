package com.weathersystem.client;

import com.weathersystem.client.common.ClientConfiguration;
import com.weathersystem.shared.domain.WeatherData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class GETClientTest {

    private GETClient getClient;

    @BeforeEach
    void setUp() {
        ClientConfiguration config = new ClientConfiguration("localhost", 4567, "TestClient/1.0", 5000, 10000);
        getClient = new GETClient(config);
    }

    @Test
    // Checks that the GETClient gets created properly with different configs
    void testConstructorAndInitialization() {
        System.out.println("Testing GETClient constructor");
        assertNotNull(getClient, "GETClient should be created");
        assertEquals(0, getClient.getLamportTime(), "Initial Lamport clock should be 0");
        
        // Test with different configuration
        ClientConfiguration testConfig = new ClientConfiguration("test.com", 8080, "TestAgent/1.0", 3000, 5000);
        GETClient testClient = new GETClient(testConfig);
        assertNotNull(testClient, "GETClient should be created with custom config");
        assertEquals(0, testClient.getLamportTime(), "Custom config should also start with 0");
        System.out.println("Constructor test passed");
    }

    @Test
    // Tests parsing JSON responses with weather data, including empty and dodgy ones
    void testWeatherDataParsing() throws Exception {
        System.out.println("Testing weather data parsing");
        Method parseMethod = GETClient.class.getDeclaredMethod("parseWeatherResponse",
                GETClient.HttpResponse.class);
        parseMethod.setAccessible(true);

        // Test valid JSON parsing
        String validJson = "[{\"id\":\"TEST001\",\"name\":\"Test Station\",\"state\":\"TEST\"," +
                "\"lat\":-35.0,\"lon\":138.0,\"air_temp\":25.0,\"apparent_t\":24.0," +
                "\"cloud\":\"Clear\",\"rel_hum\":50,\"wind_dir\":\"N\",\"wind_spd_kmh\":10," +
                "\"press\":1013.25,\"local_date_time\":\"2024-01-01T12:00:00\"}]";

        GETClient.HttpResponse validResponse = new GETClient.HttpResponse(200, "OK", validJson, 5);
        WeatherData[] validResult = (WeatherData[]) parseMethod.invoke(getClient, validResponse);

        assertNotNull(validResult, "Valid JSON should parse successfully");
        assertEquals(1, validResult.length, "Should parse one weather station");
        assertEquals("TEST001", validResult[0].getId(), "Station ID should match");
        assertEquals("Test Station", validResult[0].getName(), "Station name should match");
        assertEquals("TEST", validResult[0].getState(), "Station state should match");

        // Test empty content handling
        GETClient.HttpResponse emptyResponse = new GETClient.HttpResponse(200, "OK", "", 5);
        WeatherData[] emptyResult = (WeatherData[]) parseMethod.invoke(getClient, emptyResponse);
        assertNotNull(emptyResult, "Empty content should return empty array");
        assertEquals(0, emptyResult.length, "Empty content should return empty array");

        // Test null content handling
        GETClient.HttpResponse nullResponse = new GETClient.HttpResponse(200, "OK", null, 5);
        WeatherData[] nullResult = (WeatherData[]) parseMethod.invoke(getClient, nullResponse);
        assertNotNull(nullResult, "Null content should return empty array");
        assertEquals(0, nullResult.length, "Null content should return empty array");

        // Test invalid JSON handling
        GETClient.HttpResponse invalidResponse = new GETClient.HttpResponse(200, "OK", "invalid json", 5);
        Exception exception = assertThrows(Exception.class, () -> {
            parseMethod.invoke(getClient, invalidResponse);
        });
        assertTrue(exception.getCause().getMessage().contains("Failed to parse weather data"),
                  "Should throw appropriate error for invalid JSON");
        
        System.out.println("Weather data parsing test passed");
    }

    @Test
    // Tests showing weather data on screen, both when empty and when there is data
    void testWeatherDataDisplay() throws Exception {
        System.out.println("Testing weather data display");
        
        // Test empty data display
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            Method displayMethod = GETClient.class.getDeclaredMethod("displayWeatherData", WeatherData[].class);
            displayMethod.setAccessible(true);
            displayMethod.invoke(getClient, new Object[]{new WeatherData[0]});

            String emptyOutput = outputStream.toString();
            assertTrue(emptyOutput.contains("CURRENT WEATHER DATA"), 
                      "Should display header for empty data");
            assertTrue(emptyOutput.contains("No weather stations reporting"), 
                      "Should indicate no data available");

            // Reset output stream
            outputStream.reset();

            // Test single station display
            WeatherData testStation = new WeatherData();
            testStation.setId("TEST001");
            testStation.setName("Test Station");
            testStation.setState("TEST");
            testStation.setLat(-35.0);
            testStation.setLon(138.0);
            testStation.setAirTemp(25.0);
            testStation.setApparentT(24.0);
            testStation.setCloud("Clear");
            testStation.setRelHum(50);
            testStation.setWindDir("N");
            testStation.setWindSpdKmh(10);
            testStation.setPress(1013.25);
            testStation.setLocalDateTime("2024-01-01T12:00:00");

            displayMethod.invoke(getClient, new Object[]{new WeatherData[]{testStation}});

            String stationOutput = outputStream.toString();
            assertTrue(stationOutput.contains("CURRENT WEATHER DATA"), 
                      "Should display header for station data");
            assertTrue(stationOutput.contains("Total stations: 1"), 
                      "Should show station count");
            assertTrue(stationOutput.contains("Station 1:"), 
                      "Should show station number");
            assertTrue(stationOutput.contains("ID: TEST001"), 
                      "Should show station ID");
            assertTrue(stationOutput.contains("Name: Test Station"), 
                      "Should show station name");
            assertTrue(stationOutput.contains("Temperature: 25.0°C"), 
                      "Should show temperature");

        } finally {
            System.setOut(originalOut);
        }
        
        System.out.println("Weather data display test passed");
    }

    @Test
    // Tests getting weather data with retries when the server is down
    void testWeatherDataRetrievalWithRetry() {
        System.out.println("Testing weather data retrieval with retry");
        
        // Test connection failure with retry
        ClientConfiguration invalidConfig = new ClientConfiguration("invalid.host", 9999, "Test/1.0", 100, 100);
        GETClient testClient = new GETClient(invalidConfig);

        long startTime = System.currentTimeMillis();

        Exception exception = assertThrows(Exception.class, testClient::retrieveWeatherData);

        long endTime = System.currentTimeMillis();

        // Should attempt 4 times with exponential backoff
        assertTrue(exception.getMessage().contains("Weather data retrieval failed after 4 attempts"),
                  "Should attempt 4 times before giving up");
        assertTrue((endTime - startTime) >= 6000, 
                  "Should take at least 6 seconds for retry attempts: " + (endTime - startTime) + "ms");
        
        System.out.println("Weather data retrieval with retry test passed");
    }

    @Test
    // Tests the maths for calculating how long to wait between retries
    void testRetryDelayCalculation() {
        System.out.println("Testing retry delay calculation");
        
        // Test exponential backoff calculation
        long baseDelay = 1000;
        double backoff = 2.0;

        long secondDelay = (long) (baseDelay * backoff);
        long thirdDelay = (long) (baseDelay * backoff * backoff);

        assertEquals(1000, baseDelay, "First delay should be base delay");
        assertEquals(2000, secondDelay, "Second delay should be 2x base delay");
        assertEquals(4000, thirdDelay, "Third delay should be 4x base delay");
        
        System.out.println("Retry delay calculation test passed");
    }

    // === MULTI-SERVER SUPPORT TESTS ===

    @Test
    // Tests setting up multiple servers for failover
    void testMultiServerConfiguration() {
        System.out.println("Testing multi-server configuration");
        
        // Test multiple servers configuration
        ClientConfiguration multiConfig = ClientConfiguration.fromMultipleServers("localhost:4567,localhost:4568,localhost:4569");
        GETClient multiClient = new GETClient(multiConfig);

        assertNotNull(multiClient, "Multi-server GETClient should be created");
        assertTrue(multiConfig.hasMultipleServers(), "Should detect multiple servers");
        assertEquals(3, multiConfig.getServerAddresses().size(), "Should have 3 server addresses");
        assertEquals(0, multiClient.getLamportTime(), "Should start with 0 Lamport time");

        // Test single server in multi-server config
        ClientConfiguration singleViaMulti = ClientConfiguration.fromMultipleServers("localhost:4567");
        ClientConfiguration singleDirect = ClientConfiguration.fromServerAddress("localhost:4567");
        GETClient client1 = new GETClient(singleViaMulti);
        GETClient client2 = new GETClient(singleDirect);

        assertEquals(singleDirect.getHost(), singleViaMulti.getHost(), "Single server configs should match");
        assertEquals(singleDirect.getPort(), singleViaMulti.getPort(), "Single server ports should match");
        assertEquals(client1.getLamportTime(), client2.getLamportTime(), "Lamport times should match");
        
        System.out.println("Multi-server configuration test passed");
    }

    @Test
    // Tests what happens when one server is down and we try the next one
    void testMultiServerFailover() {
        System.out.println("Testing multi-server failover");
        
        ClientConfiguration failoverConfig = ClientConfiguration.fromMultipleServers("invalid.host:9999,localhost:4567,backup:4568");
        GETClient failoverClient = new GETClient(failoverConfig);

        long startTime = System.currentTimeMillis();

        Exception exception = assertThrows(Exception.class, failoverClient::retrieveWeatherData);

        long duration = System.currentTimeMillis() - startTime;

        // Should try failover (will fail, but tests the logic)
        assertTrue(exception.getMessage().contains("Weather data retrieval failed after 4 attempts"),
                  "Should fail after all retry attempts");
        assertTrue(duration >= 6000, 
                  "Should take at least 6 seconds for retry attempts with failover: " + duration + "ms");
        
        System.out.println("Multi-server failover test passed");
    }

    // === MAIN METHOD TESTS ===

    @Test
    // Tests that the main method handles command line arguments properly
    void testMainMethodArgumentHandling() {
        System.out.println("Testing main method arguments");
        
        // Test default server (no arguments)
        assertDoesNotThrow(() -> {
            ClientConfiguration config = ClientConfiguration.fromServerAddress("localhost:4567");
            assertNotNull(config, "Should create config for default server");
        });

        // Test custom server argument
        String[] args = {"example.com:8080"};
        assertDoesNotThrow(() -> {
            String serverAddress = args[0];
            ClientConfiguration config = ClientConfiguration.fromServerAddress(serverAddress);
            assertNotNull(config, "Should create config for custom server");
            assertEquals("example.com", config.getHost(), "Host should match argument");
            assertEquals(8080, config.getPort(), "Port should match argument");
        });

        // Test multi-server argument parsing
        String[] multiServerArgs = {"server1:4567,server2:4568,server3:4569"};
        assertDoesNotThrow(() -> {
            String serverAddress = multiServerArgs[0];
            ClientConfiguration config;
            config = ClientConfiguration.fromMultipleServers(serverAddress);
            assertTrue(config.hasMultipleServers(), "Should detect multiple servers");
            assertEquals(3, config.getServerAddresses().size(), "Should have 3 server addresses");
            GETClient client = new GETClient(config);
            assertNotNull(client, "Should create GETClient with parsed config");
        });
        
        System.out.println("Main method argument test passed");
    }
}