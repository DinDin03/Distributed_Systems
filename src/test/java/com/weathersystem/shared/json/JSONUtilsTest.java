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
    void testToJSONSingleObject() {
        String json = JSONUtils.toJSON(sampleWeatherData);

        assertNotNull(json, "JSON should not be null");
        assertFalse(json.isEmpty(), "JSON should not be empty");
        assertTrue(json.contains("\"id\""), "JSON should contain id field");
        assertTrue(json.contains("\"IDS60901\""), "JSON should contain correct id value");
        assertTrue(json.contains("\"name\""), "JSON should contain name field");
        assertTrue(json.contains("Adelaide"), "JSON should contain correct name");
        assertTrue(json.contains("\"airTemp\""), "JSON should contain airTemp field");
        assertTrue(json.contains("13.3"), "JSON should contain correct temperature");
    }

    @Test
    void testToJSONArray() {
        String json = JSONUtils.toJSON(sampleWeatherArray);

        assertNotNull(json, "JSON array should not be null");
        assertFalse(json.isEmpty(), "JSON array should not be empty");
        assertTrue(json.startsWith("["), "JSON array should start with [");
        assertTrue(json.endsWith("]"), "JSON array should end with ]");
        assertTrue(json.contains("IDS60901"), "JSON array should contain first station");
        assertTrue(json.contains("IDS60902"), "JSON array should contain second station");
        assertTrue(json.contains("Adelaide"), "JSON array should contain Adelaide");
        assertTrue(json.contains("Melbourne"), "JSON array should contain Melbourne");
    }

    @Test
    void testFromJSONSingleObject() {
        String json = JSONUtils.toJSON(sampleWeatherData);
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
    }

    @Test
    void testFromJSONArray() {
        String json = JSONUtils.toJSON(sampleWeatherArray);
        WeatherData[] parsed = JSONUtils.fromJSONArray(json);

        assertNotNull(parsed, "Parsed array should not be null");
        assertEquals(2, parsed.length, "Array should have 2 elements");

        // Check first element
        assertEquals(sampleWeatherArray[0].getId(), parsed[0].getId(), "First element ID should match");
        assertEquals(sampleWeatherArray[0].getName(), parsed[0].getName(), "First element name should match");
        assertEquals(sampleWeatherArray[0].getAirTemp(), parsed[0].getAirTemp(), 0.001, "First element temperature should match");

        // Check second element
        assertEquals(sampleWeatherArray[1].getId(), parsed[1].getId(), "Second element ID should match");
        assertEquals(sampleWeatherArray[1].getName(), parsed[1].getName(), "Second element name should match");
        assertEquals(sampleWeatherArray[1].getAirTemp(), parsed[1].getAirTemp(), 0.001, "Second element temperature should match");
    }

    @Test
    void testRoundTripConsistency() {
        // Test single object round trip
        String json = JSONUtils.toJSON(sampleWeatherData);
        WeatherData parsed = JSONUtils.fromJSON(json);
        String jsonAgain = JSONUtils.toJSON(parsed);

        WeatherData parsedAgain = JSONUtils.fromJSON(jsonAgain);
        assertEquals(sampleWeatherData.getId(), parsedAgain.getId(), "Round trip should preserve ID");
        assertEquals(sampleWeatherData.getAirTemp(), parsedAgain.getAirTemp(), 0.001, "Round trip should preserve temperature");

        // Test array round trip
        String arrayJson = JSONUtils.toJSON(sampleWeatherArray);
        WeatherData[] parsedArray = JSONUtils.fromJSONArray(arrayJson);
        String arrayJsonAgain = JSONUtils.toJSON(parsedArray);

        WeatherData[] parsedArrayAgain = JSONUtils.fromJSONArray(arrayJsonAgain);
        assertEquals(sampleWeatherArray.length, parsedArrayAgain.length, "Array length should be preserved");
    }

    @Test
    void testNullWeatherData() {
        String json = JSONUtils.toJSON((WeatherData) null);
        assertEquals("null", json, "Null WeatherData should serialize to 'null'");
    }

    @Test
    void testNullWeatherDataArray() {
        String json = JSONUtils.toJSON((WeatherData[]) null);
        assertEquals("null", json, "Null WeatherData array should serialize to 'null'");
    }

    @Test
    void testEmptyWeatherDataArray() {
        WeatherData[] emptyArray = new WeatherData[0];
        String json = JSONUtils.toJSON(emptyArray);

        assertNotNull(json, "Empty array JSON should not be null");
        assertEquals("[]", json.replaceAll("\\s", ""), "Empty array should serialize to []");

        WeatherData[] parsed = JSONUtils.fromJSONArray(json);
        assertNotNull(parsed, "Parsed empty array should not be null");
        assertEquals(0, parsed.length, "Parsed array should be empty");
    }

    @Test
    void testFromJSONNullString() {
        WeatherData result = JSONUtils.fromJSON(null);
        assertNull(result, "Parsing null string should return null");
    }

    @Test
    void testFromJSONArrayNullString() {
        WeatherData[] result = JSONUtils.fromJSONArray(null);
        assertNull(result, "Parsing null string should return null");
    }

    @Test
    void testFromJSONEmptyString() {
        // Empty string parsing behavior may vary, let's test actual behavior
        assertDoesNotThrow(() -> {
            WeatherData result = JSONUtils.fromJSON("");
            // Result may be null or empty object depending on Gson behavior
        }, "Should handle empty string gracefully");
    }

    @Test
    void testFromJSONInvalidJson() {
        assertThrows(Exception.class, () -> {
            JSONUtils.fromJSON("invalid json");
        }, "Parsing invalid JSON should throw exception");
    }

    @Test
    void testFromJSONMalformedWeatherData() {
        String malformedJson = "{\"id\": \"test\", \"invalidField\": true}";

        // Should not throw exception but create object with available fields
        assertDoesNotThrow(() -> {
            WeatherData result = JSONUtils.fromJSON(malformedJson);
            assertNotNull(result, "Should create object despite missing fields");
            assertEquals("test", result.getId(), "Should parse available fields");
        }, "Should handle malformed JSON gracefully");
    }

    @Test
    void testJSONFormatting() {
        String json = JSONUtils.toJSON(sampleWeatherData);

        // Should be pretty printed (contains newlines and indentation)
        assertTrue(json.contains("\n"), "JSON should be pretty printed with newlines");
        assertTrue(json.contains("  "), "JSON should contain indentation");
    }

    @Test
    void testSpecialCharactersInWeatherData() {
        WeatherData specialData = new WeatherData();
        specialData.setId("Test\"With'Quotes");
        specialData.setName("Station with unicode: café, naïve");
        specialData.setCloud("Partly \"cloudy\" with 'mixed' conditions");

        String json = JSONUtils.toJSON(specialData);
        WeatherData parsed = JSONUtils.fromJSON(json);

        assertEquals(specialData.getId(), parsed.getId(), "Should handle quotes in ID");
        assertEquals(specialData.getName(), parsed.getName(), "Should handle unicode characters");
        assertEquals(specialData.getCloud(), parsed.getCloud(), "Should handle quotes in description");
    }

    @Test
    void testNumericFieldPrecision() {
        WeatherData precisionData = new WeatherData();
        precisionData.setAirTemp(13.12345678);
        precisionData.setLat(-34.123456789);
        precisionData.setPress(1023.987654321);

        String json = JSONUtils.toJSON(precisionData);
        WeatherData parsed = JSONUtils.fromJSON(json);

        assertEquals(precisionData.getAirTemp(), parsed.getAirTemp(), 0.000001, "Should preserve temperature precision");
        assertEquals(precisionData.getLat(), parsed.getLat(), 0.000001, "Should preserve latitude precision");
        assertEquals(precisionData.getPress(), parsed.getPress(), 0.000001, "Should preserve pressure precision");
    }

    @Test
    void testExtremeNumericValues() {
        WeatherData extremeData = new WeatherData();
        extremeData.setAirTemp(Double.MAX_VALUE);
        extremeData.setLat(-90.0);
        extremeData.setLon(180.0);
        extremeData.setPress(0.0);
        extremeData.setRelHum(100);
        extremeData.setWindSpdKmh(Integer.MAX_VALUE);

        String json = JSONUtils.toJSON(extremeData);
        WeatherData parsed = JSONUtils.fromJSON(json);

        assertEquals(extremeData.getAirTemp(), parsed.getAirTemp(), "Should handle extreme temperature");
        assertEquals(extremeData.getLat(), parsed.getLat(), "Should handle minimum latitude");
        assertEquals(extremeData.getLon(), parsed.getLon(), "Should handle maximum longitude");
        assertEquals(extremeData.getPress(), parsed.getPress(), "Should handle zero pressure");
        assertEquals(extremeData.getRelHum(), parsed.getRelHum(), "Should handle maximum humidity");
        assertEquals(extremeData.getWindSpdKmh(), parsed.getWindSpdKmh(), "Should handle extreme wind speed");
    }

    @Test
    void testLargeArraySerialization() {
        final int LARGE_SIZE = 1000;
        WeatherData[] largeArray = new WeatherData[LARGE_SIZE];

        for (int i = 0; i < LARGE_SIZE; i++) {
            WeatherData data = new WeatherData();
            data.setId("ID" + i);
            data.setName("Station " + i);
            data.setAirTemp(i * 0.1);
            largeArray[i] = data;
        }

        String json = JSONUtils.toJSON(largeArray);
        assertNotNull(json, "Large array JSON should not be null");
        assertTrue(json.contains("ID0"), "Should contain first element");
        assertTrue(json.contains("ID" + (LARGE_SIZE - 1)), "Should contain last element");

        WeatherData[] parsed = JSONUtils.fromJSONArray(json);
        assertEquals(LARGE_SIZE, parsed.length, "Parsed array should have correct size");
        assertEquals("ID0", parsed[0].getId(), "First element should be correct");
        assertEquals("ID" + (LARGE_SIZE - 1), parsed[LARGE_SIZE - 1].getId(), "Last element should be correct");
    }
}