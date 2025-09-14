package com.weathersystem.server.handlers;

import com.weathersystem.server.http.HttpRequest;
import com.weathersystem.server.services.WeatherDataService;
import com.weathersystem.shared.domain.WeatherData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for GetRequestHandler class.
 * Tests weather data retrieval, JSON response formatting, and various data scenarios.
 */
class GetRequestHandlerTest {

    private GetRequestHandler getHandler;
    private WeatherDataService weatherDataService;
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() {
        weatherDataService = new WeatherDataService();
        getHandler = new GetRequestHandler(weatherDataService);
        
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
    }

    private WeatherData createSampleWeatherData(String id, String name) {
        WeatherData data = new WeatherData();
        data.setId(id);
        data.setName(name);
        data.setState("TEST");
        data.setTimeZone("UTC");
        data.setLat(-35.0);
        data.setLon(138.0);
        data.setLocalDateTime("15/04:00pm");
        data.setLocalDateTimeFull("20230715160000");
        data.setAirTemp(20.0);
        data.setApparentT(18.5);
        data.setCloud("Clear");
        data.setDewpt(10.0);
        data.setPress(1013.0);
        data.setRelHum(60);
        data.setWindDir("N");
        data.setWindSpdKmh(10);
        data.setWindSpdKt(5);
        return data;
    }

    private HttpRequest createHttpRequest(String method, int contentLength, long lamportTime) {
        return new HttpRequest(method, "/weather.json", "HTTP/1.1", 
                              java.util.Map.of(), contentLength, lamportTime);
    }

    private BufferedReader createBufferedReader(String content) {
        return new BufferedReader(new StringReader(content));
    }

    private String getResponseContent() {
        printWriter.flush();
        return stringWriter.toString();
    }

    // === CORE FUNCTIONALITY TESTS ===

