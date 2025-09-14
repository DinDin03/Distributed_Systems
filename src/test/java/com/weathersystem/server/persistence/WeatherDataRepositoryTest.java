package com.weathersystem.server.persistence;

import com.weathersystem.shared.domain.WeatherData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.awaitility.Awaitility;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WeatherDataRepositoryTest {

    @TempDir
    Path tempDir;

    private WeatherDataRepository repository;
    private String dataFilePath;
    private String backupFilePath;

    @BeforeEach
    void setUp() {
        dataFilePath = tempDir.resolve("weather_data.json").toString();
        backupFilePath = tempDir.resolve("weather_data_backup.json").toString();
        repository = new WeatherDataRepository(dataFilePath, backupFilePath);
    }

    @AfterEach
    void tearDown() {
        if (repository != null) {
            repository.shutdown();
            try {
                // Give shutdown time to complete
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
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
    void testLoadFromEmptyRepository() {
        WeatherData[] data = repository.load();

        assertNotNull(data, "Load should return non-null array");
        assertEquals(0, data.length, "Empty repository should return empty array");
    }

    @Test
    void testSaveAndLoadSingleStation() {
        WeatherData[] testData = {createSampleWeatherData("TEST001", "Test Station 1")};

        repository.saveAsync(testData);

        // Wait for async save to complete
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> new File(dataFilePath).exists());

        WeatherData[] loaded = repository.load();

        assertEquals(1, loaded.length, "Should load one station");
        assertEquals("TEST001", loaded[0].getId(), "Station ID should match");
        assertEquals("Test Station 1", loaded[0].getName(), "Station name should match");
        assertEquals(20.0, loaded[0].getAirTemp(), 0.001, "Temperature should match");
    }

    @Test
    void testSaveAndLoadMultipleStations() {
        WeatherData[] testData = {
                createSampleWeatherData("TEST001", "Test Station 1"),
                createSampleWeatherData("TEST002", "Test Station 2"),
                createSampleWeatherData("TEST003", "Test Station 3")
        };

        repository.saveAsync(testData);

        // Wait for async save to complete
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> new File(dataFilePath).exists() &&
                          repository.load().length == 3);

        WeatherData[] loaded = repository.load();

        assertEquals(3, loaded.length, "Should load three stations");

        // Verify each station
        boolean foundStation1 = false, foundStation2 = false, foundStation3 = false;
        for (WeatherData data : loaded) {
            switch (data.getId()) {
                case "TEST001":
                    foundStation1 = true;
                    assertEquals("Test Station 1", data.getName());
                    break;
                case "TEST002":
                    foundStation2 = true;
                    assertEquals("Test Station 2", data.getName());
                    break;
                case "TEST003":
                    foundStation3 = true;
                    assertEquals("Test Station 3", data.getName());
                    break;
            }
        }

        assertTrue(foundStation1, "Should find station 1");
        assertTrue(foundStation2, "Should find station 2");
        assertTrue(foundStation3, "Should find station 3");
    }

    @Test
    void testMultipleAsyncSaves() throws InterruptedException {
        final int numSaves = 10;
        final CountDownLatch latch = new CountDownLatch(numSaves);
        final AtomicInteger saveCount = new AtomicInteger(0);

        // Perform multiple async saves
        for (int i = 0; i < numSaves; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    WeatherData[] data = {createSampleWeatherData("TEST" + index, "Station " + index)};
                    repository.saveAsync(data);
                    saveCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All async saves should complete");
        assertEquals(numSaves, saveCount.get(), "All saves should be initiated");

        // Wait for the last save to complete
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> new File(dataFilePath).exists());

        // The final state should be the last save that completed
        WeatherData[] finalData = repository.load();
        assertEquals(1, finalData.length, "Should have data from one save");
    }

    @Test
    void testSaveEmptyArray() {
        WeatherData[] emptyData = new WeatherData[0];

        repository.saveAsync(emptyData);

        // Wait for async save to complete
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> new File(dataFilePath).exists());

        WeatherData[] loaded = repository.load();
        assertEquals(0, loaded.length, "Should load empty array");
    }

    @Test
    void testSaveNullArray() {
        // Test that saving null doesn't crash the system
        assertDoesNotThrow(() -> {
            repository.saveAsync(null);
            // Give async operation time to complete
            Thread.sleep(500);
        }, "Should handle null array gracefully");
    }

    @Test
    void testLoadWithCorruptedFile() throws IOException {
        // Create a corrupted data file with invalid JSON
        Files.writeString(Path.of(dataFilePath), "corrupted json content");

        WeatherData[] loaded = repository.load();

        assertEquals(0, loaded.length, "Corrupted file should return empty array");
    }

    @Test
    void testLoadWithEmptyFile() throws IOException {
        // Create empty data file
        Files.writeString(Path.of(dataFilePath), "");

        WeatherData[] loaded = repository.load();

        assertEquals(0, loaded.length, "Empty file should return empty array");
    }

    @Test
    void testBackupAndRecovery() throws IOException {
        // First, save some data to create primary file
        WeatherData[] originalData = {createSampleWeatherData("BACKUP001", "Backup Test Station")};
        repository.saveAsync(originalData);

        // Wait for save to complete
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> new File(dataFilePath).exists());

        // Save different data to trigger backup creation
        WeatherData[] newData = {createSampleWeatherData("BACKUP002", "New Test Station")};
        repository.saveAsync(newData);

        // Wait for second save to complete (this should create the backup)
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> new File(backupFilePath).exists());

        // Verify backup file was created
        assertTrue(new File(backupFilePath).exists(), "Backup file should be created after second save");

        // Corrupt the primary file with invalid JSON
        Files.writeString(Path.of(dataFilePath), "corrupted data file");

        // Load should recover from backup
        WeatherData[] recovered = repository.load();

        assertEquals(1, recovered.length, "Should recover one station from backup");
        assertEquals("BACKUP001", recovered[0].getId(), "Recovered station ID should match original data");
        assertEquals("Backup Test Station", recovered[0].getName(), "Recovered station name should match");

        // Primary file should be restored after recovery
        assertTrue(new File(dataFilePath).exists(), "Primary file should be restored");
    }

    @Test
    void testBothFilesCorrupted() throws IOException {
        // Create corrupted files with invalid JSON
        Files.writeString(Path.of(dataFilePath), "corrupted primary file");
        Files.writeString(Path.of(backupFilePath), "corrupted backup file");

        WeatherData[] loaded = repository.load();

        assertEquals(0, loaded.length, "Both corrupted files should return empty array");
    }

    @Test
    void testRepositoryShutdown() throws InterruptedException {
        // Start an async save
        WeatherData[] testData = {createSampleWeatherData("SHUTDOWN001", "Shutdown Test")};
        repository.saveAsync(testData);

        // Shutdown repository
        repository.shutdown();

        // Should shutdown gracefully without exceptions
        assertDoesNotThrow(() -> {
            repository.shutdown(); // Second shutdown should be safe
        }, "Multiple shutdowns should be safe");
    }

    @Test
    void testConcurrentSaveAndLoad() throws InterruptedException {
        final int numOperations = 10; // Reduced to avoid temp dir issues
        final CountDownLatch saveLatch = new CountDownLatch(numOperations);
        final CountDownLatch loadLatch = new CountDownLatch(numOperations);
        final AtomicInteger successfulLoads = new AtomicInteger(0);

        // Start concurrent save operations
        for (int i = 0; i < numOperations; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    WeatherData[] data = {createSampleWeatherData("CONCURRENT" + index, "Station " + index)};
                    repository.saveAsync(data);
                } finally {
                    saveLatch.countDown();
                }
            }).start();
        }

        // Start concurrent load operations
        for (int i = 0; i < numOperations; i++) {
            new Thread(() -> {
                try {
                    WeatherData[] loaded = repository.load();
                    if (loaded != null) {
                        successfulLoads.incrementAndGet();
                    }
                } finally {
                    loadLatch.countDown();
                }
            }).start();
        }

        assertTrue(saveLatch.await(10, TimeUnit.SECONDS), "All save operations should complete");
        assertTrue(loadLatch.await(10, TimeUnit.SECONDS), "All load operations should complete");

        assertTrue(successfulLoads.get() >= numOperations * 0.7, "Most load operations should succeed (at least 70%)");
    }

    @Test
    void testFilePermissionsHandling() {
        // Create a directory with the same name as data file to simulate permission issues
        File dataDir = new File(dataFilePath);
        dataDir.mkdirs();

        WeatherData[] testData = {createSampleWeatherData("PERM001", "Permission Test")};

        // Save should handle permission issues gracefully
        assertDoesNotThrow(() -> {
            repository.saveAsync(testData);

            // Wait a bit for async operation
            Thread.sleep(1000);
        }, "Should handle permission issues gracefully");

        // Cleanup
        dataDir.delete();
    }

    @Test
    void testLargeDataSet() {
        final int LARGE_SIZE = 1000;
        WeatherData[] largeDataSet = new WeatherData[LARGE_SIZE];

        for (int i = 0; i < LARGE_SIZE; i++) {
            largeDataSet[i] = createSampleWeatherData("LARGE" + i, "Large Station " + i);
            largeDataSet[i].setAirTemp(i * 0.1); // Different temperatures
        }

        repository.saveAsync(largeDataSet);

        // Wait for large save to complete
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> {
                    WeatherData[] loaded = repository.load();
                    return loaded.length == LARGE_SIZE;
                });

        WeatherData[] loaded = repository.load();

        assertEquals(LARGE_SIZE, loaded.length, "Should load all stations");

        // Verify some sample data
        boolean foundFirst = false, foundLast = false;
        for (WeatherData data : loaded) {
            if ("LARGE0".equals(data.getId())) {
                foundFirst = true;
                assertEquals(0.0, data.getAirTemp(), 0.001, "First station temperature should be correct");
            }
            if (("LARGE" + (LARGE_SIZE - 1)).equals(data.getId())) {
                foundLast = true;
                assertEquals((LARGE_SIZE - 1) * 0.1, data.getAirTemp(), 0.001,
                           "Last station temperature should be correct");
            }
        }

        assertTrue(foundFirst, "Should find first station");
        assertTrue(foundLast, "Should find last station");
    }

    @Test
    void testAsyncSaveErrorHandling() throws InterruptedException {
        // Create a repository with invalid paths to trigger errors
        WeatherDataRepository errorRepository = new WeatherDataRepository(
            "/invalid/path/that/does/not/exist",
            "/another/invalid/path"
        );

        WeatherData[] testData = {createSampleWeatherData("ERROR001", "Error Test")};

        // Async save with invalid path should handle error gracefully
        assertDoesNotThrow(() -> {
            errorRepository.saveAsync(testData);

            // Give async operation time to complete
            Thread.sleep(2000);
        }, "Should handle async save errors gracefully");

        errorRepository.shutdown();
    }

    @Test
    void testDataIntegrityAfterMultipleSaves() {
        WeatherData[] data1 = {createSampleWeatherData("INTEGRITY1", "First Save")};
        WeatherData[] data2 = {createSampleWeatherData("INTEGRITY2", "Second Save")};
        WeatherData[] data3 = {createSampleWeatherData("INTEGRITY3", "Third Save")};

        repository.saveAsync(data1);
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> repository.load().length == 1);

        repository.saveAsync(data2);
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> "INTEGRITY2".equals(repository.load()[0].getId()));

        repository.saveAsync(data3);
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> "INTEGRITY3".equals(repository.load()[0].getId()));

        // Final verification
        WeatherData[] finalData = repository.load();
        assertEquals(1, finalData.length, "Should have final dataset");
        assertEquals("INTEGRITY3", finalData[0].getId(), "Should have data from last save");
        assertEquals("Third Save", finalData[0].getName(), "Should have correct name from last save");
    }
}