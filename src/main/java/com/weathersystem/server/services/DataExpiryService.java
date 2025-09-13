package com.weathersystem.server.services;

import com.weathersystem.server.persistence.WeatherStationEntry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataExpiryService {

    private final long expiryTimeMs;
    private final long cleanupIntervalMs;
    private final ConcurrentHashMap<String, WeatherStationEntry> dataStore;
    private final ReentrantReadWriteLock.WriteLock writeLock;
    private final ScheduledExecutorService cleanupService;
    private final Runnable onDataExpired; // Callback for when data is removed

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

    public void removeExpiredData() {
        writeLock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            int initialSize = dataStore.size();

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