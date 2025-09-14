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

    @Test
    void testHandleValidNewStation() throws Exception {
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
    }

    @Test
    void testHandleValidExistingStation() throws Exception {
        // First, add a station
        String jsonData1 = createValidJsonData("EXIST001", "Original Name");
        HttpRequest request1 = createHttpRequest("PUT", jsonData1.length(), 3L);
        BufferedReader reader1 = createBufferedReader(jsonData1);
        putHandler.handle(request1, reader1, printWriter, 3L);
        
        // Reset for second request
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        dataChangedCallbackCalled = false;

        // Update the same station
        String jsonData2 = createValidJsonData("EXIST001", "Updated Name");
        HttpRequest request2 = createHttpRequest("PUT", jsonData2.length(), 7L);
        BufferedReader reader2 = createBufferedReader(jsonData2);

        putHandler.handle(request2, reader2, printWriter, 7L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 for existing station update");
        assertTrue(response.contains("Lamport-Time: 7"), "Should include Lamport time in response");
        assertTrue(dataChangedCallbackCalled, "Data changed callback should be called");
        
        // Verify data was updated
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should still have one station");
        assertEquals("Updated Name", allData[0].getName(), "Should have updated name");
    }

    @Test
    void testHandleNoContent() throws Exception {
        HttpRequest request = createHttpRequest("PUT", 0, 2L);
        BufferedReader reader = createBufferedReader("");

        putHandler.handle(request, reader, printWriter, 2L);

        String response = getResponseContent();
        assertTrue(response.contains("204 No Content"), "Should return 204 for no content");
        assertTrue(response.contains("Lamport-Time: 2"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");
    }

    @Test
    void testHandleEmptyContent() throws Exception {
        HttpRequest request = createHttpRequest("PUT", 0, 3L); // Set content length to 0
        BufferedReader reader = createBufferedReader("");

        putHandler.handle(request, reader, printWriter, 3L);

        String response = getResponseContent();
        assertTrue(response.contains("204 No Content"), "Should return 204 for empty content");
        assertTrue(response.contains("Lamport-Time: 3"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");
    }

    @Test
    void testHandleInvalidJson() throws Exception {
        String invalidJson = "{\"id\":\"INVALID\",\"name\":\"Invalid Station\",\"invalid\":}";
        HttpRequest request = createHttpRequest("PUT", invalidJson.length(), 4L);
        BufferedReader reader = createBufferedReader(invalidJson);

        putHandler.handle(request, reader, printWriter, 4L);

        String response = getResponseContent();
        assertTrue(response.contains("500 Internal Server Error"), "Should return 500 for invalid JSON");
        assertTrue(response.contains("Lamport-Time: 4"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");
    }

    @Test
    void testHandleMissingRequiredFields() throws Exception {
        String incompleteJson = "{\"name\":\"Incomplete Station\"}"; // Missing required 'id' field
        HttpRequest request = createHttpRequest("PUT", incompleteJson.length(), 6L);
        BufferedReader reader = createBufferedReader(incompleteJson);

        putHandler.handle(request, reader, printWriter, 6L);

        String response = getResponseContent();
        assertTrue(response.contains("500 Internal Server Error"), "Should return 500 for missing required fields");
        assertTrue(response.contains("Lamport-Time: 6"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");
    }

    @Test
    void testHandleNullWeatherData() throws Exception {
        String nullDataJson = "null";
        HttpRequest request = createHttpRequest("PUT", nullDataJson.length(), 8L);
        BufferedReader reader = createBufferedReader(nullDataJson);

        putHandler.handle(request, reader, printWriter, 8L);

        String response = getResponseContent();
        assertTrue(response.contains("500 Internal Server Error"), "Should return 500 for null data");
        assertTrue(response.contains("Lamport-Time: 8"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");
    }

    @Test
    void testHandleEmptyIdField() throws Exception {
        String emptyIdJson = "{\"id\":\"\",\"name\":\"Empty ID Station\"}";
        HttpRequest request = createHttpRequest("PUT", emptyIdJson.length(), 9L);
        BufferedReader reader = createBufferedReader(emptyIdJson);

        putHandler.handle(request, reader, printWriter, 9L);

        String response = getResponseContent();
        assertTrue(response.contains("500 Internal Server Error"), "Should return 500 for empty ID");
        assertTrue(response.contains("Lamport-Time: 9"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");
    }

    @Test
    void testHandleEmptyNameField() throws Exception {
        String emptyNameJson = "{\"id\":\"EMPTY_NAME\",\"name\":\"\"}";
        HttpRequest request = createHttpRequest("PUT", emptyNameJson.length(), 10L);
        BufferedReader reader = createBufferedReader(emptyNameJson);

        putHandler.handle(request, reader, printWriter, 10L);

        String response = getResponseContent();
        assertTrue(response.contains("500 Internal Server Error"), "Should return 500 for empty name");
        assertTrue(response.contains("Lamport-Time: 10"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");
    }

    @Test
    void testHandleIOExceptionDuringRead() throws Exception {
        HttpRequest request = createHttpRequest("PUT", 100, 11L);
        
        BufferedReader faultyReader = new BufferedReader(new StringReader("")) {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("Simulated IO error");
            }
        };

        putHandler.handle(request, faultyReader, printWriter, 11L);

        String response = getResponseContent();
        assertTrue(response.contains("400 Bad Request"), "Should return 400 for IO error");
        assertTrue(response.contains("Lamport-Time: 11"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");
    }

    @Test
    void testHandleUnexpectedEndOfStream() throws Exception {
        HttpRequest request = createHttpRequest("PUT", 100, 12L);
        BufferedReader reader = createBufferedReader("incomplete");

        putHandler.handle(request, reader, printWriter, 12L);

        String response = getResponseContent();
        assertTrue(response.contains("400 Bad Request"), "Should return 400 for incomplete data");
        assertTrue(response.contains("Lamport-Time: 12"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");
    }

    @Test
    void testHandleLargeJsonData() throws Exception {
        // Create a large JSON payload
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
        HttpRequest request = createHttpRequest("PUT", jsonData.length(), 13L);
        BufferedReader reader = createBufferedReader(jsonData);

        putHandler.handle(request, reader, printWriter, 13L);

        String response = getResponseContent();
        assertTrue(response.contains("201 Created"), "Should handle large JSON data successfully");
        assertTrue(response.contains("Lamport-Time: 13"), "Should include Lamport time in response");
        assertTrue(dataChangedCallbackCalled, "Data changed callback should be called");
    }

    @Test
    void testHandleSpecialCharactersInJson() throws Exception {
        String specialJson = "{\"id\":\"SPECIAL001\",\"name\":\"Station with special chars: ñáéíóú & symbols\",\"state\":\"TEST\",\"time_zone\":\"UTC\",\"lat\":-35.0,\"lon\":138.0,\"local_date_time\":\"15/04:00pm\",\"local_date_time_full\":\"20230715160000\",\"air_temp\":20.0,\"apparent_t\":18.5,\"cloud\":\"Partly \\\"cloudy\\\" with mixed conditions\",\"dewpt\":10.0,\"press\":1013.0,\"rel_hum\":60,\"wind_dir\":\"N\",\"wind_spd_kmh\":10,\"wind_spd_kt\":5}";
        
        HttpRequest request = createHttpRequest("PUT", specialJson.length(), 14L);
        BufferedReader reader = createBufferedReader(specialJson);

        putHandler.handle(request, reader, printWriter, 14L);

        String response = getResponseContent();
        assertTrue(response.contains("201 Created"), "Should handle special characters successfully");
        assertTrue(response.contains("Lamport-Time: 14"), "Should include Lamport time in response");
        assertTrue(dataChangedCallbackCalled, "Data changed callback should be called");
        
        // Verify special characters were preserved
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should have one station stored");
        assertTrue(allData[0].getName().contains("ñáéíóú"), "Should preserve special characters");
        assertTrue(allData[0].getCloud().contains("\"cloudy\""), "Should preserve quotes in cloud description");
    }

    @Test
    void testHandleMultipleStationsSequentially() throws Exception {
        // Add first station
        String jsonData1 = createValidJsonData("MULTI001", "First Station");
        HttpRequest request1 = createHttpRequest("PUT", jsonData1.length(), 15L);
        BufferedReader reader1 = createBufferedReader(jsonData1);
        putHandler.handle(request1, reader1, printWriter, 15L);
        
        // Reset for second request
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        dataChangedCallbackCalled = false;

        // Add second station
        String jsonData2 = createValidJsonData("MULTI002", "Second Station");
        HttpRequest request2 = createHttpRequest("PUT", jsonData2.length(), 16L);
        BufferedReader reader2 = createBufferedReader(jsonData2);
        putHandler.handle(request2, reader2, printWriter, 16L);

        String response = getResponseContent();
        assertTrue(response.contains("201 Created"), "Should return 201 for second station");
        assertTrue(dataChangedCallbackCalled, "Data changed callback should be called");
        
        // Verify both stations are stored
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(2, allData.length, "Should have two stations stored");
    }

    @Test
    void testHandleZeroContentLength() throws Exception {
        HttpRequest request = createHttpRequest("PUT", 0, 17L);
        BufferedReader reader = createBufferedReader("");

        putHandler.handle(request, reader, printWriter, 17L);

        String response = getResponseContent();
        assertTrue(response.contains("204 No Content"), "Should return 204 for zero content length");
        assertTrue(response.contains("Lamport-Time: 17"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");
    }

    @Test
    void testHandleNegativeContentLength() throws Exception {
        HttpRequest request = createHttpRequest("PUT", -1, 18L);
        BufferedReader reader = createBufferedReader("");

        putHandler.handle(request, reader, printWriter, 18L);

        String response = getResponseContent();
        assertTrue(response.contains("204 No Content"), "Should return 204 for negative content length");
        assertTrue(response.contains("Lamport-Time: 18"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");
    }

    @Test
    void testHandleWhitespaceOnlyContent() throws Exception {
        String whitespaceContent = "   \n\t  ";
        HttpRequest request = createHttpRequest("PUT", whitespaceContent.length(), 19L);
        BufferedReader reader = createBufferedReader(whitespaceContent);

        putHandler.handle(request, reader, printWriter, 19L);

        String response = getResponseContent();
        assertTrue(response.contains("204 No Content"), "Should return 204 for whitespace-only content");
        assertTrue(response.contains("Lamport-Time: 19"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");
    }

    @Test
    void testHandleMalformedJsonStructure() throws Exception {
        String malformedJson = "{\"id\":\"MALFORMED\",\"name\":\"Malformed Station\",\"state\":\"TEST\""; // Missing closing brace
        HttpRequest request = createHttpRequest("PUT", malformedJson.length(), 20L);
        BufferedReader reader = createBufferedReader(malformedJson);

        putHandler.handle(request, reader, printWriter, 20L);

        String response = getResponseContent();
        assertTrue(response.contains("500 Internal Server Error"), "Should return 500 for malformed JSON");
        assertTrue(response.contains("Lamport-Time: 20"), "Should include Lamport time in response");
        assertFalse(dataChangedCallbackCalled, "Data changed callback should not be called");
    }

    @Test
    void testHandleJsonWithExtraFields() throws Exception {
        String extraFieldsJson = "{\"id\":\"EXTRA001\",\"name\":\"Extra Fields Station\",\"state\":\"TEST\",\"time_zone\":\"UTC\",\"lat\":-35.0,\"lon\":138.0,\"local_date_time\":\"15/04:00pm\",\"local_date_time_full\":\"20230715160000\",\"air_temp\":20.0,\"apparent_t\":18.5,\"cloud\":\"Clear\",\"dewpt\":10.0,\"press\":1013.0,\"rel_hum\":60,\"wind_dir\":\"N\",\"wind_spd_kmh\":10,\"wind_spd_kt\":5,\"extra_field\":\"extra_value\",\"another_field\":123}";
        
        HttpRequest request = createHttpRequest("PUT", extraFieldsJson.length(), 21L);
        BufferedReader reader = createBufferedReader(extraFieldsJson);

        putHandler.handle(request, reader, printWriter, 21L);

        String response = getResponseContent();
        assertTrue(response.contains("201 Created"), "Should handle extra fields gracefully");
        assertTrue(response.contains("Lamport-Time: 21"), "Should include Lamport time in response");
        assertTrue(dataChangedCallbackCalled, "Data changed callback should be called");
        
        // Verify data was stored correctly despite extra fields
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should have one station stored");
        assertEquals("EXTRA001", allData[0].getId(), "Should have correct station ID");
        assertEquals("Extra Fields Station", allData[0].getName(), "Should have correct station name");
    }
}
