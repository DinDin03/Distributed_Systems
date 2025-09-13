package com.weathersystem.server.persistence;

import com.weathersystem.shared.domain.WeatherData;
import com.weathersystem.shared.json.JSONUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class WeatherDataPersistence {
    private static final String DATA_FILE = "data/weather.json";
    private static final String BACKUP_FILE = "data/weather.json.backup";

    public void save(WeatherData[] weatherData) throws IOException {
        String jsonData = JSONUtils.toJSON(weatherData);
        performAtomicFileWrite(jsonData);
    }

    public WeatherData[] load() throws IOException {
        File dataFile = new File(DATA_FILE);
        File backupFile = new File(BACKUP_FILE);

        // Try primary file first
        if (dataFile.exists()) {
            try {
                return loadFromFile(dataFile);
            } catch (IOException e) {
                System.out.println("Primary data file corrupted: " + e.getMessage());
            }
        }

        // Try backup file
        if (backupFile.exists()) {
            try {
                WeatherData[] data = loadFromFile(backupFile);
                // Restore primary file from backup
                save(data);
                return data;
            } catch (IOException e) {
                System.out.println("Backup file corrupted: " + e.getMessage());
            }
        }

        // Return empty array if no files exist or both are corrupted
        return new WeatherData[0];
    }

    private WeatherData[] loadFromFile(File file) throws IOException {
        String jsonContent = new String(Files.readAllBytes(file.toPath()));

        if (jsonContent.trim().isEmpty()) {
            return new WeatherData[0];
        }

        return JSONUtils.fromJSONArray(jsonContent);
    }

    private void performAtomicFileWrite(String jsonData) throws IOException {

        File tempFile = new File(DATA_FILE + ".tmp");
        File dataFile = new File(DATA_FILE);
        File backupFile = new File(BACKUP_FILE);

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