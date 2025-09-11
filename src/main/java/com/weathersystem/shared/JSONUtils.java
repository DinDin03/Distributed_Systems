package com.weathersystem.shared;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JSONUtils {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static String toJSON(WeatherData data) {
        return gson.toJson(data);
    }

    public static WeatherData fromJSON(String json) {
        return gson.fromJson(json, WeatherData.class);
    }

    public static WeatherData[] fromJSONArray(String json) {
        return gson.fromJson(json, WeatherData[].class);
    }
}