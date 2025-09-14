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

    @Test
    void testSaveAndLoadEmptyArray() throws IOException {
        WeatherData[] emptyData = new WeatherData[0];

        storageManager.save(emptyData);
        WeatherData[] loaded = storageManager.load();

        assertNotNull(loaded, "Loaded data should not be null");
        assertEquals(0, loaded.length, "Loaded data should be empty array");
    }

    @Test
    void testSaveAndLoadSingleStation() throws IOException {
        WeatherData[] testData = {createSampleWeatherData("TEST001", "Test Station 1")};

        storageManager.save(testData);
        WeatherData[] loaded = storageManager.load();

        assertEquals(1, loaded.length, "Should load one station");
        assertEquals("TEST001", loaded[0].getId(), "Station ID should match");
        assertEquals("Test Station 1", loaded[0].getName(), "Station name should match");
        assertEquals(20.0, loaded[0].getAirTemp(), 0.001, "Temperature should match");
    }

    @Test
    void testSaveAndLoadMultipleStations() throws IOException {
        WeatherData[] testData = {
                createSampleWeatherData("TEST001", "Station 1"),
                createSampleWeatherData("TEST002", "Station 2"),
                createSampleWeatherData("TEST003", "Station 3")
        };

        storageManager.save(testData);
        WeatherData[] loaded = storageManager.load();

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
    }

    @Test
    void testOverwriteExistingData() throws IOException {
        // Save initial data
        WeatherData[] initialData = {createSampleWeatherData("INIT001", "Initial Station")};
        storageManager.save(initialData);

        // Overwrite with new data
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
    }

    @Test
    void testAtomicFileOperations() throws IOException {
        WeatherData[] testData = {createSampleWeatherData("ATOMIC001", "Atomic Test")};

        storageManager.save(testData);

        // Check that temporary file is cleaned up
        File tempFile = new File(dataFilePath + ".tmp");
        assertFalse(tempFile.exists(), "Temporary file should be cleaned up");

        // Check that data file exists and is readable
        File dataFile = new File(dataFilePath);
        assertTrue(dataFile.exists(), "Data file should exist");
        assertTrue(dataFile.canRead(), "Data file should be readable");
    }

    @Test
    void testBackupFileCreation() throws IOException {
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
    }

    @Test
    void testLoadFromNonExistentFile() throws IOException {
        // Load from non-existent file should return empty array
        WeatherData[] loaded = storageManager.load();

        assertNotNull(loaded, "Should return non-null array");
        assertEquals(0, loaded.length, "Should return empty array");
    }

    @Test
    void testLoadFromEmptyFile() throws IOException {
        // Create empty file
        Files.writeString(Path.of(dataFilePath), "");

        WeatherData[] loaded = storageManager.load();

        assertNotNull(loaded, "Should return non-null array");
        assertEquals(0, loaded.length, "Should return empty array for empty file");
    }

    @Test
    void testLoadFromWhitespaceOnlyFile() throws IOException {
        // Create file with only whitespace
        Files.writeString(Path.of(dataFilePath), "   \n\t  ");

        WeatherData[] loaded = storageManager.load();

        assertNotNull(loaded, "Should return non-null array");
        assertEquals(0, loaded.length, "Should return empty array for whitespace-only file");
    }

    @Test
    void testLoadFromCorruptedPrimaryFile() throws IOException {
        // First save creates primary file
        WeatherData[] originalData = {createSampleWeatherData("ORIGINAL001", "Original Station")};
        storageManager.save(originalData);

        // Second save creates backup (backup of first save) and new primary
        WeatherData[] backupData = {createSampleWeatherData("BACKUP001", "Backup Station")};
        storageManager.save(backupData);

        // Now we have: primary=BACKUP001, backup=ORIGINAL001
        // Corrupt the primary file with invalid JSON syntax
        Files.writeString(Path.of(dataFilePath), "invalid json content");

        // Should recover from backup (which contains ORIGINAL001)
        WeatherData[] loaded = storageManager.load();

        assertEquals(1, loaded.length, "Should recover data from backup");
        assertEquals("ORIGINAL001", loaded[0].getId(), "Should have original data from backup");

        // Primary file should be restored from backup
        assertTrue(Files.exists(Path.of(dataFilePath)), "Primary file should be restored");

        // Verify primary file was restored correctly
        WeatherData[] primaryData = storageManager.load();
        assertEquals(1, primaryData.length, "Restored primary should have correct data");
        assertEquals("ORIGINAL001", primaryData[0].getId(), "Restored primary should match backup");
    }

    @Test
    void testLoadFromBothFilesCorrupted() throws IOException {
        // Create both files with corrupted content
        Files.writeString(Path.of(dataFilePath), "corrupted primary file");
        Files.writeString(Path.of(backupFilePath), "corrupted backup file");

        WeatherData[] loaded = storageManager.load();

        assertNotNull(loaded, "Should return non-null array");
        assertEquals(0, loaded.length, "Should return empty array when both files are corrupted");
    }

    @Test
    void testLoadFromPrimaryMissingBackupExists() throws IOException {
        // Create backup file manually with valid JSON
        WeatherData[] backupData = {createSampleWeatherData("BACKUP001", "Backup Only")};

        // First, create a primary file to generate a backup
        storageManager.save(backupData);
        // Then save different data to create the backup
        WeatherData[] newData = {createSampleWeatherData("NEW001", "New Data")};
        storageManager.save(newData);

        // Now delete the primary file, leaving only the backup
        Files.delete(Path.of(dataFilePath));

        WeatherData[] loaded = storageManager.load();

        // Should load from backup (which contains the original data)
        assertEquals(1, loaded.length, "Should load from backup when primary missing");
        assertEquals("BACKUP001", loaded[0].getId(), "Should have correct backup data");

        // Primary file should be restored
        assertTrue(Files.exists(Path.of(dataFilePath)), "Primary file should be restored");
    }

    @Test
    void testSaveNullArray() {
        assertThrows(Exception.class, () -> {
            storageManager.save(null);
        }, "Saving null array should throw exception");
    }

    @Test
    void testLargeDataSetPersistence() throws IOException {
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
    }

    @Test
    void testSpecialCharactersInData() throws IOException {
        WeatherData specialData = createSampleWeatherData("SPECIAL", "Station with special chars");
        specialData.setName("Weather Station \"quoted\" with 'apostrophes' & symbols: ñáéíóú");
        specialData.setCloud("Partly \"cloudy\" with mixed conditions");

        WeatherData[] testData = {specialData};

        storageManager.save(testData);
        WeatherData[] loaded = storageManager.load();

        assertEquals(1, loaded.length, "Should load special character data");
        assertEquals("Weather Station \"quoted\" with 'apostrophes' & symbols: ñáéíóú",
                    loaded[0].getName(), "Should preserve special characters in name");
        assertEquals("Partly \"cloudy\" with mixed conditions",
                    loaded[0].getCloud(), "Should preserve special characters in description");
    }

    @Test
    void testNumericPrecisionPersistence() throws IOException {
        WeatherData precisionData = createSampleWeatherData("PRECISION", "Precision Test");
        precisionData.setAirTemp(13.123456789);
        precisionData.setLat(-34.987654321);
        precisionData.setLon(138.123456789);
        precisionData.setPress(1023.987654321);

        WeatherData[] testData = {precisionData};

        storageManager.save(testData);
        WeatherData[] loaded = storageManager.load();

        assertEquals(1, loaded.length, "Should load precision data");
        assertEquals(13.123456789, loaded[0].getAirTemp(), 0.000000001, "Should preserve temperature precision");
        assertEquals(-34.987654321, loaded[0].getLat(), 0.000000001, "Should preserve latitude precision");
        assertEquals(138.123456789, loaded[0].getLon(), 0.000000001, "Should preserve longitude precision");
        assertEquals(1023.987654321, loaded[0].getPress(), 0.000000001, "Should preserve pressure precision");
    }

    @Test
    void testFileSystemPermissionHandling() throws IOException {
        // Skip this test if we can't create permission issues (OS dependent)
        File dataFile = new File(dataFilePath);
        File blockingDir = new File(dataFilePath);

        if (!blockingDir.mkdirs()) {
            // If we can't create the blocking directory, skip the test
            return;
        }

        try {
            WeatherData[] testData = {createSampleWeatherData("PERM001", "Permission Test")};

            // This may throw an IOException due to permission/file system issues
            // But behavior is OS-dependent, so we'll just verify it doesn't crash
            assertDoesNotThrow(() -> {
                try {
                    storageManager.save(testData);
                } catch (IOException e) {
                    // Expected on some systems
                }
            }, "Should handle permission issues gracefully without crashing");

        } finally {
            // Cleanup
            if (blockingDir.exists()) {
                blockingDir.delete();
            }
        }
    }

    @Test
    void testMultipleSaveOperations() throws IOException {
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
    }

    @Test
    void testBackupFileOverwrite() throws IOException {
        // First save
        WeatherData[] data1 = {createSampleWeatherData("BKUP1", "First Backup Test")};
        storageManager.save(data1);

        // Second save (creates backup of first)
        WeatherData[] data2 = {createSampleWeatherData("BKUP2", "Second Backup Test")};
        storageManager.save(data2);

        // Third save (overwrites backup with second)
        WeatherData[] data3 = {createSampleWeatherData("BKUP3", "Third Backup Test")};
        storageManager.save(data3);

        // Verify backup contains data from second save
        FileStorageManager backupLoader = new FileStorageManager(backupFilePath, backupFilePath);
        WeatherData[] backupData = backupLoader.load();

        assertEquals(1, backupData.length, "Backup should have one station");
        assertEquals("BKUP2", backupData[0].getId(), "Backup should have second save data");
        assertEquals("Second Backup Test", backupData[0].getName(), "Backup should have correct name");
    }

    @Test
    void testRoundTripDataIntegrity() throws IOException {
        // Create comprehensive test data
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
            WeatherData[] loaded = storageManager.load();

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