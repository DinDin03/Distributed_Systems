package com.weathersystem.server.persistence;

import com.weathersystem.shared.domain.WeatherData;
import com.weathersystem.shared.json.JSONUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

// Handles saving and loading weather data to/from JSON files with backup support
public class FileStorageManager {

    private final String dataFilePath;
    private final String backupFilePath;

    // Sets up the storage manager with data and backup file paths
    public FileStorageManager(String dataFilePath, String backupFilePath) {
        this.dataFilePath = dataFilePath;
        this.backupFilePath = backupFilePath;

    }

    // Saves weather data to JSON file using atomic write operation
    public void save(WeatherData[] weatherData) throws IOException {
        String jsonData = JSONUtils.toJSON(weatherData);
        performAtomicFileWrite(jsonData);
        System.out.println("Weather data saved to persistent storage (" +
                weatherData.length + " stations)");
    }

    // Loads weather data from file, tries backup if main file is corrupted
    public WeatherData[] load() throws IOException {
        File dataFile = new File(dataFilePath);
        File backupFile = new File(backupFilePath);

        if (dataFile.exists()) {
            try {
                return loadFromFile(dataFile);
            } catch (IOException e) {
                System.out.println("Primary data file corrupted: " + e.getMessage());
            }
        }

        if (backupFile.exists()) {
            try {
                WeatherData[] data = loadFromFile(backupFile);
                // Restore primary file from backup
                save(data);
                System.out.println("Data restored from backup file");
                return data;
            } catch (IOException e) {
                System.out.println("Backup file corrupted: " + e.getMessage());
            }
        }

        // Return empty array if no files exist or both are corrupted
        System.out.println("No valid data files found, starting with empty database");
        return new WeatherData[0];
    }

    // Loads weather data from a specific file and parses JSON
    private WeatherData[] loadFromFile(File file) throws IOException {
        String jsonContent = new String(Files.readAllBytes(file.toPath()));

        if (jsonContent.trim().isEmpty()) {
            return new WeatherData[0];
        }

        try {
            return JSONUtils.fromJSONArray(jsonContent);
        } catch (Exception e) {
            // Convert any JSON parsing exception to IOException for consistent error handling
            throw new IOException("Failed to parse JSON content: " + e.getMessage(), e);
        }
    }

    // Writes data atomically using temp file and backup to prevent corruption
    private void performAtomicFileWrite(String jsonData) throws IOException {
        String tempFilePath = dataFilePath + ".tmp";
        File tempFile = new File(tempFilePath);
        File dataFile = new File(dataFilePath);
        File backupFile = new File(backupFilePath);

        // Write to temporary file first
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(jsonData);
        }

        // Create backup of current file (if it exists)
        if (dataFile.exists()) {
            Files.copy(dataFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // Atomically move temp file to final location
        Files.move(tempFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

}