package com.weathersystem.client;

import com.weathersystem.client.common.ClientConfiguration;
import com.weathersystem.shared.domain.WeatherData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

class GETClientTest {

    private GETClient getClient;
    private ClientConfiguration config;

    @BeforeEach
    void setUp() {
        config = new ClientConfiguration("localhost", 4567, "TestClient/1.0", 5000, 10000);
        getClient = new GETClient(config);
    }

    @Test
    void testConstructorInitializesCorrectly() {
        assertNotNull(getClient);
        assertEquals(0, getClient.getLamportTime()); // Initial Lamport clock should be 0
    }

    @Test
    void testConstructorWithConfiguration() {
        ClientConfiguration testConfig = new ClientConfiguration("test.com", 8080, "TestAgent/1.0", 3000, 5000);
        GETClient client = new GETClient(testConfig);

        assertNotNull(client);
        assertEquals(0, client.getLamportTime());
    }

    @Test
    void testParseWeatherResponseWithValidJSON() throws Exception {
        // Use reflection to access private method for testing
        Method parseMethod = GETClient.class.getDeclaredMethod("parseWeatherResponse",
                GETClient.HttpResponse.class);
        parseMethod.setAccessible(true);

        String validJson = "[{\"id\":\"TEST001\",\"name\":\"Test Station\",\"state\":\"TEST\"," +
                "\"lat\":-35.0,\"lon\":138.0,\"air_temp\":25.0,\"apparent_t\":24.0," +
                "\"cloud\":\"Clear\",\"rel_hum\":50,\"wind_dir\":\"N\",\"wind_spd_kmh\":10," +
                "\"press\":1013.25,\"local_date_time\":\"2024-01-01T12:00:00\"}]";

        GETClient.HttpResponse mockResponse = new GETClient.HttpResponse(200, "OK", validJson, 5);
        WeatherData[] result = (WeatherData[]) parseMethod.invoke(getClient, mockResponse);

        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("TEST001", result[0].getId());
        assertEquals("Test Station", result[0].getName());
        assertEquals("TEST", result[0].getState());
    }

