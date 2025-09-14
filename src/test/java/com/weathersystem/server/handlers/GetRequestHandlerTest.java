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

    @Test
    void testHandleGetWithNoData() throws Exception {
        HttpRequest request = createHttpRequest("GET", 0, 5L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 5L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 5"), "Should include Lamport time in response");
        assertTrue(response.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response.contains("[]"), "Should return empty JSON array for no data");
    }

    @Test
    void testHandleGetWithSingleStation() throws Exception {
        // Add a station to the service
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
    }

    @Test
    void testHandleGetWithMultipleStations() throws Exception {
        // Add multiple stations to the service
        WeatherData station1 = createSampleWeatherData("MULTI001", "First Station");
        WeatherData station2 = createSampleWeatherData("MULTI002", "Second Station");
        WeatherData station3 = createSampleWeatherData("MULTI003", "Third Station");
        
        weatherDataService.storeWeatherData(station1);
        weatherDataService.storeWeatherData(station2);
        weatherDataService.storeWeatherData(station3);

        HttpRequest request = createHttpRequest("GET", 0, 9L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 9L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 9"), "Should include Lamport time in response");
        assertTrue(response.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response.contains("MULTI001"), "Should include first station ID");
        assertTrue(response.contains("MULTI002"), "Should include second station ID");
        assertTrue(response.contains("MULTI003"), "Should include third station ID");
        assertTrue(response.contains("First Station"), "Should include first station name");
        assertTrue(response.contains("Second Station"), "Should include second station name");
        assertTrue(response.contains("Third Station"), "Should include third station name");
    }

    @Test
    void testHandleGetWithSpecialCharacters() throws Exception {
        // Add a station with special characters
        WeatherData specialStation = createSampleWeatherData("SPECIAL001", "Station with special chars: ñáéíóú");
        specialStation.setCloud("Partly \"cloudy\" with mixed conditions");
        specialStation.setWindDir("SW");
        weatherDataService.storeWeatherData(specialStation);

        HttpRequest request = createHttpRequest("GET", 0, 11L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 11L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 11"), "Should include Lamport time in response");
        assertTrue(response.contains("SPECIAL001"), "Should include station ID");
        assertTrue(response.contains("ñáéíóú"), "Should preserve special characters in name");
        assertTrue(response.contains("cloudy"), "Should preserve cloud description");
    }

    @Test
    void testHandleGetWithNumericPrecision() throws Exception {
        // Add a station with precise numeric values
        WeatherData precisionStation = createSampleWeatherData("PRECISION001", "Precision Station");
        precisionStation.setAirTemp(13.123456789);
        precisionStation.setLat(-34.987654321);
        precisionStation.setLon(138.123456789);
        precisionStation.setPress(1023.987654321);
        weatherDataService.storeWeatherData(precisionStation);

        HttpRequest request = createHttpRequest("GET", 0, 13L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 13L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 13"), "Should include Lamport time in response");
        assertTrue(response.contains("PRECISION001"), "Should include station ID");
        assertTrue(response.contains("13.123456789"), "Should preserve temperature precision");
        assertTrue(response.contains("-34.987654321"), "Should preserve latitude precision");
        assertTrue(response.contains("138.123456789"), "Should preserve longitude precision");
        assertTrue(response.contains("1023.987654321"), "Should preserve pressure precision");
    }

    @Test
    void testHandleGetWithEmptyFields() throws Exception {
        // Add a station with some empty fields
        WeatherData emptyFieldsStation = createSampleWeatherData("EMPTY001", "Empty Fields Station");
        emptyFieldsStation.setCloud(""); // Empty cloud field
        emptyFieldsStation.setWindDir(""); // Empty wind direction
        weatherDataService.storeWeatherData(emptyFieldsStation);

        HttpRequest request = createHttpRequest("GET", 0, 15L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 15L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 15"), "Should include Lamport time in response");
        assertTrue(response.contains("EMPTY001"), "Should include station ID");
        assertTrue(response.contains("Empty Fields Station"), "Should include station name");
    }

    @Test
    void testHandleGetWithZeroValues() throws Exception {
        // Add a station with zero values
        WeatherData zeroValuesStation = createSampleWeatherData("ZERO001", "Zero Values Station");
        zeroValuesStation.setAirTemp(0.0);
        zeroValuesStation.setLat(0.0);
        zeroValuesStation.setLon(0.0);
        zeroValuesStation.setPress(0.0);
        zeroValuesStation.setRelHum(0);
        zeroValuesStation.setWindSpdKmh(0);
        zeroValuesStation.setWindSpdKt(0);
        weatherDataService.storeWeatherData(zeroValuesStation);

        HttpRequest request = createHttpRequest("GET", 0, 17L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 17L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 17"), "Should include Lamport time in response");
        assertTrue(response.contains("ZERO001"), "Should include station ID");
        assertTrue(response.contains("Zero Values Station"), "Should include station name");
    }

    @Test
    void testHandleGetWithNegativeValues() throws Exception {
        // Add a station with negative values
        WeatherData negativeValuesStation = createSampleWeatherData("NEGATIVE001", "Negative Values Station");
        negativeValuesStation.setAirTemp(-5.5);
        negativeValuesStation.setLat(-90.0);
        negativeValuesStation.setLon(-180.0);
        negativeValuesStation.setPress(-10.0);
        weatherDataService.storeWeatherData(negativeValuesStation);

        HttpRequest request = createHttpRequest("GET", 0, 19L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 19L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 19"), "Should include Lamport time in response");
        assertTrue(response.contains("NEGATIVE001"), "Should include station ID");
        assertTrue(response.contains("Negative Values Station"), "Should include station name");
        assertTrue(response.contains("-5.5"), "Should preserve negative temperature");
        assertTrue(response.contains("-90.0"), "Should preserve negative latitude");
        assertTrue(response.contains("-180.0"), "Should preserve negative longitude");
    }

    @Test
    void testHandleGetWithLargeValues() throws Exception {
        // Add a station with large values
        WeatherData largeValuesStation = createSampleWeatherData("LARGE001", "Large Values Station");
        largeValuesStation.setAirTemp(999.999);
        largeValuesStation.setLat(90.0);
        largeValuesStation.setLon(180.0);
        largeValuesStation.setPress(9999.999);
        largeValuesStation.setRelHum(100);
        largeValuesStation.setWindSpdKmh(999);
        largeValuesStation.setWindSpdKt(999);
        weatherDataService.storeWeatherData(largeValuesStation);

        HttpRequest request = createHttpRequest("GET", 0, 21L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 21L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 21"), "Should include Lamport time in response");
        assertTrue(response.contains("LARGE001"), "Should include station ID");
        assertTrue(response.contains("Large Values Station"), "Should include station name");
        assertTrue(response.contains("999.999"), "Should preserve large temperature");
        assertTrue(response.contains("90.0"), "Should preserve large latitude");
        assertTrue(response.contains("180.0"), "Should preserve large longitude");
        assertTrue(response.contains("9999.999"), "Should preserve large pressure");
    }

    @Test
    void testHandleGetWithMixedDataTypes() throws Exception {
        // Add stations with different data characteristics
        WeatherData station1 = createSampleWeatherData("MIXED001", "Station One");
        station1.setAirTemp(15.5);
        station1.setRelHum(75);
        station1.setWindSpdKmh(25);
        
        WeatherData station2 = createSampleWeatherData("MIXED002", "Station Two");
        station2.setAirTemp(-2.3);
        station2.setRelHum(30);
        station2.setWindSpdKmh(5);
        
        weatherDataService.storeWeatherData(station1);
        weatherDataService.storeWeatherData(station2);

        HttpRequest request = createHttpRequest("GET", 0, 23L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 23L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 23"), "Should include Lamport time in response");
        assertTrue(response.contains("MIXED001"), "Should include first station ID");
        assertTrue(response.contains("MIXED002"), "Should include second station ID");
        assertTrue(response.contains("Station One"), "Should include first station name");
        assertTrue(response.contains("Station Two"), "Should include second station name");
        assertTrue(response.contains("15.5"), "Should include first station temperature");
        assertTrue(response.contains("-2.3"), "Should include second station temperature");
    }

    @Test
    void testHandleGetAfterDataUpdate() throws Exception {
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
    }

    @Test
    void testHandleGetWithConcurrentAccess() throws Exception {
        // Add multiple stations concurrently (simulated by sequential adds)
        for (int i = 0; i < 10; i++) {
            WeatherData station = createSampleWeatherData("CONCURRENT" + i, "Concurrent Station " + i);
            station.setAirTemp(i * 1.5);
            weatherDataService.storeWeatherData(station);
        }

        HttpRequest request = createHttpRequest("GET", 0, 27L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 27L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 27"), "Should include Lamport time in response");
        
        // Verify all stations are included
        for (int i = 0; i < 10; i++) {
            assertTrue(response.contains("CONCURRENT" + i), "Should include station " + i);
            assertTrue(response.contains("Concurrent Station " + i), "Should include station name " + i);
        }
    }

    @Test
    void testHandleGetWithEmptyService() throws Exception {
        // Ensure service is empty
        weatherDataService.clearAllData();

        HttpRequest request = createHttpRequest("GET", 0, 29L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 29L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 29"), "Should include Lamport time in response");
        assertTrue(response.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response.contains("[]"), "Should return empty JSON array");
    }

    @Test
    void testHandleGetWithVeryLongStationName() throws Exception {
        // Add a station with a very long name
        StringBuilder longName = new StringBuilder("Very Long Station Name");
        for (int i = 0; i < 100; i++) {
            longName.append(" with additional text");
        }
        
        WeatherData longNameStation = createSampleWeatherData("LONGNAME001", longName.toString());
        weatherDataService.storeWeatherData(longNameStation);

        HttpRequest request = createHttpRequest("GET", 0, 31L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 31L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 31"), "Should include Lamport time in response");
        assertTrue(response.contains("LONGNAME001"), "Should include station ID");
        assertTrue(response.contains("Very Long Station Name"), "Should include long station name");
    }

    @Test
    void testHandleGetWithUnicodeCharacters() throws Exception {
        // Add a station with Unicode characters
        WeatherData unicodeStation = createSampleWeatherData("UNICODE001", "Station with Unicode: 中文 日本語 한국어 العربية");
        unicodeStation.setCloud("Partly cloudy with 中文 characters");
        unicodeStation.setWindDir("北"); // North in Chinese
        weatherDataService.storeWeatherData(unicodeStation);

        HttpRequest request = createHttpRequest("GET", 0, 33L);
        BufferedReader reader = createBufferedReader("");

        getHandler.handle(request, reader, printWriter, 33L);

        String response = getResponseContent();
        assertTrue(response.contains("200 OK"), "Should return 200 OK");
        assertTrue(response.contains("Lamport-Time: 33"), "Should include Lamport time in response");
        assertTrue(response.contains("UNICODE001"), "Should include station ID");
        assertTrue(response.contains("中文"), "Should preserve Chinese characters");
        assertTrue(response.contains("日本語"), "Should preserve Japanese characters");
        assertTrue(response.contains("한국어"), "Should preserve Korean characters");
        assertTrue(response.contains("العربية"), "Should preserve Arabic characters");
    }

    @Test
    void testHandleGetResponseFormat() throws Exception {
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
    }
}
