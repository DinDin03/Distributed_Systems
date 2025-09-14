package com.weathersystem.server.persistence;

import com.weathersystem.shared.domain.WeatherData;
import lombok.Getter;

@Getter
// Wraps weather data with timestamp for expiry checking
public class WeatherStationEntry {
    private final WeatherData weatherData;
    private final long lastUpdateTime;

    // Creates entry with weather data and update timestamp
    public WeatherStationEntry(WeatherData weatherData, long lastUpdateTime){
        this.weatherData = weatherData;
        this.lastUpdateTime = lastUpdateTime;
    }

    // Checks if this entry has expired based on current time and timeout
    public boolean isExpired(long currentTime, long timeOutMillis){
        return (currentTime - lastUpdateTime) > timeOutMillis;
    }
}
