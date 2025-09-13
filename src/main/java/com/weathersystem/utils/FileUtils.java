package com.weathersystem.utils;

import com.weathersystem.shared.domain.WeatherData;
import com.weathersystem.shared.exceptions.DataValidationException;
import com.weathersystem.shared.exceptions.PersistenceException;
import com.weathersystem.shared.validation.WeatherDataValidator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {

    public static WeatherData parseWeatherFile(String filePath) throws PersistenceException, DataValidationException {
        validateFilePath(filePath);

        WeatherData data = new WeatherData();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            parseWeatherDataFromReader(reader, data);
        } catch (IOException e) {
            throw PersistenceException.readError(filePath, e);
        }

        WeatherDataValidator.validateAndThrow(data);
        return data;
    }

    public static WeatherData parseWeatherData(String content) throws DataValidationException {
        WeatherData data = new WeatherData();

        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            parseWeatherDataFromReader(reader, data);
        } catch (IOException e) {
            throw new DataValidationException("content", "Failed to parse weather data: " + e.getMessage());
        }

        WeatherDataValidator.validateAndThrow(data);
        return data;
    }

    private static void parseWeatherDataFromReader(BufferedReader reader, WeatherData data) throws IOException {
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }

            int colonIndex = line.indexOf(':');
            if (colonIndex == -1) {
                continue;
            }

            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1).trim();

            setWeatherDataField(data, key, value);
        }
    }

    private static void setWeatherDataField(WeatherData data, String key, String value) throws IOException {
        try {
            switch (key) {
                case "id":
                    data.setId(value);
                    break;
                case "name":
                    data.setName(value);
                    break;
                case "state":
                    data.setState(value);
                    break;
                case "time_zone":
                    data.setTimeZone(value);
                    break;
                case "lat":
                    data.setLat(Double.parseDouble(value));
                    break;
                case "lon":
                    data.setLon(Double.parseDouble(value));
                    break;
                case "local_date_time":
                    data.setLocalDateTime(value);
                    break;
                case "local_date_time_full":
                    data.setLocalDateTimeFull(value);
                    break;
                case "air_temp":
                    data.setAirTemp(Double.parseDouble(value));
                    break;
                case "apparent_t":
                    data.setApparentT(Double.parseDouble(value));
                    break;
                case "cloud":
                    data.setCloud(value);
                    break;
                case "dewpt":
                    data.setDewpt(Double.parseDouble(value));
                    break;
                case "press":
                    data.setPress(Double.parseDouble(value));
                    break;
                case "rel_hum":
                    data.setRelHum(Integer.parseInt(value));
                    break;
                case "wind_dir":
                    data.setWindDir(value);
                    break;
                case "wind_spd_kmh":
                    data.setWindSpdKmh(Integer.parseInt(value));
                    break;
                case "wind_spd_kt":
                    data.setWindSpdKt(Integer.parseInt(value));
                    break;
                default:
                    System.out.println("Unknown field: " + key + " = " + value);
            }
        } catch (NumberFormatException e) {
            throw new IOException("Invalid numeric value for field '" + key + "': " + value);
        }
    }

    private static void validateFilePath(String filePath) throws PersistenceException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw PersistenceException.readError(filePath, "File path cannot be null or empty");
        }

        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw PersistenceException.readError(filePath, "File does not exist");
        }

        if (!Files.isRegularFile(path)) {
            throw PersistenceException.readError(filePath, "Path is not a regular file");
        }

        if (!Files.isReadable(path)) {
            throw PersistenceException.readError(filePath, "File is not readable");
        }
    }

    public static boolean isValidWeatherFile(String filePath) {
        try {
            validateFilePath(filePath);
            return true;
        } catch (PersistenceException e) {
            return false;
        }
    }

    public static long getFileSize(String filePath) throws PersistenceException {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            throw PersistenceException.readError(filePath, "Cannot determine file size: " + e.getMessage());
        }
    }
}