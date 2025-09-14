package com.weathersystem.server.persistence;

import com.weathersystem.shared.domain.WeatherData;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Repository that handles async saving and loading of weather data to/from files
public class WeatherDataRepository {

    private final FileStorageManager fileStorageManager;
    private final ExecutorService persistenceExecutor;

    // Sets up the repository with file storage manager and async executor
    public WeatherDataRepository(String dataFilePath, String backupFilePath) {
        this.fileStorageManager = new FileStorageManager(dataFilePath, backupFilePath);
        this.persistenceExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "WeatherDataPersistence");
            t.setDaemon(true); // Don't prevent JVM shutdown
            return t;
        });
    }

    // Saves weather data asynchronously in background thread
    public void saveAsync(WeatherData[] weatherData) {
        CompletableFuture.runAsync(() -> {
            try {
                fileStorageManager.save(weatherData);
                System.out.println("Async save completed successfully");
            } catch (IOException e) {
                System.out.println("Async save failed: " + e.getMessage());
                throw new RuntimeException("Failed to save weather data", e);
            }
        }, persistenceExecutor);
    }

    // Loads weather data synchronously from file storage
    public WeatherData[] load() {
        try {
            return fileStorageManager.load();
        } catch (IOException e) {
            System.out.println("Failed to load weather data: " + e.getMessage());
            return new WeatherData[0]; // Return empty array instead of throwing
        }
    }

    // Shuts down the async executor and stops background tasks
    public void shutdown() {
        if (persistenceExecutor != null && !persistenceExecutor.isShutdown()) {
            persistenceExecutor.shutdown();
            System.out.println("Weather data repository shutdown complete");
        }
    }
}