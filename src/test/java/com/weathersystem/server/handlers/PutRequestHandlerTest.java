package com.weathersystem.server.handlers;

import com.weathersystem.server.http.HttpRequest;
import com.weathersystem.server.http.HttpResponseBuilder;
import com.weathersystem.server.http.HttpStatusCodes;
import com.weathersystem.server.services.WeatherDataService;
import com.weathersystem.shared.domain.WeatherData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for PutRequestHandler class.
 * Tests weather data storage, JSON parsing, validation, and various error scenarios.
 */
class PutRequestHandlerTest {

    @TempDir
    Path tempDir;

    private PutRequestHandler putHandler;
    private WeatherDataService weatherDataService;
    private StringWriter stringWriter;
    private PrintWriter printWriter;
    private boolean dataChangedCallbackCalled;

    @BeforeEach
    void setUp() {
        weatherDataService = new WeatherDataService();
        dataChangedCallbackCalled = false;
        
        putHandler = new PutRequestHandler(weatherDataService, () -> {
            dataChangedCallbackCalled = true;
        });
        
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

    private String createValidJsonData(String id, String name) {
        WeatherData data = createSampleWeatherData(id, name);
        return String.format(
            "{\"id\":\"%s\",\"name\":\"%s\",\"state\":\"TEST\",\"time_zone\":\"UTC\"," +
            "\"lat\":-35.0,\"lon\":138.0,\"local_date_time\":\"15/04:00pm\"," +
            "\"local_date_time_full\":\"20230715160000\",\"air_temp\":20.0," +
            "\"apparent_t\":18.5,\"cloud\":\"Clear\",\"dewpt\":10.0," +
            "\"press\":1013.0,\"rel_hum\":60,\"wind_dir\":\"N\"," +
            "\"wind_spd_kmh\":10,\"wind_spd_kt\":5}",
            id, name
        );
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
    // Tests PUT request handling with valid weather data including new and existing stations
    void testHandleValidWeatherData() throws Exception {
        System.out.println("Testing PUT request with valid weather data...");
        
        // Test new station
        String jsonData = createValidJsonData("NEW001", "New Station");
        HttpRequest request = createHttpRequest("PUT", jsonData.length(), 5L);
        BufferedReader reader = createBufferedReader(jsonData);

        putHandler.handle(request, reader, printWriter, 5L);

        String response = getResponseContent();
        assertTrue(response.contains("201 Created"), "Should return 201 for new station");
        assertTrue(response.contains("Lamport-Time: 5"), "Should include Lamport time in response");
        assertTrue(dataChangedCallbackCalled, "Data changed callback should be called");
        
        // Verify data was stored
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should have one station stored");
        assertEquals("NEW001", allData[0].getId(), "Should have correct station ID");
        
        // Test existing station update
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        dataChangedCallbackCalled = false;

        String updateJsonData = createValidJsonData("NEW001", "Updated Station");
        HttpRequest updateRequest = createHttpRequest("PUT", updateJsonData.length(), 7L);
        BufferedReader updateReader = createBufferedReader(updateJsonData);

        putHandler.handle(updateRequest, updateReader, printWriter, 7L);

        response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 for existing station update");
        assertTrue(response.contains("Lamport-Time: 7"), "Should include Lamport time in response");
        assertTrue(dataChangedCallbackCalled, "Data changed callback should be called");
        
        // Verify data was updated
        allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should still have one station");
        assertEquals("Updated Station", allData[0].getName(), "Should have updated name");
        
        System.out.println("✓ Valid weather data test passed");
    }

    @Test
    // Tests PUT request handling with no content scenarios including zero and negative content length
    void testHandleNoContentScenarios() throws Exception {
        System.out.println("Testing PUT request with no content scenarios...");
        
        // Test zero content length
        HttpRequest request1 = createHttpRequest("PUT", 0, 2L);
        BufferedReader reader1 = createBufferedReader("");
        putHandler.handle(request1, reader1, printWriter, 2L);
        String response1 = getResponseContent();
        assertTrue(response1.contains("204 No Content"), "Should return 204 for zero content length");
        assertTrue(response1.contains("Lamport-Time: 2"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");

        // Reset for next test
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        dataChangedCallbackCalled = false;

        // Test negative content length
        HttpRequest request2 = createHttpRequest("PUT", -1, 3L);
        BufferedReader reader2 = createBufferedReader("");
        putHandler.handle(request2, reader2, printWriter, 3L);
        String response2 = getResponseContent();
        assertTrue(response2.contains("204 No Content"), "Should return 204 for negative content length");
        assertTrue(response2.contains("Lamport-Time: 3"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");

        // Reset for next test
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        dataChangedCallbackCalled = false;

        // Test whitespace-only content
        String whitespaceContent = "   \n\t  ";
        HttpRequest request3 = createHttpRequest("PUT", whitespaceContent.length(), 4L);
        BufferedReader reader3 = createBufferedReader(whitespaceContent);
        putHandler.handle(request3, reader3, printWriter, 4L);
        String response3 = getResponseContent();
        assertTrue(response3.contains("204 No Content"), "Should return 204 for whitespace-only content");
        assertTrue(response3.contains("Lamport-Time: 4"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");
        
        System.out.println("✓ No content scenarios test passed");
    }

    // === ERROR HANDLING TESTS ===

    @Test
    // Tests PUT request handling with invalid JSON data and error responses
    void testHandleInvalidJsonData() throws Exception {
        System.out.println("Testing PUT request with invalid JSON data...");
        
        // Test malformed JSON
        String malformedJson = "{\"id\":\"MALFORMED\",\"name\":\"Malformed Station\",\"state\":\"TEST\""; // Missing closing brace
        HttpRequest request1 = createHttpRequest("PUT", malformedJson.length(), 6L);
        BufferedReader reader1 = createBufferedReader(malformedJson);
        putHandler.handle(request1, reader1, printWriter, 6L);
        String response1 = getResponseContent();
        assertTrue(response1.contains("500 Internal Server Error"), "Should return 500 for malformed JSON");
        assertTrue(response1.contains("Lamport-Time: 6"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");

        // Reset for next test
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        dataChangedCallbackCalled = false;

        // Test invalid JSON syntax
        String invalidJson = "{\"id\":\"INVALID\",\"name\":\"Invalid Station\",\"invalid\":}";
        HttpRequest request2 = createHttpRequest("PUT", invalidJson.length(), 7L);
        BufferedReader reader2 = createBufferedReader(invalidJson);
        putHandler.handle(request2, reader2, printWriter, 7L);
        String response2 = getResponseContent();
        assertTrue(response2.contains("500 Internal Server Error"), "Should return 500 for invalid JSON syntax");
        assertTrue(response2.contains("Lamport-Time: 7"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");

        // Reset for next test
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        dataChangedCallbackCalled = false;

        // Test null data
        String nullDataJson = "null";
        HttpRequest request3 = createHttpRequest("PUT", nullDataJson.length(), 8L);
        BufferedReader reader3 = createBufferedReader(nullDataJson);
        putHandler.handle(request3, reader3, printWriter, 8L);
        String response3 = getResponseContent();
        assertTrue(response3.contains("500 Internal Server Error"), "Should return 500 for null data");
        assertTrue(response3.contains("Lamport-Time: 8"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");
        
        System.out.println("✓ Invalid JSON data test passed");
    }

    @Test
    // Tests PUT request handling with validation errors including missing required fields
    void testHandleValidationErrors() throws Exception {
        System.out.println("Testing PUT request with validation errors...");
        
        // Test missing required fields
        String incompleteJson = "{\"name\":\"Incomplete Station\"}"; // Missing required 'id' field
        HttpRequest request1 = createHttpRequest("PUT", incompleteJson.length(), 9L);
        BufferedReader reader1 = createBufferedReader(incompleteJson);
        putHandler.handle(request1, reader1, printWriter, 9L);
        String response1 = getResponseContent();
        assertTrue(response1.contains("500 Internal Server Error"), "Should return 500 for missing required fields");
        assertTrue(response1.contains("Lamport-Time: 9"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");

        // Reset for next test
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        dataChangedCallbackCalled = false;

        // Test empty ID field
        String emptyIdJson = "{\"id\":\"\",\"name\":\"Empty ID Station\"}";
        HttpRequest request2 = createHttpRequest("PUT", emptyIdJson.length(), 10L);
        BufferedReader reader2 = createBufferedReader(emptyIdJson);
        putHandler.handle(request2, reader2, printWriter, 10L);
        String response2 = getResponseContent();
        assertTrue(response2.contains("500 Internal Server Error"), "Should return 500 for empty ID");
        assertTrue(response2.contains("Lamport-Time: 10"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");

        // Reset for next test
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        dataChangedCallbackCalled = false;

        // Test empty name field
        String emptyNameJson = "{\"id\":\"EMPTY_NAME\",\"name\":\"\"}";
        HttpRequest request3 = createHttpRequest("PUT", emptyNameJson.length(), 11L);
        BufferedReader reader3 = createBufferedReader(emptyNameJson);
        putHandler.handle(request3, reader3, printWriter, 11L);
        String response3 = getResponseContent();
        assertTrue(response3.contains("500 Internal Server Error"), "Should return 500 for empty name");
        assertTrue(response3.contains("Lamport-Time: 11"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");
        
        System.out.println("✓ Validation errors test passed");
    }

    @Test
    // Tests PUT request handling with IO errors and incomplete data
    void testHandleIOErrors() throws Exception {
        System.out.println("Testing PUT request with IO errors...");
        
        // Test IO exception during read
        HttpRequest request1 = createHttpRequest("PUT", 100, 12L);
        BufferedReader faultyReader = new BufferedReader(new StringReader("")) {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("Simulated IO error");
            }
        };
        putHandler.handle(request1, faultyReader, printWriter, 12L);
        String response1 = getResponseContent();
        assertTrue(response1.contains("400 Bad Request"), "Should return 400 for IO error");
        assertTrue(response1.contains("Lamport-Time: 12"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");

        // Reset for next test
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        dataChangedCallbackCalled = false;

        // Test unexpected end of stream
        HttpRequest request2 = createHttpRequest("PUT", 100, 13L);
        BufferedReader reader2 = createBufferedReader("incomplete");
        putHandler.handle(request2, reader2, printWriter, 13L);
        String response2 = getResponseContent();
        assertTrue(response2.contains("400 Bad Request"), "Should return 400 for incomplete data");
        assertTrue(response2.contains("Lamport-Time: 13"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");
        
        System.out.println("✓ IO errors test passed");
    }

    // === EDGE CASES TESTS ===

    @Test
    // Tests PUT request handling with special data scenarios including special characters and large data
    void testHandleSpecialDataScenarios() throws Exception {
        System.out.println("Testing PUT request with special data scenarios...");
        
        // Test special characters
        String specialJson = "{\"id\":\"SPECIAL001\",\"name\":\"Station with special chars: ñáéíóú & symbols\",\"state\":\"TEST\",\"time_zone\":\"UTC\",\"lat\":-35.0,\"lon\":138.0,\"local_date_time\":\"15/04:00pm\",\"local_date_time_full\":\"20230715160000\",\"air_temp\":20.0,\"apparent_t\":18.5,\"cloud\":\"Partly \\\"cloudy\\\" with mixed conditions\",\"dewpt\":10.0,\"press\":1013.0,\"rel_hum\":60,\"wind_dir\":\"N\",\"wind_spd_kmh\":10,\"wind_spd_kt\":5}";
        HttpRequest request1 = createHttpRequest("PUT", specialJson.length(), 14L);
        BufferedReader reader1 = createBufferedReader(specialJson);
        putHandler.handle(request1, reader1, printWriter, 14L);
        String response1 = getResponseContent();
        assertTrue(response1.contains("201 Created"), "Should handle special characters successfully");
        assertTrue(response1.contains("Lamport-Time: 14"), "Should include Lamport time in response");
        assertTrue(dataChangedCallbackCalled, "Data changed callback should be called");
        
        // Verify special characters were preserved
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should have one station stored");
        assertTrue(allData[0].getName().contains("ñáéíóú"), "Should preserve special characters");
        assertTrue(allData[0].getCloud().contains("\"cloudy\""), "Should preserve quotes in cloud description");

        // Reset for next test
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        dataChangedCallbackCalled = false;

        // Test large JSON data
        StringBuilder largeJson = new StringBuilder();
        largeJson.append("{\"id\":\"LARGE001\",\"name\":\"Large Station\",\"state\":\"TEST\",");
        largeJson.append("\"time_zone\":\"UTC\",\"lat\":-35.0,\"lon\":138.0,");
        largeJson.append("\"local_date_time\":\"15/04:00pm\",\"local_date_time_full\":\"20230715160000\",");
        largeJson.append("\"air_temp\":20.0,\"apparent_t\":18.5,\"cloud\":\"Clear\",");
        largeJson.append("\"dewpt\":10.0,\"press\":1013.0,\"rel_hum\":60,");
        largeJson.append("\"wind_dir\":\"N\",\"wind_spd_kmh\":10,\"wind_spd_kt\":5,");
        largeJson.append("\"extra_field\":\"");
        
        // Add a large string to make the JSON substantial
        for (int i = 0; i < 1000; i++) {
            largeJson.append("This is additional data to make the JSON larger. ");
        }
        largeJson.append("\"}");

        String jsonData = largeJson.toString();
        HttpRequest request2 = createHttpRequest("PUT", jsonData.length(), 15L);
        BufferedReader reader2 = createBufferedReader(jsonData);
        putHandler.handle(request2, reader2, printWriter, 15L);
        String response2 = getResponseContent();
        assertTrue(response2.contains("201 Created"), "Should handle large JSON data successfully");
        assertTrue(response2.contains("Lamport-Time: 15"), "Should include Lamport time in response");
        assertTrue(dataChangedCallbackCalled, "Data changed callback should be called");

        // Reset for next test
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        dataChangedCallbackCalled = false;

        // Test JSON with extra fields
        String extraFieldsJson = "{\"id\":\"EXTRA001\",\"name\":\"Extra Fields Station\",\"state\":\"TEST\",\"time_zone\":\"UTC\",\"lat\":-35.0,\"lon\":138.0,\"local_date_time\":\"15/04:00pm\",\"local_date_time_full\":\"20230715160000\",\"air_temp\":20.0,\"apparent_t\":18.5,\"cloud\":\"Clear\",\"dewpt\":10.0,\"press\":1013.0,\"rel_hum\":60,\"wind_dir\":\"N\",\"wind_spd_kmh\":10,\"wind_spd_kt\":5,\"extra_field\":\"extra_value\",\"another_field\":123}";
        HttpRequest request3 = createHttpRequest("PUT", extraFieldsJson.length(), 16L);
        BufferedReader reader3 = createBufferedReader(extraFieldsJson);
        putHandler.handle(request3, reader3, printWriter, 16L);
        String response3 = getResponseContent();
        assertTrue(response3.contains("201 Created"), "Should handle extra fields gracefully");
        assertTrue(response3.contains("Lamport-Time: 16"), "Should include Lamport time in response");
        assertTrue(dataChangedCallbackCalled, "Data changed callback should be called");
        
        // Verify data was stored correctly despite extra fields
        allData = weatherDataService.getAllWeatherData();
        assertEquals(3, allData.length, "Should have three stations stored");
        assertEquals("EXTRA001", allData[2].getId(), "Should have correct station ID");
        assertEquals("Extra Fields Station", allData[2].getName(), "Should have correct station name");
        
        System.out.println("✓ Special data scenarios test passed");
    }

    @Test
    // Tests PUT request handling with multiple weather stations
    void testHandleMultipleStations() throws Exception {
        System.out.println("Testing PUT request with multiple stations...");
        
        // Add first station
        String jsonData1 = createValidJsonData("MULTI001", "First Station");
        HttpRequest request1 = createHttpRequest("PUT", jsonData1.length(), 17L);
        BufferedReader reader1 = createBufferedReader(jsonData1);
        putHandler.handle(request1, reader1, printWriter, 17L);
        
        // Reset for second request
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        dataChangedCallbackCalled = false;

        // Add second station
        String jsonData2 = createValidJsonData("MULTI002", "Second Station");
        HttpRequest request2 = createHttpRequest("PUT", jsonData2.length(), 18L);
        BufferedReader reader2 = createBufferedReader(jsonData2);
        putHandler.handle(request2, reader2, printWriter, 18L);

        String response = getResponseContent();
        assertTrue(response.contains("201 Created"), "Should return 201 for second station");
        assertTrue(dataChangedCallbackCalled, "Data changed callback should be called");
        
        // Verify both stations are stored
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(2, allData.length, "Should have two stations stored");
        
        System.out.println("Multiple stations test passed");
    }
}
