package com.weathersystem.server.persistence;

import com.weathersystem.shared.domain.WeatherData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for FileStorageManager class.
 * Tests file persistence, backup creation, error recovery, and data integrity.
 */
class FileStorageManagerTest {

    @TempDir
    Path tempDir;

    private FileStorageManager storageManager;
    private String dataFilePath;
    private String backupFilePath;

    @BeforeEach
    void setUp() {
        dataFilePath = tempDir.resolve("test_data.json").toString();
        backupFilePath = tempDir.resolve("test_backup.json").toString();
        storageManager = new FileStorageManager(dataFilePath, backupFilePath);
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

    // === CORE FUNCTIONALITY TESTS ===

    @Test
    // Tests basic save and load functionality with empty, single, and multiple weather stations
    void testBasicSaveAndLoad() throws IOException {
        System.out.println("Testing basic save and load functionality...");
        
        // Test empty array
        WeatherData[] emptyData = new WeatherData[0];
        storageManager.save(emptyData);
        WeatherData[] loaded = storageManager.load();
        assertNotNull(loaded, "Loaded data should not be null");
        assertEquals(0, loaded.length, "Loaded data should be empty array");

        // Test single station
        WeatherData[] singleData = {createSampleWeatherData("TEST001", "Test Station 1")};
        storageManager.save(singleData);
        loaded = storageManager.load();
        assertEquals(1, loaded.length, "Should load one station");
        assertEquals("TEST001", loaded[0].getId(), "Station ID should match");
        assertEquals("Test Station 1", loaded[0].getName(), "Station name should match");
        assertEquals(20.0, loaded[0].getAirTemp(), 0.001, "Temperature should match");

        // Test multiple stations
        WeatherData[] multipleData = {
                createSampleWeatherData("TEST001", "Station 1"),
                createSampleWeatherData("TEST002", "Station 2"),
                createSampleWeatherData("TEST003", "Station 3")
        };
        storageManager.save(multipleData);
        loaded = storageManager.load();
        assertEquals(3, loaded.length, "Should load three stations");

        // Verify each station exists
        boolean found1 = false, found2 = false, found3 = false;
        for (WeatherData data : loaded) {
            switch (data.getId()) {
                case "TEST001":
                    found1 = true;
                    assertEquals("Station 1", data.getName());
                    break;
                case "TEST002":
                    found2 = true;
                    assertEquals("Station 2", data.getName());
                    break;
                case "TEST003":
                    found3 = true;
                    assertEquals("Station 3", data.getName());
                    break;
            }
        }
        assertTrue(found1, "Should find station 1");
        assertTrue(found2, "Should find station 2");
        assertTrue(found3, "Should find station 3");
        
        System.out.println("✓ Basic save and load test passed");
    }

    @Test
    // Tests data overwrite and atomic file operations with temporary file cleanup
    void testDataOverwriteAndAtomicOperations() throws IOException {
        System.out.println("Testing data overwrite and atomic operations...");
        
        // Test data overwrite
        WeatherData[] initialData = {createSampleWeatherData("INIT001", "Initial Station")};
        storageManager.save(initialData);

        WeatherData[] newData = {
                createSampleWeatherData("NEW001", "New Station 1"),
                createSampleWeatherData("NEW002", "New Station 2")
        };
        storageManager.save(newData);

        WeatherData[] loaded = storageManager.load();
        assertEquals(2, loaded.length, "Should have new data with 2 stations");
        assertFalse(containsStationId(loaded, "INIT001"), "Should not contain initial station");
        assertTrue(containsStationId(loaded, "NEW001"), "Should contain new station 1");
        assertTrue(containsStationId(loaded, "NEW002"), "Should contain new station 2");

        // Test atomic file operations
        WeatherData[] atomicData = {createSampleWeatherData("ATOMIC001", "Atomic Test")};
        storageManager.save(atomicData);

        // Check that temporary file is cleaned up
        File tempFile = new File(dataFilePath + ".tmp");
        assertFalse(tempFile.exists(), "Temporary file should be cleaned up");

        // Check that data file exists and is readable
        File dataFile = new File(dataFilePath);
        assertTrue(dataFile.exists(), "Data file should exist");
        assertTrue(dataFile.canRead(), "Data file should be readable");
        
        System.out.println("✓ Data overwrite and atomic operations test passed");
    }

    @Test
    // Tests backup file management including creation, overwrite, and recovery
    void testBackupFileManagement() throws IOException {
        System.out.println("Testing backup file management...");
        
        // First save creates no backup (no existing file)
        WeatherData[] data1 = {createSampleWeatherData("BACKUP001", "First Save")};
        storageManager.save(data1);

        File backupFile = new File(backupFilePath);
        assertFalse(backupFile.exists(), "No backup should exist after first save");

        // Second save should create backup of first save
        WeatherData[] data2 = {createSampleWeatherData("BACKUP002", "Second Save")};
        storageManager.save(data2);

        assertTrue(backupFile.exists(), "Backup file should exist after second save");

        // Load backup and verify it contains first save data
        FileStorageManager backupLoader = new FileStorageManager(backupFilePath, backupFilePath);
        WeatherData[] backupData = backupLoader.load();

        assertEquals(1, backupData.length, "Backup should contain data from first save");
        assertEquals("BACKUP001", backupData[0].getId(), "Backup should have first save data");

        // Test backup file overwrite
        WeatherData[] data3 = {createSampleWeatherData("BACKUP003", "Third Save")};
        storageManager.save(data3);

        // Verify backup contains data from second save
        backupData = backupLoader.load();
        assertEquals(1, backupData.length, "Backup should have one station");
        assertEquals("BACKUP002", backupData[0].getId(), "Backup should have second save data");
        assertEquals("Second Save", backupData[0].getName(), "Backup should have correct name");
        
        System.out.println("✓ Backup file management test passed");
    }

    // === ERROR HANDLING TESTS ===

    @Test
    // Tests error handling and recovery from corrupted files and missing data
    void testErrorHandlingAndRecovery() throws IOException {
        System.out.println("Testing error handling and recovery...");
        
        // Test loading from non-existent file
        WeatherData[] loaded = storageManager.load();
        assertNotNull(loaded, "Should return non-null array");
        assertEquals(0, loaded.length, "Should return empty array");

        // Test loading from empty file
        Files.writeString(Path.of(dataFilePath), "");
        loaded = storageManager.load();
        assertNotNull(loaded, "Should return non-null array");
        assertEquals(0, loaded.length, "Should return empty array for empty file");

        // Test loading from whitespace-only file
        Files.writeString(Path.of(dataFilePath), "   \n\t  ");
        loaded = storageManager.load();
        assertNotNull(loaded, "Should return non-null array");
        assertEquals(0, loaded.length, "Should return empty array for whitespace-only file");

        // Test corrupted primary file recovery
        WeatherData[] originalData = {createSampleWeatherData("ORIGINAL001", "Original Station")};
        storageManager.save(originalData);

        WeatherData[] backupData = {createSampleWeatherData("BACKUP001", "Backup Station")};
        storageManager.save(backupData);

        // Corrupt the primary file with invalid JSON syntax
        Files.writeString(Path.of(dataFilePath), "invalid json content");

        // Should recover from backup (which contains ORIGINAL001)
        loaded = storageManager.load();
        assertEquals(1, loaded.length, "Should recover data from backup");
        assertEquals("ORIGINAL001", loaded[0].getId(), "Should have original data from backup");

        // Primary file should be restored from backup
        assertTrue(Files.exists(Path.of(dataFilePath)), "Primary file should be restored");

        // Test both files corrupted
        Files.writeString(Path.of(dataFilePath), "corrupted primary file");
        Files.writeString(Path.of(backupFilePath), "corrupted backup file");

        loaded = storageManager.load();
        assertNotNull(loaded, "Should return non-null array");
        assertEquals(0, loaded.length, "Should return empty array when both files are corrupted");

        // Test primary missing, backup exists
        WeatherData[] testData = {createSampleWeatherData("BACKUP001", "Backup Only")};
        storageManager.save(testData);
        WeatherData[] newData = {createSampleWeatherData("NEW001", "New Data")};
        storageManager.save(newData);

        // Delete the primary file, leaving only the backup
        Files.delete(Path.of(dataFilePath));

        loaded = storageManager.load();
        assertEquals(1, loaded.length, "Should load from backup when primary missing");
        assertEquals("BACKUP001", loaded[0].getId(), "Should have correct backup data");

        // Primary file should be restored
        assertTrue(Files.exists(Path.of(dataFilePath)), "Primary file should be restored");

        // Test saving null array
        assertThrows(Exception.class, () -> {
            storageManager.save(null);
        }, "Saving null array should throw exception");
        
        System.out.println("✓ Error handling and recovery test passed");
    }

    // === EDGE CASES TESTS ===

    @Test
    // Tests large dataset persistence and special data including Unicode and precision
    void testLargeDataSetAndSpecialData() throws IOException {
        System.out.println("Testing large dataset and special data...");
        
        // Test large dataset persistence
        final int LARGE_SIZE = 1000;
        WeatherData[] largeDataSet = new WeatherData[LARGE_SIZE];

        for (int i = 0; i < LARGE_SIZE; i++) {
            largeDataSet[i] = createSampleWeatherData("LARGE" + i, "Station " + i);
            largeDataSet[i].setAirTemp(i * 0.1); // Different temperatures for each
        }

        storageManager.save(largeDataSet);
        WeatherData[] loaded = storageManager.load();

        assertEquals(LARGE_SIZE, loaded.length, "Should load all stations");

        // Verify some sample data
        boolean foundFirst = false, foundLast = false;
        for (WeatherData data : loaded) {
            if ("LARGE0".equals(data.getId())) {
                foundFirst = true;
                assertEquals(0.0, data.getAirTemp(), 0.001);
            }
            if (("LARGE" + (LARGE_SIZE - 1)).equals(data.getId())) {
                foundLast = true;
                assertEquals((LARGE_SIZE - 1) * 0.1, data.getAirTemp(), 0.001);
            }
        }
        assertTrue(foundFirst, "Should find first station");
        assertTrue(foundLast, "Should find last station");

        // Test special characters in data
        WeatherData specialData = createSampleWeatherData("SPECIAL", "Station with special chars");
        specialData.setName("Weather Station \"quoted\" with 'apostrophes' & symbols: ñáéíóú");
        specialData.setCloud("Partly \"cloudy\" with mixed conditions");

        WeatherData[] testData = {specialData};
        storageManager.save(testData);
        loaded = storageManager.load();

        assertEquals(1, loaded.length, "Should load special character data");
        assertEquals("Weather Station \"quoted\" with 'apostrophes' & symbols: ñáéíóú",
                    loaded[0].getName(), "Should preserve special characters in name");
        assertEquals("Partly \"cloudy\" with mixed conditions",
                    loaded[0].getCloud(), "Should preserve special characters in description");

        // Test numeric precision persistence
        WeatherData precisionData = createSampleWeatherData("PRECISION", "Precision Test");
        precisionData.setAirTemp(13.123456789);
        precisionData.setLat(-34.987654321);
        precisionData.setLon(138.123456789);
        precisionData.setPress(1023.987654321);

        testData = new WeatherData[]{precisionData};
        storageManager.save(testData);
        loaded = storageManager.load();

        assertEquals(1, loaded.length, "Should load precision data");
        assertEquals(13.123456789, loaded[0].getAirTemp(), 0.000000001, "Should preserve temperature precision");
        assertEquals(-34.987654321, loaded[0].getLat(), 0.000000001, "Should preserve latitude precision");
        assertEquals(138.123456789, loaded[0].getLon(), 0.000000001, "Should preserve longitude precision");
        assertEquals(1023.987654321, loaded[0].getPress(), 0.000000001, "Should preserve pressure precision");
        
        System.out.println("✓ Large dataset and special data test passed");
    }

    @Test
    // Tests multiple operations and data integrity across save/load cycles
    void testMultipleOperationsAndDataIntegrity() throws IOException {
        System.out.println("Testing multiple operations and data integrity...");
        
        // Test multiple sequential save operations
        for (int i = 0; i < 10; i++) {
            WeatherData[] data = {createSampleWeatherData("MULTI" + i, "Multi Station " + i)};
            storageManager.save(data);
        }

        // Load final state
        WeatherData[] loaded = storageManager.load();
        assertEquals(1, loaded.length, "Should have final save data");
        assertEquals("MULTI9", loaded[0].getId(), "Should have data from last save");
        assertEquals("Multi Station 9", loaded[0].getName(), "Should have correct name from last save");

        // Test round-trip data integrity
        WeatherData[] originalData = {
                createSampleWeatherData("INTEGRITY1", "First Station"),
                createSampleWeatherData("INTEGRITY2", "Second Station")
        };

        // Modify some values to test all fields
        originalData[0].setAirTemp(25.7);
        originalData[0].setRelHum(85);
        originalData[1].setWindDir("SW");
        originalData[1].setWindSpdKmh(25);

        // Save and load multiple times
        for (int i = 0; i < 5; i++) {
            storageManager.save(originalData);
            loaded = storageManager.load();

            assertEquals(originalData.length, loaded.length, "Array length should be preserved");

            for (int j = 0; j < originalData.length; j++) {
                assertEquals(originalData[j].getId(), loaded[j].getId(), "ID should match");
                assertEquals(originalData[j].getName(), loaded[j].getName(), "Name should match");
                assertEquals(originalData[j].getAirTemp(), loaded[j].getAirTemp(), 0.001, "Temperature should match");
                assertEquals(originalData[j].getRelHum(), loaded[j].getRelHum(), "Humidity should match");
                assertEquals(originalData[j].getWindDir(), loaded[j].getWindDir(), "Wind direction should match");
                assertEquals(originalData[j].getWindSpdKmh(), loaded[j].getWindSpdKmh(), "Wind speed should match");
            }

            originalData = loaded; // Use loaded data for next iteration
        }
        
        System.out.println("✓ Multiple operations and data integrity test passed");
    }

    private boolean containsStationId(WeatherData[] data, String id) {
        for (WeatherData station : data) {
            if (id.equals(station.getId())) {
                return true;
            }
        }
        return false;
    }
}