    @Test
    void testHandleGetWithNoData() throws Exception {
        System.out.println("Testing GET request with no weather data...");
        HttpRequest request = createHttpRequest("GET", 0, 5L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 5L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 5"), "Should include Lamport time in response");
        assertTrue(response.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response.contains("[]"), "Should return empty JSON array for no data");
        System.out.println("✓ No data test passed");
    }

    @Test
    void testHandleGetWithWeatherData() throws Exception {
        System.out.println("Testing GET request with weather data...");
        
        // Add single station
        WeatherData station = createSampleWeatherData("SINGLE001", "Single Station");
        weatherDataService.storeWeatherData(station);

        HttpRequest request = createHttpRequest("GET", 0, 7L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 7L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 7"), "Should include Lamport time in response");
        assertTrue(response.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response.contains("SINGLE001"), "Should include station ID in response");
        assertTrue(response.contains("Single Station"), "Should include station name in response");
        
        // Test multiple stations
        WeatherData station2 = createSampleWeatherData("MULTI002", "Second Station");
        WeatherData station3 = createSampleWeatherData("MULTI003", "Third Station");
        weatherDataService.storeWeatherData(station2);
        weatherDataService.storeWeatherData(station3);

        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        request = createHttpRequest("GET", 0, 9L);
        reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 9L);

        response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK for multiple stations");
        assertTrue(response.contains("MULTI002"), "Should include second station ID");
        assertTrue(response.contains("MULTI003"), "Should include third station ID");
        assertTrue(response.contains("Second Station"), "Should include second station name");
        assertTrue(response.contains("Third Station"), "Should include third station name");
        
        System.out.println("✓ Weather data test passed");
    }

    @Test
    void testHandleGetWithDataUpdate() throws Exception {
        System.out.println("Testing GET request after data update...");
        
        // Add initial station
        WeatherData initialStation = createSampleWeatherData("UPDATE001", "Initial Name");
        weatherDataService.storeWeatherData(initialStation);

        // Update the station
        WeatherData updatedStation = createSampleWeatherData("UPDATE001", "Updated Name");
        updatedStation.setAirTemp(25.0);
        weatherDataService.storeWeatherData(updatedStation);

        HttpRequest request = createHttpRequest("GET", 0, 25L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 25L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 25"), "Should include Lamport time in response");
        assertTrue(response.contains("UPDATE001"), "Should include station ID");
        assertTrue(response.contains("Updated Name"), "Should include updated station name");
        assertTrue(response.contains("25.0"), "Should include updated temperature");
        assertFalse(response.contains("Initial Name"), "Should not include old station name");
        
        System.out.println("✓ Data update test passed");
    }

    // === DATA SCENARIOS TESTS ===

    @Test
    void testHandleGetWithVariousDataTypes() throws Exception {
        System.out.println("Testing GET request with various data types...");
        
        // Test special characters
        WeatherData specialStation = createSampleWeatherData("SPECIAL001", "Station with special chars: ñáéíóú");
        specialStation.setCloud("Partly \"cloudy\" with mixed conditions");
        specialStation.setWindDir("SW");
        weatherDataService.storeWeatherData(specialStation);

        // Test numeric precision
        WeatherData precisionStation = createSampleWeatherData("PRECISION001", "Precision Station");
        precisionStation.setAirTemp(13.123456789);
        precisionStation.setLat(-34.987654321);
        precisionStation.setLon(138.123456789);
        precisionStation.setPress(1023.987654321);
        weatherDataService.storeWeatherData(precisionStation);

        // Test negative values
        WeatherData negativeStation = createSampleWeatherData("NEGATIVE001", "Negative Values Station");
        negativeStation.setAirTemp(-5.5);
        negativeStation.setLat(-90.0);
        negativeStation.setLon(-180.0);
        negativeStation.setPress(-10.0);
        weatherDataService.storeWeatherData(negativeStation);

        // Test zero values
        WeatherData zeroStation = createSampleWeatherData("ZERO001", "Zero Values Station");
        zeroStation.setAirTemp(0.0);
        zeroStation.setLat(0.0);
        zeroStation.setLon(0.0);
        zeroStation.setPress(0.0);
        zeroStation.setRelHum(0);
        zeroStation.setWindSpdKmh(0);
        weatherDataService.storeWeatherData(zeroStation);

        HttpRequest request = createHttpRequest("GET", 0, 15L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 15L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 15"), "Should include Lamport time in response");
        
        // Verify special characters
        assertTrue(response.contains("SPECIAL001"), "Should include special station ID");
        assertTrue(response.contains("ñáéíóú"), "Should preserve special characters in name");
        assertTrue(response.contains("cloudy"), "Should preserve cloud description");
        
        // Verify numeric precision
        assertTrue(response.contains("PRECISION001"), "Should include precision station ID");
        assertTrue(response.contains("13.123456789"), "Should preserve temperature precision");
        assertTrue(response.contains("-34.987654321"), "Should preserve latitude precision");
        assertTrue(response.contains("138.123456789"), "Should preserve longitude precision");
        
        // Verify negative values
        assertTrue(response.contains("NEGATIVE001"), "Should include negative station ID");
        assertTrue(response.contains("-5.5"), "Should preserve negative temperature");
        assertTrue(response.contains("-90.0"), "Should preserve negative latitude");
        assertTrue(response.contains("-180.0"), "Should preserve negative longitude");
        
        // Verify zero values
        assertTrue(response.contains("ZERO001"), "Should include zero station ID");
        assertTrue(response.contains("Zero Values Station"), "Should include zero station name");
        
        System.out.println("✓ Various data types test passed");
    }

    @Test
    void testHandleGetWithUnicodeAndEdgeCases() throws Exception {
        System.out.println("Testing GET request with Unicode and edge cases...");
        
        // Test Unicode characters
        WeatherData unicodeStation = createSampleWeatherData("UNICODE001", "Station with Unicode: 中文 日本語 한국어 العربية");
        unicodeStation.setCloud("Partly cloudy with 中文 characters");
        unicodeStation.setWindDir("北"); // North in Chinese
        weatherDataService.storeWeatherData(unicodeStation);

        // Test empty fields
        WeatherData emptyFieldsStation = createSampleWeatherData("EMPTY001", "Empty Fields Station");
        emptyFieldsStation.setCloud(""); // Empty cloud field
        emptyFieldsStation.setWindDir(""); // Empty wind direction
        weatherDataService.storeWeatherData(emptyFieldsStation);

        // Test large values
        WeatherData largeStation = createSampleWeatherData("LARGE001", "Large Values Station");
        largeStation.setAirTemp(999.999);
        largeStation.setLat(90.0);
        largeStation.setLon(180.0);
        largeStation.setPress(9999.999);
        largeStation.setRelHum(100);
        largeStation.setWindSpdKmh(999);
        weatherDataService.storeWeatherData(largeStation);

        HttpRequest request = createHttpRequest("GET", 0, 20L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 20L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 20"), "Should include Lamport time in response");
        
        // Verify Unicode characters
        assertTrue(response.contains("UNICODE001"), "Should include Unicode station ID");
        assertTrue(response.contains("中文"), "Should preserve Chinese characters");
        assertTrue(response.contains("日本語"), "Should preserve Japanese characters");
        assertTrue(response.contains("한국어"), "Should preserve Korean characters");
        assertTrue(response.contains("العربية"), "Should preserve Arabic characters");
        
        // Verify empty fields
        assertTrue(response.contains("EMPTY001"), "Should include empty fields station ID");
        assertTrue(response.contains("Empty Fields Station"), "Should include empty fields station name");
        
        // Verify large values
        assertTrue(response.contains("LARGE001"), "Should include large station ID");
        assertTrue(response.contains("999.999"), "Should preserve large temperature");
        assertTrue(response.contains("90.0"), "Should preserve large latitude");
        assertTrue(response.contains("180.0"), "Should preserve large longitude");
        assertTrue(response.contains("9999.999"), "Should preserve large pressure");
        
        System.out.println("✓ Unicode and edge cases test passed");
    }

    // === RESPONSE FORMAT TESTS ===

    @Test
    void testHandleGetResponseFormat() throws Exception {
        System.out.println("Testing GET request response format...");
        
        // Add a station for testing response format
        WeatherData station = createSampleWeatherData("FORMAT001", "Format Test Station");
        weatherDataService.storeWeatherData(station);

        HttpRequest request = createHttpRequest("GET", 0, 35L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 35L);

        String response = getResponseContent();
        
        // Test response format
        assertTrue(response.startsWith("HTTP/1.1 200 OK"), "Should start with HTTP status line");
        assertTrue(response.contains("Content-Type: application/json"), "Should include content type header");
        assertTrue(response.contains("Lamport-Time: 35"), "Should include Lamport time header");
        assertTrue(response.contains("Content-Length:"), "Should include content length header");
        assertTrue(response.contains("\r\n\r\n"), "Should have proper header/body separator");
        
        // Test JSON format
        assertTrue(response.contains("["), "Should start with JSON array");
        assertTrue(response.contains("]"), "Should end with JSON array");
        assertTrue(response.contains("{"), "Should contain JSON object");
        assertTrue(response.contains("}"), "Should contain closing JSON object");
        
        System.out.println("✓ Response format test passed");
    }

    @Test
    void testHandleGetWithMultipleStations() throws Exception {
        System.out.println("Testing GET request with multiple stations...");
        
        // Add multiple stations
        for (int i = 0; i < 5; i++) {
            WeatherData station = createSampleWeatherData("MULTI" + i, "Station " + i);
            station.setAirTemp(i * 1.5);
            weatherDataService.storeWeatherData(station);
        }

        HttpRequest request = createHttpRequest("GET", 0, 30L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 30L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 30"), "Should include Lamport time in response");
        
        // Verify all stations are included
        for (int i = 0; i < 5; i++) {
            assertTrue(response.contains("MULTI" + i), "Should include station " + i);
            assertTrue(response.contains("Station " + i), "Should include station name " + i);
        }
        
        System.out.println("✓ Multiple stations test passed");
    }
}
