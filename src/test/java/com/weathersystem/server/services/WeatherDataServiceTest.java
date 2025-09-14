package com.weathersystem.server.services;

import com.weathersystem.shared.domain.WeatherData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for WeatherDataService class.
 * Tests weather data storage, retrieval, concurrent access, and data integrity.
 */
class WeatherDataServiceTest {

    private WeatherDataService weatherDataService;

    @BeforeEach
    void setUp() {
        weatherDataService = new WeatherDataService();
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
    void testBasicStorageAndRetrieval() {
        System.out.println("Testing basic storage and retrieval functionality...");
        
        // Test new station storage
        WeatherData newStation = createSampleWeatherData("NEW001", "New Station");
        boolean isNew = weatherDataService.storeWeatherData(newStation);
        
        assertTrue(isNew, "Should return true for new station");
        assertEquals(1, weatherDataService.getStationCount(), "Should have one station");
        
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should return one station");
        assertEquals("NEW001", allData[0].getId(), "Should have correct station ID");
        assertEquals("New Station", allData[0].getName(), "Should have correct station name");

        // Test existing station update
        WeatherData updatedStation = createSampleWeatherData("NEW001", "Updated Name");
        boolean isNewUpdate = weatherDataService.storeWeatherData(updatedStation);
        
        assertFalse(isNewUpdate, "Second store should return false");
        assertEquals(1, weatherDataService.getStationCount(), "Should still have one station");
        
        allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should return one station");
        assertEquals("NEW001", allData[0].getId(), "Should have correct station ID");
        assertEquals("Updated Name", allData[0].getName(), "Should have updated name");

        // Test multiple stations
        WeatherData station1 = createSampleWeatherData("MULTI001", "Station 1");
        WeatherData station2 = createSampleWeatherData("MULTI002", "Station 2");
        WeatherData station3 = createSampleWeatherData("MULTI003", "Station 3");
        
        weatherDataService.storeWeatherData(station1);
        weatherDataService.storeWeatherData(station2);
        weatherDataService.storeWeatherData(station3);
        
        assertEquals(4, weatherDataService.getStationCount(), "Should have four stations total");
        
        allData = weatherDataService.getAllWeatherData();
        assertEquals(4, allData.length, "Should return four stations");
        
        // Verify all stations are present
        boolean found1 = false, found2 = false, found3 = false, foundUpdated = false;
        for (WeatherData data : allData) {
            switch (data.getId()) {
                case "MULTI001":
                    found1 = true;
                    assertEquals("Station 1", data.getName());
                    break;
                case "MULTI002":
                    found2 = true;
                    assertEquals("Station 2", data.getName());
                    break;
                case "MULTI003":
                    found3 = true;
                    assertEquals("Station 3", data.getName());
                    break;
                case "NEW001":
                    foundUpdated = true;
                    assertEquals("Updated Name", data.getName());
                    break;
            }
        }
        
        assertTrue(found1, "Should find station 1");
        assertTrue(found2, "Should find station 2");
        assertTrue(found3, "Should find station 3");
        assertTrue(foundUpdated, "Should find updated station");
        
        System.out.println("✓ Basic storage and retrieval test passed");
    }

    // === ERROR HANDLING TESTS ===

    @Test
    void testErrorHandlingAndDataLoading() {
        System.out.println("Testing error handling and data loading...");
        
        // Test null data handling
        assertThrows(IllegalArgumentException.class, () -> {
            weatherDataService.storeWeatherData(null);
        }, "Should throw exception for null data");

        // Test null ID handling
        WeatherData station = createSampleWeatherData(null, "Test Station");
        assertThrows(IllegalArgumentException.class, () -> {
            weatherDataService.storeWeatherData(station);
        }, "Should throw exception for null ID");

        // Test empty service
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertNotNull(allData, "Should return non-null array");
        assertEquals(0, allData.length, "Should return empty array");
        assertEquals(0, weatherDataService.getStationCount(), "Should have zero stations");

        // Test clear functionality
        WeatherData clearStation = createSampleWeatherData("CLEAR001", "Clear Test");
        weatherDataService.storeWeatherData(clearStation);
        assertEquals(1, weatherDataService.getStationCount(), "Should have one station");
        
        weatherDataService.clearAllData();
        assertEquals(0, weatherDataService.getStationCount(), "Should have zero stations after clear");
        
        allData = weatherDataService.getAllWeatherData();
        assertEquals(0, allData.length, "Should return empty array after clear");

        // Test data loading
        WeatherData[] stations = {
            createSampleWeatherData("LOAD001", "Load Station 1"),
            createSampleWeatherData("LOAD002", "Load Station 2")
        };
        
        long timestamp = System.currentTimeMillis();
        weatherDataService.loadWeatherData(stations, timestamp);
        
        assertEquals(2, weatherDataService.getStationCount(), "Should have two stations after load");
        
        allData = weatherDataService.getAllWeatherData();
        assertEquals(2, allData.length, "Should return two stations");
        
        // Verify stations are present
        boolean found1 = false, found2 = false;
        for (WeatherData data : allData) {
            if ("LOAD001".equals(data.getId())) {
                found1 = true;
                assertEquals("Load Station 1", data.getName());
            } else if ("LOAD002".equals(data.getId())) {
                found2 = true;
                assertEquals("Load Station 2", data.getName());
            }
        }
        assertTrue(found1, "Should find station 1");
        assertTrue(found2, "Should find station 2");

        // Test loading null data (should not clear existing data)
        weatherDataService.loadWeatherData(null, System.currentTimeMillis());
        assertEquals(2, weatherDataService.getStationCount(), "Should still have two stations after loading null");

        // Test loading with null elements
        WeatherData[] stationsWithNulls = {
            createSampleWeatherData("VALID001", "Valid Station"),
            null,
            createSampleWeatherData("VALID002", "Another Valid Station")
        };
        
        weatherDataService.loadWeatherData(stationsWithNulls, System.currentTimeMillis());
        assertEquals(2, weatherDataService.getStationCount(), "Should have two valid stations");
        
        allData = weatherDataService.getAllWeatherData();
        assertEquals(2, allData.length, "Should return two stations");

        // Test loading with null ID elements
        WeatherData validStation = createSampleWeatherData("VALID001", "Valid Station");
        WeatherData invalidStation = createSampleWeatherData(null, "Invalid Station");
        
        WeatherData[] stationsWithNullIds = {validStation, invalidStation};
        weatherDataService.loadWeatherData(stationsWithNullIds, System.currentTimeMillis());
        
        assertEquals(1, weatherDataService.getStationCount(), "Should have one valid station");
        
        allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should return one station");
        assertEquals("VALID001", allData[0].getId(), "Should have valid station");
        
        System.out.println("✓ Error handling and data loading test passed");
    }

    // === CONCURRENCY TESTS ===

    @Test
    void testConcurrentOperations() throws InterruptedException {
        System.out.println("Testing concurrent operations...");
        
        // Test concurrent writes
        final int numThreads = 10;
        final int operationsPerThread = 50; // Reduced for faster execution
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(numThreads);
        final AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for start signal
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        WeatherData station = createSampleWeatherData("CONCURRENT" + threadId + "_" + j, "Station " + threadId + "_" + j);
                        weatherDataService.storeWeatherData(station);
                        successCount.incrementAndGet();
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all threads to complete
        assertTrue(completionLatch.await(10, TimeUnit.SECONDS), "All threads should complete within timeout");
        
        executor.shutdown();
        
        // Verify all operations succeeded
        assertEquals(numThreads * operationsPerThread, successCount.get(), "All operations should succeed");
        assertEquals(numThreads * operationsPerThread, weatherDataService.getStationCount(), "Should have correct number of stations");

        // Test concurrent read and write
        final int numWriters = 5;
        final int numReaders = 5;
        final int operationsPerWriter = 25; // Reduced for faster execution
        final ExecutorService readWriteExecutor = Executors.newFixedThreadPool(numWriters + numReaders);
        final CountDownLatch readWriteStartLatch = new CountDownLatch(1);
        final CountDownLatch readWriteCompletionLatch = new CountDownLatch(numWriters + numReaders);
        final AtomicInteger readCount = new AtomicInteger(0);
        final AtomicInteger writeCount = new AtomicInteger(0);
        
        // Start writers
        for (int i = 0; i < numWriters; i++) {
            final int writerId = i;
            readWriteExecutor.submit(() -> {
                try {
                    readWriteStartLatch.await();
                    
                    for (int j = 0; j < operationsPerWriter; j++) {
                        WeatherData station = createSampleWeatherData("WRITER" + writerId + "_" + j, "Writer " + writerId + "_" + j);
                        weatherDataService.storeWeatherData(station);
                        writeCount.incrementAndGet();
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    readWriteCompletionLatch.countDown();
                }
            });
        }
        
        // Start readers
        for (int i = 0; i < numReaders; i++) {
            readWriteExecutor.submit(() -> {
                try {
                    readWriteStartLatch.await();
                    
                    for (int j = 0; j < operationsPerWriter; j++) {
                        WeatherData[] data = weatherDataService.getAllWeatherData();
                        assertNotNull(data, "Data should not be null");
                        readCount.incrementAndGet();
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    readWriteCompletionLatch.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        readWriteStartLatch.countDown();
        
        // Wait for all threads to complete
        assertTrue(readWriteCompletionLatch.await(10, TimeUnit.SECONDS), "All threads should complete within timeout");
        
        readWriteExecutor.shutdown();
        
        // Verify operations succeeded
        assertEquals(numWriters * operationsPerWriter, writeCount.get(), "All write operations should succeed");
        assertEquals(numReaders * operationsPerWriter, readCount.get(), "All read operations should succeed");
        
        System.out.println("✓ Concurrent operations test passed");
    }

    // === EDGE CASES TESTS ===

    @Test
    void testDataIntegrityAndEdgeCases() {
        System.out.println("Testing data integrity and edge cases...");
        
        // Test special characters
        WeatherData specialStation = createSampleWeatherData("SPECIAL001", "Station with special chars: ñáéíóú & symbols");
        specialStation.setCloud("Partly \"cloudy\" with mixed conditions");
        specialStation.setWindDir("北"); // North in Chinese
        
        weatherDataService.storeWeatherData(specialStation);
        
        assertEquals(1, weatherDataService.getStationCount(), "Should have one station");
        
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should return one station");
        assertEquals("SPECIAL001", allData[0].getId(), "Should have correct station ID");
        assertEquals("Station with special chars: ñáéíóú & symbols", allData[0].getName(), "Should preserve special characters");
        assertEquals("Partly \"cloudy\" with mixed conditions", allData[0].getCloud(), "Should preserve quotes");
        assertEquals("北", allData[0].getWindDir(), "Should preserve Unicode characters");

        // Test numeric precision
        WeatherData precisionStation = createSampleWeatherData("PRECISION001", "Precision Station");
        precisionStation.setAirTemp(13.123456789);
        precisionStation.setLat(-34.987654321);
        precisionStation.setLon(138.123456789);
        precisionStation.setPress(1023.987654321);
        
        weatherDataService.storeWeatherData(precisionStation);
        
        allData = weatherDataService.getAllWeatherData();
        assertEquals(2, allData.length, "Should return two stations");
        
        // Find precision station
        WeatherData foundPrecision = null;
        for (WeatherData data : allData) {
            if ("PRECISION001".equals(data.getId())) {
                foundPrecision = data;
                break;
            }
        }
        assertNotNull(foundPrecision, "Should find precision station");
        assertEquals(13.123456789, foundPrecision.getAirTemp(), 0.000000001, "Should preserve temperature precision");
        assertEquals(-34.987654321, foundPrecision.getLat(), 0.000000001, "Should preserve latitude precision");
        assertEquals(138.123456789, foundPrecision.getLon(), 0.000000001, "Should preserve longitude precision");
        assertEquals(1023.987654321, foundPrecision.getPress(), 0.000000001, "Should preserve pressure precision");

        // Test zero values
        WeatherData zeroStation = createSampleWeatherData("ZERO001", "Zero Values Station");
        zeroStation.setAirTemp(0.0);
        zeroStation.setLat(0.0);
        zeroStation.setLon(0.0);
        zeroStation.setPress(0.0);
        zeroStation.setRelHum(0);
        zeroStation.setWindSpdKmh(0);
        zeroStation.setWindSpdKt(0);
        
        weatherDataService.storeWeatherData(zeroStation);
        
        allData = weatherDataService.getAllWeatherData();
        assertEquals(3, allData.length, "Should return three stations");
        
        // Find zero station
        WeatherData foundZero = null;
        for (WeatherData data : allData) {
            if ("ZERO001".equals(data.getId())) {
                foundZero = data;
                break;
            }
        }
        assertNotNull(foundZero, "Should find zero station");
        assertEquals(0.0, foundZero.getAirTemp(), 0.001, "Should preserve zero temperature");
        assertEquals(0.0, foundZero.getLat(), 0.001, "Should preserve zero latitude");
        assertEquals(0.0, foundZero.getLon(), 0.001, "Should preserve zero longitude");
        assertEquals(0.0, foundZero.getPress(), 0.001, "Should preserve zero pressure");
        assertEquals(0, foundZero.getRelHum(), "Should preserve zero humidity");
        assertEquals(0, foundZero.getWindSpdKmh(), "Should preserve zero wind speed kmh");
        assertEquals(0, foundZero.getWindSpdKt(), "Should preserve zero wind speed kt");

        // Test negative values
        WeatherData negativeStation = createSampleWeatherData("NEGATIVE001", "Negative Values Station");
        negativeStation.setAirTemp(-5.5);
        negativeStation.setLat(-90.0);
        negativeStation.setLon(-180.0);
        negativeStation.setPress(-10.0);
        
        weatherDataService.storeWeatherData(negativeStation);
        
        allData = weatherDataService.getAllWeatherData();
        assertEquals(4, allData.length, "Should return four stations");
        
        // Find negative station
        WeatherData foundNegative = null;
        for (WeatherData data : allData) {
            if ("NEGATIVE001".equals(data.getId())) {
                foundNegative = data;
                break;
            }
        }
        assertNotNull(foundNegative, "Should find negative station");
        assertEquals(-5.5, foundNegative.getAirTemp(), 0.001, "Should preserve negative temperature");
        assertEquals(-90.0, foundNegative.getLat(), 0.001, "Should preserve negative latitude");
        assertEquals(-180.0, foundNegative.getLon(), 0.001, "Should preserve negative longitude");
        assertEquals(-10.0, foundNegative.getPress(), 0.001, "Should preserve negative pressure");

        // Test large values
        WeatherData largeStation = createSampleWeatherData("LARGE001", "Large Values Station");
        largeStation.setAirTemp(999.999);
        largeStation.setLat(90.0);
        largeStation.setLon(180.0);
        largeStation.setPress(9999.999);
        largeStation.setRelHum(100);
        largeStation.setWindSpdKmh(999);
        largeStation.setWindSpdKt(999);
        
        weatherDataService.storeWeatherData(largeStation);
        
        allData = weatherDataService.getAllWeatherData();
        assertEquals(5, allData.length, "Should return five stations");
        
        // Find large station
        WeatherData foundLarge = null;
        for (WeatherData data : allData) {
            if ("LARGE001".equals(data.getId())) {
                foundLarge = data;
                break;
            }
        }
        assertNotNull(foundLarge, "Should find large station");
        assertEquals(999.999, foundLarge.getAirTemp(), 0.001, "Should preserve large temperature");
        assertEquals(90.0, foundLarge.getLat(), 0.001, "Should preserve large latitude");
        assertEquals(180.0, foundLarge.getLon(), 0.001, "Should preserve large longitude");
        assertEquals(9999.999, foundLarge.getPress(), 0.001, "Should preserve large pressure");
        assertEquals(100, foundLarge.getRelHum(), "Should preserve large humidity");
        assertEquals(999, foundLarge.getWindSpdKmh(), "Should preserve large wind speed kmh");
        assertEquals(999, foundLarge.getWindSpdKt(), "Should preserve large wind speed kt");

        // Test empty fields
        WeatherData emptyStation = createSampleWeatherData("EMPTY001", "Empty Fields Station");
        emptyStation.setCloud(""); // Empty cloud field
        emptyStation.setWindDir(""); // Empty wind direction
        emptyStation.setName(""); // Empty name (should be handled by validation)
        
        // This should still work as the validation is in the handler, not the service
        weatherDataService.storeWeatherData(emptyStation);
        
        allData = weatherDataService.getAllWeatherData();
        assertEquals(6, allData.length, "Should return six stations");
        
        // Find empty station
        WeatherData foundEmpty = null;
        for (WeatherData data : allData) {
            if ("EMPTY001".equals(data.getId())) {
                foundEmpty = data;
                break;
            }
        }
        assertNotNull(foundEmpty, "Should find empty station");
        assertEquals("EMPTY001", foundEmpty.getId(), "Should have correct station ID");
        assertEquals("", foundEmpty.getName(), "Should preserve empty name");
        assertEquals("", foundEmpty.getCloud(), "Should preserve empty cloud");
        assertEquals("", foundEmpty.getWindDir(), "Should preserve empty wind direction");

        // Test station count functionality
        assertEquals(6, weatherDataService.getStationCount(), "Should have six stations");
        
        // Test update existing station
        WeatherData updatedStation = createSampleWeatherData("SPECIAL001", "Updated Special Station");
        boolean isNew = weatherDataService.storeWeatherData(updatedStation);
        assertFalse(isNew, "Should return false for existing station update");
        assertEquals(6, weatherDataService.getStationCount(), "Should still have six stations after update");
        
        allData = weatherDataService.getAllWeatherData();
        assertEquals(6, allData.length, "Should return six stations");
        
        // Find updated station
        WeatherData foundUpdated = null;
        for (WeatherData data : allData) {
            if ("SPECIAL001".equals(data.getId())) {
                foundUpdated = data;
                break;
            }
        }
        assertNotNull(foundUpdated, "Should find updated station");
        assertEquals("Updated Special Station", foundUpdated.getName(), "Should have updated name");
        
        System.out.println("✓ Data integrity and edge cases test passed");
    }
}
