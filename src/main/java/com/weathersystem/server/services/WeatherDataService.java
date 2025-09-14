package com.weathersystem.server.services;

import com.weathersystem.server.persistence.WeatherStationEntry;
import com.weathersystem.shared.domain.WeatherData;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// Manages weather data storage with thread-safe operations
public class WeatherDataService {

    private final ConcurrentHashMap<String, WeatherStationEntry> weatherStationStore;
    private final ReentrantReadWriteLock.ReadLock readLock;
    @Getter
    private final ReentrantReadWriteLock.WriteLock writeLock;

    // Sets up the service with thread-safe storage and locks
    public WeatherDataService() {
        this.weatherStationStore = new ConcurrentHashMap<>();
        ReentrantReadWriteLock dataStoreLock = new ReentrantReadWriteLock();
        this.readLock = dataStoreLock.readLock();
        this.writeLock = dataStoreLock.writeLock();
    }

    // Stores weather data and returns true if it's a new station
    public boolean storeWeatherData(WeatherData weatherData) {
        if (weatherData == null || weatherData.getId() == null) {
            throw new IllegalArgumentException("Weather data and station ID cannot be null");
        }

        writeLock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            WeatherStationEntry entry = new WeatherStationEntry(weatherData, currentTime);

            boolean isNewStation = !weatherStationStore.containsKey(weatherData.getId());
            weatherStationStore.put(weatherData.getId(), entry);

            System.out.println("Stored weather data for station: " + weatherData.getId() +
                    " (" + (isNewStation ? "new station" : "updated existing") + ")");

            return isNewStation;

        } finally {
            writeLock.unlock();
        }
    }

    // Gets all weather data from storage using read lock
    public WeatherData[] getAllWeatherData() {
        readLock.lock();
        try {
            WeatherData[] allData = weatherStationStore.values().stream()
                    .map(WeatherStationEntry::getWeatherData)
                    .toArray(WeatherData[]::new);

            System.out.println("Retrieved weather data for " + allData.length + " active stations");
            return allData;

        } finally {
            readLock.unlock();
        }
    }

    // Removes all weather data from storage
    public void clearAllData() {
        writeLock.lock();
        try {
            int removedCount = weatherStationStore.size();
            weatherStationStore.clear();
            System.out.println("Cleared all weather data (" + removedCount + " stations)");
        } finally {
            writeLock.unlock();
        }
    }

    // Loads weather data from array into storage with given timestamp
    public void loadWeatherData(WeatherData[] weatherDataArray, long timestamp) {
        if (weatherDataArray == null) {
            return;
        }

        writeLock.lock();
        try {
            clearAllData(); // Clear existing data first

            for (WeatherData weatherData : weatherDataArray) {
                if (weatherData != null && weatherData.getId() != null) {
                    WeatherStationEntry entry = new WeatherStationEntry(weatherData, timestamp);
                    weatherStationStore.put(weatherData.getId(), entry);
                }
            }

            System.out.println("Loaded " + weatherStationStore.size() + " weather stations from storage");

        } finally {
            writeLock.unlock();
        }
    }

    // Returns how many weather stations are currently stored
    public int getStationCount() {
        return weatherStationStore.size();
    }

    // Returns the internal storage map for other services to use
    public ConcurrentHashMap<String, WeatherStationEntry> getInternalStorage() {
        return weatherStationStore;
    }

}