package com.weathersystem.shared.json;

import com.weathersystem.shared.domain.WeatherData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class JSONUtilsTest {

    private WeatherData sampleWeatherData;
    private WeatherData[] sampleWeatherArray;

    @BeforeEach
    void setUp() {
        sampleWeatherData = createSampleWeatherData();
        sampleWeatherArray = new WeatherData[]{
                createSampleWeatherData(),
                createSampleWeatherData2()
        };
    }

    // === CORE FUNCTIONALITY TESTS ===

    private WeatherData createSampleWeatherData() {
        WeatherData data = new WeatherData();
        data.setId("IDS60901");
        data.setName("Adelaide (West Terrace /  ngayirdapira)");
        data.setState("SA");
        data.setTimeZone("CST");
        data.setLat(-34.9);
        data.setLon(138.6);
        data.setLocalDateTime("15/04:00pm");
        data.setLocalDateTimeFull("20230715160000");
        data.setAirTemp(13.3);
        data.setApparentT(9.5);
        data.setCloud("Partly cloudy");
        data.setDewpt(4.7);
        data.setPress(1023.9);
        data.setRelHum(58);
        data.setWindDir("S");
        data.setWindSpdKmh(15);
        data.setWindSpdKt(8);
        return data;
    }

    private WeatherData createSampleWeatherData2() {
        WeatherData data = new WeatherData();
        data.setId("IDS60902");
        data.setName("Melbourne (Olympic Park)");
        data.setState("VIC");
        data.setTimeZone("EST");
        data.setLat(-37.8);
        data.setLon(145.0);
        data.setLocalDateTime("15/04:30pm");
        data.setLocalDateTimeFull("20230715163000");
        data.setAirTemp(16.2);
        data.setApparentT(14.8);
        data.setCloud("Clear");
        data.setDewpt(8.1);
        data.setPress(1018.7);
        data.setRelHum(65);
        data.setWindDir("NW");
        data.setWindSpdKmh(12);
        data.setWindSpdKt(6);
        return data;
    }

    @Test
    // Tests converting weather data to JSON and back again
    void testBasicSerializationAndDeserialization() {
        System.out.println("Testing basic serialization and deserialization...");
        
        // Test single object serialization
        String json = JSONUtils.toJSON(sampleWeatherData);
        assertNotNull(json, "JSON should not be null");
        assertFalse(json.isEmpty(), "JSON should not be empty");
        assertTrue(json.contains("\"id\""), "JSON should contain id field");
        assertTrue(json.contains("\"IDS60901\""), "JSON should contain correct id value");
        assertTrue(json.contains("\"name\""), "JSON should contain name field");
        assertTrue(json.contains("Adelaide"), "JSON should contain correct name");
        assertTrue(json.contains("\"airTemp\""), "JSON should contain airTemp field");
        assertTrue(json.contains("13.3"), "JSON should contain correct temperature");

        // Test single object deserialization
        WeatherData parsed = JSONUtils.fromJSON(json);
        assertNotNull(parsed, "Parsed weather data should not be null");
        assertEquals(sampleWeatherData.getId(), parsed.getId(), "ID should match");
        assertEquals(sampleWeatherData.getName(), parsed.getName(), "Name should match");
        assertEquals(sampleWeatherData.getState(), parsed.getState(), "State should match");
        assertEquals(sampleWeatherData.getTimeZone(), parsed.getTimeZone(), "TimeZone should match");
        assertEquals(sampleWeatherData.getLat(), parsed.getLat(), 0.001, "Latitude should match");
        assertEquals(sampleWeatherData.getLon(), parsed.getLon(), 0.001, "Longitude should match");
        assertEquals(sampleWeatherData.getLocalDateTime(), parsed.getLocalDateTime(), "LocalDateTime should match");
        assertEquals(sampleWeatherData.getLocalDateTimeFull(), parsed.getLocalDateTimeFull(), "LocalDateTimeFull should match");
        assertEquals(sampleWeatherData.getAirTemp(), parsed.getAirTemp(), 0.001, "Air temperature should match");
        assertEquals(sampleWeatherData.getApparentT(), parsed.getApparentT(), 0.001, "Apparent temperature should match");
        assertEquals(sampleWeatherData.getCloud(), parsed.getCloud(), "Cloud description should match");
        assertEquals(sampleWeatherData.getDewpt(), parsed.getDewpt(), 0.001, "Dew point should match");
        assertEquals(sampleWeatherData.getPress(), parsed.getPress(), 0.001, "Pressure should match");
        assertEquals(sampleWeatherData.getRelHum(), parsed.getRelHum(), "Relative humidity should match");
        assertEquals(sampleWeatherData.getWindDir(), parsed.getWindDir(), "Wind direction should match");
        assertEquals(sampleWeatherData.getWindSpdKmh(), parsed.getWindSpdKmh(), "Wind speed kmh should match");
        assertEquals(sampleWeatherData.getWindSpdKt(), parsed.getWindSpdKt(), "Wind speed kt should match");

        // Test array serialization
        String arrayJson = JSONUtils.toJSON(sampleWeatherArray);
        assertNotNull(arrayJson, "JSON array should not be null");
        assertFalse(arrayJson.isEmpty(), "JSON array should not be empty");
        assertTrue(arrayJson.startsWith("["), "JSON array should start with [");
        assertTrue(arrayJson.endsWith("]"), "JSON array should end with ]");
        assertTrue(arrayJson.contains("IDS60901"), "JSON array should contain first station");
        assertTrue(arrayJson.contains("IDS60902"), "JSON array should contain second station");
        assertTrue(arrayJson.contains("Adelaide"), "JSON array should contain Adelaide");
        assertTrue(arrayJson.contains("Melbourne"), "JSON array should contain Melbourne");

        // Test array deserialization
        WeatherData[] parsedArray = JSONUtils.fromJSONArray(arrayJson);
        assertNotNull(parsedArray, "Parsed array should not be null");
        assertEquals(2, parsedArray.length, "Array should have 2 elements");

        // Check first element
        assertEquals(sampleWeatherArray[0].getId(), parsedArray[0].getId(), "First element ID should match");
        assertEquals(sampleWeatherArray[0].getName(), parsedArray[0].getName(), "First element name should match");
        assertEquals(sampleWeatherArray[0].getAirTemp(), parsedArray[0].getAirTemp(), 0.001, "First element temperature should match");

        // Check second element
        assertEquals(sampleWeatherArray[1].getId(), parsedArray[1].getId(), "Second element ID should match");
        assertEquals(sampleWeatherArray[1].getName(), parsedArray[1].getName(), "Second element name should match");
        assertEquals(sampleWeatherArray[1].getAirTemp(), parsedArray[1].getAirTemp(), 0.001, "Second element temperature should match");

        // Test round trip consistency
        String jsonAgain = JSONUtils.toJSON(parsed);
        WeatherData parsedAgain = JSONUtils.fromJSON(jsonAgain);
        assertEquals(sampleWeatherData.getId(), parsedAgain.getId(), "Round trip should preserve ID");
        assertEquals(sampleWeatherData.getAirTemp(), parsedAgain.getAirTemp(), 0.001, "Round trip should preserve temperature");

        String arrayJsonAgain = JSONUtils.toJSON(parsedArray);
        WeatherData[] parsedArrayAgain = JSONUtils.fromJSONArray(arrayJsonAgain);
        assertEquals(sampleWeatherArray.length, parsedArrayAgain.length, "Array length should be preserved");
        
        System.out.println("✓ Basic serialization and deserialization test passed");
    }

    // === ERROR HANDLING TESTS ===

    @Test
    // Tests what happens when we get dodgy data or null values
    void testErrorHandlingAndEdgeCases() {
        System.out.println("Testing error handling and edge cases...");
        
        // Test null handling
        String nullJson = JSONUtils.toJSON((WeatherData) null);
        assertEquals("null", nullJson, "Null WeatherData should serialize to 'null'");

        String nullArrayJson = JSONUtils.toJSON((WeatherData[]) null);
        assertEquals("null", nullArrayJson, "Null WeatherData array should serialize to 'null'");

        WeatherData nullResult = JSONUtils.fromJSON(null);
        assertNull(nullResult, "Parsing null string should return null");

        WeatherData[] nullArrayResult = JSONUtils.fromJSONArray(null);
        assertNull(nullArrayResult, "Parsing null string should return null");

        // Test empty array
        WeatherData[] emptyArray = new WeatherData[0];
        String emptyJson = JSONUtils.toJSON(emptyArray);
        assertNotNull(emptyJson, "Empty array JSON should not be null");
        assertEquals("[]", emptyJson.replaceAll("\\s", ""), "Empty array should serialize to []");

        WeatherData[] parsedEmpty = JSONUtils.fromJSONArray(emptyJson);
        assertNotNull(parsedEmpty, "Parsed empty array should not be null");
        assertEquals(0, parsedEmpty.length, "Parsed array should be empty");

        // Test empty string handling
        assertDoesNotThrow(() -> {
            WeatherData result = JSONUtils.fromJSON("");
            // Result may be null or empty object depending on Gson behavior
        }, "Should handle empty string gracefully");

        // Test invalid JSON
        assertThrows(Exception.class, () -> {
            JSONUtils.fromJSON("invalid json");
        }, "Parsing invalid JSON should throw exception");

        // Test malformed JSON with missing fields
        String malformedJson = "{\"id\": \"test\", \"invalidField\": true}";
        assertDoesNotThrow(() -> {
            WeatherData result = JSONUtils.fromJSON(malformedJson);
            assertNotNull(result, "Should create object despite missing fields");
            assertEquals("test", result.getId(), "Should parse available fields");
        }, "Should handle malformed JSON gracefully");
        
        System.out.println("✓ Error handling and edge cases test passed");
    }

    // === DATA INTEGRITY AND FORMATTING TESTS ===

    @Test
    // Tests that special characters and precise numbers work properly
    void testDataIntegrityAndFormatting() {
        System.out.println("Testing data integrity and formatting...");
        
        // Test JSON formatting
        String json = JSONUtils.toJSON(sampleWeatherData);
        assertTrue(json.contains("\n"), "JSON should be pretty printed with newlines");
        assertTrue(json.contains("  "), "JSON should contain indentation");

        // Test special characters
        WeatherData specialData = new WeatherData();
        specialData.setId("Test\"With'Quotes");
        specialData.setName("Station with unicode: café, naïve");
        specialData.setCloud("Partly \"cloudy\" with 'mixed' conditions");

        String specialJson = JSONUtils.toJSON(specialData);
        WeatherData parsedSpecial = JSONUtils.fromJSON(specialJson);

        assertEquals(specialData.getId(), parsedSpecial.getId(), "Should handle quotes in ID");
        assertEquals(specialData.getName(), parsedSpecial.getName(), "Should handle unicode characters");
        assertEquals(specialData.getCloud(), parsedSpecial.getCloud(), "Should handle quotes in description");

        // Test numeric precision
        WeatherData precisionData = new WeatherData();
        precisionData.setAirTemp(13.12345678);
        precisionData.setLat(-34.123456789);
        precisionData.setPress(1023.987654321);

        String precisionJson = JSONUtils.toJSON(precisionData);
        WeatherData parsedPrecision = JSONUtils.fromJSON(precisionJson);

        assertEquals(precisionData.getAirTemp(), parsedPrecision.getAirTemp(), 0.000001, "Should preserve temperature precision");
        assertEquals(precisionData.getLat(), parsedPrecision.getLat(), 0.000001, "Should preserve latitude precision");
        assertEquals(precisionData.getPress(), parsedPrecision.getPress(), 0.000001, "Should preserve pressure precision");

        // Test extreme numeric values
        WeatherData extremeData = new WeatherData();
        extremeData.setAirTemp(Double.MAX_VALUE);
        extremeData.setLat(-90.0);
        extremeData.setLon(180.0);
        extremeData.setPress(0.0);
        extremeData.setRelHum(100);
        extremeData.setWindSpdKmh(Integer.MAX_VALUE);

        String extremeJson = JSONUtils.toJSON(extremeData);
        WeatherData parsedExtreme = JSONUtils.fromJSON(extremeJson);

        assertEquals(extremeData.getAirTemp(), parsedExtreme.getAirTemp(), "Should handle extreme temperature");
        assertEquals(extremeData.getLat(), parsedExtreme.getLat(), "Should handle minimum latitude");
        assertEquals(extremeData.getLon(), parsedExtreme.getLon(), "Should handle maximum longitude");
        assertEquals(extremeData.getPress(), parsedExtreme.getPress(), "Should handle zero pressure");
        assertEquals(extremeData.getRelHum(), parsedExtreme.getRelHum(), "Should handle maximum humidity");
        assertEquals(extremeData.getWindSpdKmh(), parsedExtreme.getWindSpdKmh(), "Should handle extreme wind speed");

        // Test large array serialization
        final int LARGE_SIZE = 100; // Reduced for faster execution
        WeatherData[] largeArray = new WeatherData[LARGE_SIZE];

        for (int i = 0; i < LARGE_SIZE; i++) {
            WeatherData data = new WeatherData();
            data.setId("ID" + i);
            data.setName("Station " + i);
            data.setAirTemp(i * 0.1);
            largeArray[i] = data;
        }

        String largeJson = JSONUtils.toJSON(largeArray);
        assertNotNull(largeJson, "Large array JSON should not be null");
        assertTrue(largeJson.contains("ID0"), "Should contain first element");
        assertTrue(largeJson.contains("ID" + (LARGE_SIZE - 1)), "Should contain last element");

        WeatherData[] parsedLarge = JSONUtils.fromJSONArray(largeJson);
        assertEquals(LARGE_SIZE, parsedLarge.length, "Parsed array should have correct size");
        assertEquals("ID0", parsedLarge[0].getId(), "First element should be correct");
        assertEquals("ID" + (LARGE_SIZE - 1), parsedLarge[LARGE_SIZE - 1].getId(), "Last element should be correct");
        
        System.out.println("✓ Data integrity and formatting test passed");
    }
}