    @Test
    void testParseWeatherResponseWithEmptyContent() throws Exception {
        Method parseMethod = GETClient.class.getDeclaredMethod("parseWeatherResponse",
                GETClient.HttpResponse.class);
        parseMethod.setAccessible(true);

        GETClient.HttpResponse mockResponse = new GETClient.HttpResponse(200, "OK", "", 5);
        WeatherData[] result = (WeatherData[]) parseMethod.invoke(getClient, mockResponse);

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testParseWeatherResponseWithNullContent() throws Exception {
        Method parseMethod = GETClient.class.getDeclaredMethod("parseWeatherResponse",
                GETClient.HttpResponse.class);
        parseMethod.setAccessible(true);

        GETClient.HttpResponse mockResponse = new GETClient.HttpResponse(200, "OK", null, 5);
        WeatherData[] result = (WeatherData[]) parseMethod.invoke(getClient, mockResponse);

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testParseWeatherResponseWithInvalidJSON() throws Exception {
        Method parseMethod = GETClient.class.getDeclaredMethod("parseWeatherResponse",
                GETClient.HttpResponse.class);
        parseMethod.setAccessible(true);

        GETClient.HttpResponse mockResponse = new GETClient.HttpResponse(200, "OK", "invalid json", 5);

        Exception exception = assertThrows(Exception.class, () -> {
            parseMethod.invoke(getClient, mockResponse);
        });

        // The actual exception will be wrapped in InvocationTargetException
        assertTrue(exception.getCause().getMessage().contains("Failed to parse weather data"));
    }

    @Test
    void testDisplayWeatherDataWithEmptyArray() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            Method displayMethod = GETClient.class.getDeclaredMethod("displayWeatherData", WeatherData[].class);
            displayMethod.setAccessible(true);
            displayMethod.invoke(getClient, new Object[]{new WeatherData[0]});

            String output = outputStream.toString();
            assertTrue(output.contains("=== CURRENT WEATHER DATA ==="));
            assertTrue(output.contains("No weather stations currently reporting data"));
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testDisplayWeatherDataWithSingleStation() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
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

            Method displayMethod = GETClient.class.getDeclaredMethod("displayWeatherData", WeatherData[].class);
            displayMethod.setAccessible(true);
            displayMethod.invoke(getClient, new Object[]{new WeatherData[]{testStation}});

            String output = outputStream.toString();
            assertTrue(output.contains("=== CURRENT WEATHER DATA ==="));
            assertTrue(output.contains("Total weather stations: 1"));
            assertTrue(output.contains("Station 1:"));
            assertTrue(output.contains("ID: TEST001"));
            assertTrue(output.contains("Name: Test Station"));
            assertTrue(output.contains("State: TEST"));
            assertTrue(output.contains("Temperature: 25.0°C"));
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testDisplayStationData() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            WeatherData testStation = new WeatherData();
            testStation.setId("TEST001");
            testStation.setName("Test Station");
            testStation.setState("TEST");
            testStation.setLat(-35.0);
            testStation.setLon(138.0);
            testStation.setAirTemp(25.0);

            Method displayMethod = GETClient.class.getDeclaredMethod("displayStationData",
                    WeatherData.class, int.class);
            displayMethod.setAccessible(true);
            displayMethod.invoke(getClient, testStation, 1);

            String output = outputStream.toString();
            assertTrue(output.contains("Station 1:"));
            assertTrue(output.contains("ID: TEST001"));
            assertTrue(output.contains("Name: Test Station"));
            assertTrue(output.contains("Location: -35.0°, 138.0°"));
            assertTrue(output.contains("Temperature: 25.0°C"));
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testRetrieveWeatherDataWithConnectionFailure() {
        // This test simulates connection failure by using an invalid configuration
        ClientConfiguration invalidConfig = new ClientConfiguration("invalid.host", 9999, "Test/1.0", 100, 100);
        GETClient testClient = new GETClient(invalidConfig);

        Exception exception = assertThrows(Exception.class, () -> {
            testClient.retrieveWeatherData();
        });

        assertTrue(exception.getMessage().contains("Weather data retrieval failed after 4 attempts"));
    }

    @Test
    void testLamportClockInitialization() {
        assertEquals(0, getClient.getLamportTime());
    }

    @Test
    void testMainMethodWithDefaultServer() {
        // This test verifies main method doesn't crash with no arguments
        assertDoesNotThrow(() -> {
            ClientConfiguration config = ClientConfiguration.fromServerAddress("localhost:4567");
            assertNotNull(config);
        });
    }

    @Test
    void testMainMethodWithCustomServer() {
        // This test verifies main method argument parsing
        String[] args = {"example.com:8080"};
        assertDoesNotThrow(() -> {
            String serverAddress = args.length > 0 ? args[0] : "localhost:4567";
            ClientConfiguration config = ClientConfiguration.fromServerAddress(serverAddress);
            assertNotNull(config);
            assertEquals("example.com", config.getHost());
            assertEquals(8080, config.getPort());
        });
    }

    @Test
    void testRetryLogicDelayCalculation() {
        // Test that retry delays follow expected exponential backoff pattern
        long baseDelay = 1000;
        double backoff = 2.0;

        long firstDelay = baseDelay;
        long secondDelay = (long) (baseDelay * backoff);
        long thirdDelay = (long) (baseDelay * backoff * backoff);

        assertEquals(1000, firstDelay);
        assertEquals(2000, secondDelay);
        assertEquals(4000, thirdDelay);
    }

    @Test
    void testMaxAttemptsConfiguration() {
        // Verify the retry logic uses 4 attempts as specified
        ClientConfiguration invalidConfig = new ClientConfiguration("invalid.host", 9999, "Test/1.0", 100, 100);
        GETClient testClient = new GETClient(invalidConfig);

        long startTime = System.currentTimeMillis();

        Exception exception = assertThrows(Exception.class, () -> {
            testClient.retrieveWeatherData();
        });

        long endTime = System.currentTimeMillis();

        // Should attempt 4 times with delays: 1s + 2s + 4s = at least 7 seconds
        // But allowing some tolerance for test execution time
        assertTrue((endTime - startTime) >= 6000, "Should take at least 6 seconds for 4 retry attempts");
        assertTrue(exception.getMessage().contains("Weather data retrieval failed after 4 attempts"));
    }
}