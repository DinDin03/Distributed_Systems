package com.weathersystem.shared.domain;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.ToString;

// Holds all the weather station data
// Uses Lombok to avoid writing heaps of getter and setter methods
// Follows the JSON structure from the assignment for weather data format
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
