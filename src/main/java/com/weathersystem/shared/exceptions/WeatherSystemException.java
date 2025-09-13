package com.weathersystem.shared.exceptions;

public class WeatherSystemException extends Exception {

    private final String errorCode;
    private final long timestamp;

    public WeatherSystemException(String message) {
        super(message);
        this.errorCode = "WEATHER_SYSTEM_ERROR";
        this.timestamp = System.currentTimeMillis();
    }

    public WeatherSystemException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "WEATHER_SYSTEM_ERROR";
        this.timestamp = System.currentTimeMillis();
    }

    public WeatherSystemException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = System.currentTimeMillis();
    }

    public WeatherSystemException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.timestamp = System.currentTimeMillis();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedMessage() {
        return String.format("[%s] %s (timestamp: %d)", errorCode, getMessage(), timestamp);
    }

    @Override
    public String toString() {
        return getFormattedMessage() + (getCause() != null ? " Caused by: " + getCause() : "");
    }
}