package com.weathersystem.server.services;

import com.weathersystem.server.persistence.WeatherStationEntry;
import com.weathersystem.shared.domain.WeatherData;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WeatherDataService {

    private final ConcurrentHashMap<String, WeatherStationEntry> weatherStationStore;
    private final ReentrantReadWriteLock.ReadLock readLock;
    @Getter
    private final ReentrantReadWriteLock.WriteLock writeLock;

    public WeatherDataService() {
        this.weatherStationStore = new ConcurrentHashMap<>();
        ReentrantReadWriteLock dataStoreLock = new ReentrantReadWriteLock();
        this.readLock = dataStoreLock.readLock();
        this.writeLock = dataStoreLock.writeLock();
    }

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

    public int getStationCount() {
        return weatherStationStore.size();
    }

    public ConcurrentHashMap<String, WeatherStationEntry> getInternalStorage() {
        return weatherStationStore;
    }

    public String getStatistics() {
        readLock.lock();
        try {
            int stationCount = weatherStationStore.size();
            long currentTime = System.currentTimeMillis();

            if (stationCount == 0) {
                return "No weather stations currently stored";
            }

            // Find oldest and newest data
            long oldestTime = Long.MAX_VALUE;
            long newestTime = Long.MIN_VALUE;

            for (WeatherStationEntry entry : weatherStationStore.values()) {
                long updateTime = entry.getLastUpdateTime();
                oldestTime = Math.min(oldestTime, updateTime);
                newestTime = Math.max(newestTime, updateTime);
            }

            long oldestAgeSeconds = (currentTime - oldestTime) / 1000;
            long newestAgeSeconds = (currentTime - newestTime) / 1000;

            return String.format("Weather stations: %d, Oldest data: %ds ago, Newest data: %ds ago",
                    stationCount, oldestAgeSeconds, newestAgeSeconds);

        } finally {
            readLock.unlock();
        }
    }
}