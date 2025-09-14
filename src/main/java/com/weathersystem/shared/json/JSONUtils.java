package com.weathersystem.shared.json;

import com.weathersystem.shared.domain.WeatherData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// Utility class for JSON serialization and deserialization of weather data
public class JSONUtils {
    // Gson instance configured with pretty printing for readable JSON output
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    // Converts single WeatherData object to JSON string
    public static String toJSON(WeatherData data) {
        return gson.toJson(data);
    }

    // Converts array of WeatherData objects to JSON string
    public static String toJSON(WeatherData[] dataArray) {
        return gson.toJson(dataArray);
    }

    // Converts JSON string to single WeatherData object
    public static WeatherData fromJSON(String json) {
        return gson.fromJson(json, WeatherData.class);
    }

    // Converts JSON string to array of WeatherData objects
    public static WeatherData[] fromJSONArray(String json) {
        return gson.fromJson(json, WeatherData[].class);
    }
}