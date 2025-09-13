package com.weathersystem.shared.domain;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class WeatherData {
    private String id;
    private String name;
    private String state;
    private String timeZone;
    private double lat;
    private double lon;
    private String localDateTime;
    private String localDateTimeFull;
    private double airTemp;
    private double apparentT;
    private String cloud;
    private double dewpt;
    private double press;
    private int relHum;
    private String windDir;
    private int windSpdKmh;
    private int windSpdKt;
}
