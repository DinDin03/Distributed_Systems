package com.weathersystem.shared.domain;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.ToString;

// Data class representing weather station information with all meteorological fields
// Uses Lombok annotations to reduce boilerplate code for getters, setters, constructor, and toString
// Follows the JSON structure from the assignment description for weather data format
@Getter
@Setter
@NoArgsConstructor
@ToString
public class WeatherData {
    private String id;              // Unique station identifier
    private String name;            // Human readable station name
    private String state;           // State or region where station is located
    private String timeZone;        // Time zone information
    private double lat;             // Latitude coordinate
    private double lon;             // Longitude coordinate
    private String localDateTime;   // Local date and time string
    private String localDateTimeFull; // Full local date and time with timezone
    private double airTemp;         // Air temperature in Celsius
    private double apparentT;       // Apparent temperature (feels like) in Celsius
    private String cloud;           // Cloud conditions description
    private double dewpt;           // Dew point temperature in Celsius
    private double press;           // Atmospheric pressure in hPa
    private int relHum;             // Relative humidity percentage
    private String windDir;         // Wind direction 
    private int windSpdKmh;         // Wind speed in kilometers per hour
    private int windSpdKt;          // Wind speed in knots
}
