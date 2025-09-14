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

    @Test
    void testStoreWeatherDataNewStation() {
        WeatherData station = createSampleWeatherData("NEW001", "New Station");
        
        boolean isNew = weatherDataService.storeWeatherData(station);
        
        assertTrue(isNew, "Should return true for new station");
        assertEquals(1, weatherDataService.getStationCount(), "Should have one station");
        
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should return one station");
        assertEquals("NEW001", allData[0].getId(), "Should have correct station ID");
        assertEquals("New Station", allData[0].getName(), "Should have correct station name");
    }

    @Test
    void testStoreWeatherDataExistingStation() {
        WeatherData station1 = createSampleWeatherData("EXIST001", "Original Name");
        WeatherData station2 = createSampleWeatherData("EXIST001", "Updated Name");
        
        boolean isNew1 = weatherDataService.storeWeatherData(station1);
        boolean isNew2 = weatherDataService.storeWeatherData(station2);
        
        assertTrue(isNew1, "First store should return true");
        assertFalse(isNew2, "Second store should return false");
        assertEquals(1, weatherDataService.getStationCount(), "Should still have one station");
        
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should return one station");
        assertEquals("EXIST001", allData[0].getId(), "Should have correct station ID");
        assertEquals("Updated Name", allData[0].getName(), "Should have updated name");
    }

    @Test
    void testStoreWeatherDataMultipleStations() {
        WeatherData station1 = createSampleWeatherData("MULTI001", "Station 1");
        WeatherData station2 = createSampleWeatherData("MULTI002", "Station 2");
        WeatherData station3 = createSampleWeatherData("MULTI003", "Station 3");
        
        weatherDataService.storeWeatherData(station1);
        weatherDataService.storeWeatherData(station2);
        weatherDataService.storeWeatherData(station3);
        
        assertEquals(3, weatherDataService.getStationCount(), "Should have three stations");
        
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(3, allData.length, "Should return three stations");
        
        // Verify all stations are present
        boolean found1 = false, found2 = false, found3 = false;
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
            }
        }
        
        assertTrue(found1, "Should find station 1");
        assertTrue(found2, "Should find station 2");
        assertTrue(found3, "Should find station 3");
    }

    @Test
    void testStoreWeatherDataWithNullData() {
        assertThrows(IllegalArgumentException.class, () -> {
            weatherDataService.storeWeatherData(null);
        }, "Should throw exception for null data");
    }

    @Test
    void testStoreWeatherDataWithNullId() {
        WeatherData station = createSampleWeatherData(null, "Test Station");
        
        assertThrows(IllegalArgumentException.class, () -> {
            weatherDataService.storeWeatherData(station);
        }, "Should throw exception for null ID");
    }

    @Test
    void testGetAllWeatherDataEmpty() {
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        
        assertNotNull(allData, "Should return non-null array");
        assertEquals(0, allData.length, "Should return empty array");
        assertEquals(0, weatherDataService.getStationCount(), "Should have zero stations");
    }

    @Test
    void testGetAllWeatherDataAfterClear() {
        WeatherData station = createSampleWeatherData("CLEAR001", "Clear Test");
        weatherDataService.storeWeatherData(station);
        
        assertEquals(1, weatherDataService.getStationCount(), "Should have one station");
        
        weatherDataService.clearAllData();
        
        assertEquals(0, weatherDataService.getStationCount(), "Should have zero stations after clear");
        
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(0, allData.length, "Should return empty array after clear");
    }

    @Test
    void testLoadWeatherData() {
        WeatherData[] stations = {
            createSampleWeatherData("LOAD001", "Load Station 1"),
            createSampleWeatherData("LOAD002", "Load Station 2")
        };
        
        long timestamp = System.currentTimeMillis();
        weatherDataService.loadWeatherData(stations, timestamp);
        
        assertEquals(2, weatherDataService.getStationCount(), "Should have two stations after load");
        
        WeatherData[] allData = weatherDataService.getAllWeatherData();
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
    }

    @Test
    void testLoadWeatherDataWithNull() {
        weatherDataService.loadWeatherData(null, System.currentTimeMillis());
        
        assertEquals(0, weatherDataService.getStationCount(), "Should have zero stations after loading null");
    }

    @Test
    void testLoadWeatherDataClearsExisting() {
        // Add initial data
        WeatherData initialStation = createSampleWeatherData("INITIAL001", "Initial Station");
        weatherDataService.storeWeatherData(initialStation);
        
        assertEquals(1, weatherDataService.getStationCount(), "Should have one station initially");
        
        // Load new data
        WeatherData[] newStations = {
            createSampleWeatherData("NEW001", "New Station 1"),
            createSampleWeatherData("NEW002", "New Station 2")
        };
        
        weatherDataService.loadWeatherData(newStations, System.currentTimeMillis());
        
        assertEquals(2, weatherDataService.getStationCount(), "Should have two stations after load");
        
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(2, allData.length, "Should return two stations");
        
        // Verify old data is gone and new data is present
        boolean foundInitial = false;
        boolean foundNew1 = false, foundNew2 = false;
        
        for (WeatherData data : allData) {
            if ("INITIAL001".equals(data.getId())) {
                foundInitial = true;
            } else if ("NEW001".equals(data.getId())) {
                foundNew1 = true;
            } else if ("NEW002".equals(data.getId())) {
                foundNew2 = true;
            }
        }
        
        assertFalse(foundInitial, "Should not find initial station");
        assertTrue(foundNew1, "Should find new station 1");
        assertTrue(foundNew2, "Should find new station 2");
    }

    @Test
    void testLoadWeatherDataWithNullElements() {
        WeatherData[] stations = {
            createSampleWeatherData("VALID001", "Valid Station"),
            null,
            createSampleWeatherData("VALID002", "Another Valid Station")
        };
        
        weatherDataService.loadWeatherData(stations, System.currentTimeMillis());
        
        assertEquals(2, weatherDataService.getStationCount(), "Should have two valid stations");
        
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(2, allData.length, "Should return two stations");
    }

    @Test
    void testLoadWeatherDataWithNullIdElements() {
        WeatherData validStation = createSampleWeatherData("VALID001", "Valid Station");
        WeatherData invalidStation = createSampleWeatherData(null, "Invalid Station");
        
        WeatherData[] stations = {validStation, invalidStation};
        
        weatherDataService.loadWeatherData(stations, System.currentTimeMillis());
        
        assertEquals(1, weatherDataService.getStationCount(), "Should have one valid station");
        
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should return one station");
        assertEquals("VALID001", allData[0].getId(), "Should have valid station");
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        final int numThreads = 10;
        final int operationsPerThread = 100;
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
    }

    @Test
    void testConcurrentReadAndWrite() throws InterruptedException {
        final int numWriters = 5;
        final int numReaders = 5;
        final int operationsPerWriter = 50;
        final ExecutorService executor = Executors.newFixedThreadPool(numWriters + numReaders);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(numWriters + numReaders);
        final AtomicInteger readCount = new AtomicInteger(0);
        final AtomicInteger writeCount = new AtomicInteger(0);
        
        // Start writers
        for (int i = 0; i < numWriters; i++) {
            final int writerId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < operationsPerWriter; j++) {
                        WeatherData station = createSampleWeatherData("WRITER" + writerId + "_" + j, "Writer " + writerId + "_" + j);
                        weatherDataService.storeWeatherData(station);
                        writeCount.incrementAndGet();
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        // Start readers
        for (int i = 0; i < numReaders; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < operationsPerWriter; j++) {
                        WeatherData[] data = weatherDataService.getAllWeatherData();
                        assertNotNull(data, "Data should not be null");
                        readCount.incrementAndGet();
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
        
        // Verify operations succeeded
        assertEquals(numWriters * operationsPerWriter, writeCount.get(), "All write operations should succeed");
        assertEquals(numReaders * operationsPerWriter, readCount.get(), "All read operations should succeed");
    }

    @Test
    void testStoreWeatherDataWithSpecialCharacters() {
        WeatherData station = createSampleWeatherData("SPECIAL001", "Station with special chars: ñáéíóú & symbols");
        station.setCloud("Partly \"cloudy\" with mixed conditions");
        station.setWindDir("北"); // North in Chinese
        
        weatherDataService.storeWeatherData(station);
        
        assertEquals(1, weatherDataService.getStationCount(), "Should have one station");
        
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should return one station");
        assertEquals("SPECIAL001", allData[0].getId(), "Should have correct station ID");
        assertEquals("Station with special chars: ñáéíóú & symbols", allData[0].getName(), "Should preserve special characters");
        assertEquals("Partly \"cloudy\" with mixed conditions", allData[0].getCloud(), "Should preserve quotes");
        assertEquals("北", allData[0].getWindDir(), "Should preserve Unicode characters");
    }

    @Test
    void testStoreWeatherDataWithNumericPrecision() {
        WeatherData station = createSampleWeatherData("PRECISION001", "Precision Station");
        station.setAirTemp(13.123456789);
        station.setLat(-34.987654321);
        station.setLon(138.123456789);
        station.setPress(1023.987654321);
        
        weatherDataService.storeWeatherData(station);
        
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should return one station");
        assertEquals(13.123456789, allData[0].getAirTemp(), 0.000000001, "Should preserve temperature precision");
        assertEquals(-34.987654321, allData[0].getLat(), 0.000000001, "Should preserve latitude precision");
        assertEquals(138.123456789, allData[0].getLon(), 0.000000001, "Should preserve longitude precision");
        assertEquals(1023.987654321, allData[0].getPress(), 0.000000001, "Should preserve pressure precision");
    }

    @Test
    void testStoreWeatherDataWithZeroValues() {
        WeatherData station = createSampleWeatherData("ZERO001", "Zero Values Station");
        station.setAirTemp(0.0);
        station.setLat(0.0);
        station.setLon(0.0);
        station.setPress(0.0);
        station.setRelHum(0);
        station.setWindSpdKmh(0);
        station.setWindSpdKt(0);
        
        weatherDataService.storeWeatherData(station);
        
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should return one station");
        assertEquals(0.0, allData[0].getAirTemp(), 0.001, "Should preserve zero temperature");
        assertEquals(0.0, allData[0].getLat(), 0.001, "Should preserve zero latitude");
        assertEquals(0.0, allData[0].getLon(), 0.001, "Should preserve zero longitude");
        assertEquals(0.0, allData[0].getPress(), 0.001, "Should preserve zero pressure");
        assertEquals(0, allData[0].getRelHum(), "Should preserve zero humidity");
        assertEquals(0, allData[0].getWindSpdKmh(), "Should preserve zero wind speed kmh");
        assertEquals(0, allData[0].getWindSpdKt(), "Should preserve zero wind speed kt");
    }

    @Test
    void testStoreWeatherDataWithNegativeValues() {
        WeatherData station = createSampleWeatherData("NEGATIVE001", "Negative Values Station");
        station.setAirTemp(-5.5);
        station.setLat(-90.0);
        station.setLon(-180.0);
        station.setPress(-10.0);
        
        weatherDataService.storeWeatherData(station);
        
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should return one station");
        assertEquals(-5.5, allData[0].getAirTemp(), 0.001, "Should preserve negative temperature");
        assertEquals(-90.0, allData[0].getLat(), 0.001, "Should preserve negative latitude");
        assertEquals(-180.0, allData[0].getLon(), 0.001, "Should preserve negative longitude");
        assertEquals(-10.0, allData[0].getPress(), 0.001, "Should preserve negative pressure");
    }

    @Test
    void testStoreWeatherDataWithLargeValues() {
        WeatherData station = createSampleWeatherData("LARGE001", "Large Values Station");
        station.setAirTemp(999.999);
        station.setLat(90.0);
        station.setLon(180.0);
        station.setPress(9999.999);
        station.setRelHum(100);
        station.setWindSpdKmh(999);
        station.setWindSpdKt(999);
        
        weatherDataService.storeWeatherData(station);
        
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should return one station");
        assertEquals(999.999, allData[0].getAirTemp(), 0.001, "Should preserve large temperature");
        assertEquals(90.0, allData[0].getLat(), 0.001, "Should preserve large latitude");
        assertEquals(180.0, allData[0].getLon(), 0.001, "Should preserve large longitude");
        assertEquals(9999.999, allData[0].getPress(), 0.001, "Should preserve large pressure");
        assertEquals(100, allData[0].getRelHum(), "Should preserve large humidity");
        assertEquals(999, allData[0].getWindSpdKmh(), "Should preserve large wind speed kmh");
        assertEquals(999, allData[0].getWindSpdKt(), "Should preserve large wind speed kt");
    }

    @Test
    void testStoreWeatherDataWithEmptyFields() {
        WeatherData station = createSampleWeatherData("EMPTY001", "Empty Fields Station");
        station.setCloud(""); // Empty cloud field
        station.setWindDir(""); // Empty wind direction
        station.setName(""); // Empty name (should be handled by validation)
        
        // This should still work as the validation is in the handler, not the service
        weatherDataService.storeWeatherData(station);
        
        WeatherData[] allData = weatherDataService.getAllWeatherData();
        assertEquals(1, allData.length, "Should return one station");
        assertEquals("EMPTY001", allData[0].getId(), "Should have correct station ID");
        assertEquals("", allData[0].getName(), "Should preserve empty name");
        assertEquals("", allData[0].getCloud(), "Should preserve empty cloud");
        assertEquals("", allData[0].getWindDir(), "Should preserve empty wind direction");
    }

    @Test
    void testGetStationCount() {
        assertEquals(0, weatherDataService.getStationCount(), "Should start with zero stations");
        
        WeatherData station1 = createSampleWeatherData("COUNT001", "Count Station 1");
        weatherDataService.storeWeatherData(station1);
        assertEquals(1, weatherDataService.getStationCount(), "Should have one station");
        
        WeatherData station2 = createSampleWeatherData("COUNT002", "Count Station 2");
        weatherDataService.storeWeatherData(station2);
        assertEquals(2, weatherDataService.getStationCount(), "Should have two stations");
        
        WeatherData station3 = createSampleWeatherData("COUNT001", "Updated Count Station 1");
        weatherDataService.storeWeatherData(station3);
        assertEquals(2, weatherDataService.getStationCount(), "Should still have two stations after update");
        
        weatherDataService.clearAllData();
        assertEquals(0, weatherDataService.getStationCount(), "Should have zero stations after clear");
    }
}
