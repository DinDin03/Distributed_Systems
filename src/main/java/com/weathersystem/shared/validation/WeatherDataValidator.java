package com.weathersystem.shared.validation;

import com.weathersystem.shared.config.SystemConfiguration;
import com.weathersystem.shared.domain.WeatherData;
import com.weathersystem.shared.exceptions.DataValidationException;

import java.util.ArrayList;
import java.util.List;

public class WeatherDataValidator {

    public static ValidationResult validate(WeatherData data) {
        if (data == null) {
            return ValidationResult.failure("Weather data cannot be null");
        }

        List<String> errors = new ArrayList<>();

        validateId(data.getId(), errors);
        validateName(data.getName(), errors);
        validateState(data.getState(), errors);
        validateCoordinates(data.getLat(), data.getLon(), errors);
        validateTemperature(data.getAirTemp(), "air_temp", errors);
        validateTemperature(data.getApparentT(), "apparent_t", errors);
        validateHumidity(data.getRelHum(), errors);
        validatePressure(data.getPress(), errors);
        validateWindSpeed(data.getWindSpdKmh(), errors);

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    public static void validateAndThrow(WeatherData data) throws DataValidationException {
        ValidationResult result = validate(data);
        if (!result.isValid()) {
            throw new DataValidationException(result.getErrors());
        }
    }

    private static void validateId(String id, List<String> errors) {
        if (id == null || id.trim().isEmpty()) {
            errors.add("Station ID is required");
            return;
        }

        if (id.length() > SystemConfiguration.MAX_STATION_ID_LENGTH) {
            errors.add("Station ID too long (max " + SystemConfiguration.MAX_STATION_ID_LENGTH + " characters)");
        }

        if (!id.matches("^[A-Za-z0-9_-]+$")) {
            errors.add("Station ID contains invalid characters (only letters, numbers, underscore, and dash allowed)");
        }
    }

    private static void validateName(String name, List<String> errors) {
        if (name == null || name.trim().isEmpty()) {
            errors.add("Station name is required");
            return;
        }

        if (name.length() > SystemConfiguration.MAX_STATION_NAME_LENGTH) {
            errors.add("Station name too long (max " + SystemConfiguration.MAX_STATION_NAME_LENGTH + " characters)");
        }
    }

    private static void validateState(String state, List<String> errors) {
        if (state != null && state.length() > 10) {
            errors.add("State code too long (max 10 characters)");
        }
    }

    private static void validateCoordinates(double lat, double lon, List<String> errors) {
        if (lat < SystemConfiguration.MIN_LATITUDE || lat > SystemConfiguration.MAX_LATITUDE) {
            errors.add("Invalid latitude: " + lat + " (must be between " +
                    SystemConfiguration.MIN_LATITUDE + " and " + SystemConfiguration.MAX_LATITUDE + ")");
        }

        if (lon < SystemConfiguration.MIN_LONGITUDE || lon > SystemConfiguration.MAX_LONGITUDE) {
            errors.add("Invalid longitude: " + lon + " (must be between " +
                    SystemConfiguration.MIN_LONGITUDE + " and " + SystemConfiguration.MAX_LONGITUDE + ")");
        }
    }

    private static void validateTemperature(double temperature, String fieldName, List<String> errors) {
        if (Double.isNaN(temperature) || Double.isInfinite(temperature)) {
            errors.add("Invalid temperature for " + fieldName);
            return;
        }

        if (temperature < -100.0 || temperature > 70.0) {
            errors.add("Temperature " + fieldName + " out of reasonable range: " + temperature + "°C");
        }
    }

    private static void validateHumidity(int humidity, List<String> errors) {
        if (humidity < 0 || humidity > 100) {
            errors.add("Invalid humidity: " + humidity + "% (must be between 0 and 100)");
        }
    }

    private static void validatePressure(double pressure, List<String> errors) {
        if (Double.isNaN(pressure) || Double.isInfinite(pressure)) {
            errors.add("Invalid pressure value");
            return;
        }

        if (pressure < 800.0 || pressure > 1200.0) {
            errors.add("Pressure out of reasonable range: " + pressure + " hPa (expected 800-1200)");
        }
    }

    private static void validateWindSpeed(int windSpeed, List<String> errors) {
        if (windSpeed < 0) {
            errors.add("Wind speed cannot be negative: " + windSpeed);
        }

        if (windSpeed > 300) {
            errors.add("Wind speed seems unrealistic: " + windSpeed + " km/h");
        }
    }

    public static boolean isValidStationId(String id) {
        return id != null &&
                !id.trim().isEmpty() &&
                id.length() <= SystemConfiguration.MAX_STATION_ID_LENGTH &&
                id.matches("^[A-Za-z0-9_-]+$");
    }

    public static boolean isValidCoordinate(double lat, double lon) {
        return lat >= SystemConfiguration.MIN_LATITUDE && lat <= SystemConfiguration.MAX_LATITUDE &&
                lon >= SystemConfiguration.MIN_LONGITUDE && lon <= SystemConfiguration.MAX_LONGITUDE;
    }
}