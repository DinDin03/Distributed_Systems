package com.weathersystem.utils;

import com.weathersystem.shared.domain.WeatherData;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Parses weather data from text files with key:value format
public class FileUtils {

    // Parses weather data from a text file with key:value format
    public static WeatherData parseWeatherFile(String filePath) throws IOException {
        validateFilePath(filePath);

        WeatherData data = new WeatherData();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            parseWeatherDataFromReader(reader, data);
        }
        return data;
    }

    // Reads weather data from BufferedReader and fills the WeatherData object
    private static void parseWeatherDataFromReader(BufferedReader reader, WeatherData data) throws IOException {
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            int colonIndex = line.indexOf(':');
            if (colonIndex == -1) continue;

            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1).trim();

            setWeatherDataField(data, key, value);
        }
    }

    // Sets the right field in WeatherData object based on the key-value pair
    private static void setWeatherDataField(WeatherData data, String key, String value) throws IOException {
        try {
            switch (key) {
                case "id" -> data.setId(value);
                case "name" -> data.setName(value);
                case "state" -> data.setState(value);
                case "time_zone" -> data.setTimeZone(value);
                case "lat" -> data.setLat(Double.parseDouble(value));
                case "lon" -> data.setLon(Double.parseDouble(value));
                case "local_date_time" -> data.setLocalDateTime(value);
                case "local_date_time_full" -> data.setLocalDateTimeFull(value);
                case "air_temp" -> data.setAirTemp(Double.parseDouble(value));
                case "apparent_t" -> data.setApparentT(Double.parseDouble(value));
                case "cloud" -> data.setCloud(value);
                case "dewpt" -> data.setDewpt(Double.parseDouble(value));
                case "press" -> data.setPress(Double.parseDouble(value));
                case "rel_hum" -> data.setRelHum(Integer.parseInt(value));
                case "wind_dir" -> data.setWindDir(value);
                case "wind_spd_kmh" -> data.setWindSpdKmh(Integer.parseInt(value));
                case "wind_spd_kt" -> data.setWindSpdKt(Integer.parseInt(value));
                default -> System.out.println("Unknown field: " + key + " = " + value);
            }
        } catch (NumberFormatException e) {
            throw new IOException("Invalid numeric value for field '" + key + "': " + value);
        }
    }

    // Checks that the file path is valid and the file exists and can be read
    private static void validateFilePath(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        Path path = Paths.get(filePath);

        if (!Files.exists(path)) throw new IOException("File does not exist: " + filePath);
        if (!Files.isRegularFile(path)) throw new IOException("Path is not a regular file: " + filePath);
        if (!Files.isReadable(path)) throw new IOException("File is not readable: " + filePath);
    }
}
