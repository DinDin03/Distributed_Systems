package com.weathersystem.server.persistence;

import com.weathersystem.shared.domain.WeatherData;
import lombok.Getter;

@Getter
public class WeatherStationEntry {
    private final WeatherData weatherData;
    private final long lastUpdateTime;

    public WeatherStationEntry(WeatherData weatherData, long lastUpdateTime){
        this.weatherData = weatherData;
        this.lastUpdateTime = lastUpdateTime;
    }

    public boolean isExpired(long currentTime, long timeOutMillis){
        return (currentTime - lastUpdateTime) > timeOutMillis;
    }
}
