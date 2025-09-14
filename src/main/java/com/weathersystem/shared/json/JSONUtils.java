package com.weathersystem.shared.json;

import com.weathersystem.shared.domain.WeatherData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// Converts weather data to and from JSON
public class JSONUtils {
    // Gson instance set up for pretty printing
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

    // Generic method to convert any object to JSON string
    public static String toJSON(Object obj) {
        return gson.toJson(obj);
    }

    // Converts JSON string to single WeatherData object
    public static WeatherData fromJSON(String json) {
        return gson.fromJson(json, WeatherData.class);
    }

    // Converts JSON string to array of WeatherData objects
    public static WeatherData[] fromJSONArray(String json) {
        return gson.fromJson(json, WeatherData[].class);
    }

    // Generic method to convert JSON string to any class type
    public static <T> T fromJSONArray(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }
}