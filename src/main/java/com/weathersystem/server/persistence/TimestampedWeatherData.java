package com.weathersystem.server.persistence;

import com.weathersystem.shared.domain.WeatherData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor  // Required for JSON deserialization
@AllArgsConstructor
// Weather data with timestamp for persistence - includes expiry information
public class TimestampedWeatherData {
    private WeatherData weatherData;
    private long lastUpdateTime;

    // Checks if this entry has expired based on current time and timeout
    public boolean isExpired(long currentTime, long timeOutMillis) {
        return (currentTime - lastUpdateTime) > timeOutMillis;
    }

    // Creates from WeatherStationEntry
    public static TimestampedWeatherData from(WeatherStationEntry entry) {
        return new TimestampedWeatherData(entry.getWeatherData(), entry.getLastUpdateTime());
    }

    // Converts to WeatherStationEntry
    public WeatherStationEntry toWeatherStationEntry() {
        return new WeatherStationEntry(weatherData, lastUpdateTime);
    }
}