package com.weathersystem.server.services;

import com.weathersystem.server.persistence.WeatherStationEntry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// Background service that automatically removes expired weather data based on timestamps
public class DataExpiryService {

    private final long expiryTimeMs;
    private final long cleanupIntervalMs;
    private final ConcurrentHashMap<String, WeatherStationEntry> dataStore;
    private final ReentrantReadWriteLock.WriteLock writeLock;
    private final ScheduledExecutorService cleanupService;
    private final Runnable onDataExpired; // Callback for when data is removed

    // Constructor that initializes the expiry service with timing parameters and data store
    public DataExpiryService(long expiryTimeMs, long cleanupIntervalMs,
                             ConcurrentHashMap<String, WeatherStationEntry> dataStore,
                             ReentrantReadWriteLock.WriteLock writeLock,
                             Runnable onDataExpired) {
        this.expiryTimeMs = expiryTimeMs;
        this.cleanupIntervalMs = cleanupIntervalMs;
        this.dataStore = dataStore;
        this.writeLock = writeLock;
        this.onDataExpired = onDataExpired;
        this.cleanupService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DataExpiryService");
            t.setDaemon(true); // Don't prevent JVM shutdown
            return t;
        });
    }

    // Starts the background cleanup service with scheduled periodic execution
    public void start() {
        cleanupService.scheduleAtFixedRate(
                this::removeExpiredData,
                cleanupIntervalMs,
                cleanupIntervalMs,
                TimeUnit.MILLISECONDS
        );
        System.out.println("Started background cleanup service (checking every " +
                cleanupIntervalMs / 1000 + " seconds)");
    }

    // Stops the background cleanup service and waits for graceful shutdown
    public void stop() {
        if (cleanupService != null && !cleanupService.isShutdown()) {
            cleanupService.shutdown();
            try {
                if (!cleanupService.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupService.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("Data expiry service stopped");
        }
    }

    // Removes expired weather station data from the data store using thread-safe operations
    public void removeExpiredData() {
        writeLock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            int initialSize = dataStore.size();

            // Remove expired entries from the data store using atomic operation
            dataStore.entrySet().removeIf(entry -> {
                WeatherStationEntry stationEntry = entry.getValue();
                boolean expired = stationEntry.isExpired(currentTime, expiryTimeMs);

                if (expired) {
                    System.out.println("Removing expired weather station: " + entry.getKey() +
                            " (last update: " +
                            (currentTime - stationEntry.getLastUpdateTime()) / 1000 + "s ago)");
                }
                return expired;
            });

            int removedCount = initialSize - dataStore.size();
            if (removedCount > 0) {
                System.out.println("Removed " + removedCount + " expired stations, " +
                        dataStore.size() + " active stations remaining");

                // Notify that data has changed (typically triggers file save)
                if (onDataExpired != null) {
                    onDataExpired.run();
                }
            }

        } finally {
            writeLock.unlock();
        }
    }
}