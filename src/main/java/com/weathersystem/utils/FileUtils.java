package com.weathersystem.utils;

import com.weathersystem.shared.domain.WeatherData;

import java.io.*;

public class FileUtils {
    public static WeatherData parseWeatherFile(String filePath) throws IOException{
        WeatherData data = new WeatherData();

        try(BufferedReader reader = new BufferedReader(new FileReader (filePath))){
            String line;

            while((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                int colonIndex = line.indexOf(':');
                if (colonIndex == -1) {
                    continue;
                }
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();

                switch (key) {
                    case "id":
                        data.setId(value);
                        break;
                    case "name":
                        data.setName(value);
                        break;
                    case "state":
                        data.setState(value);
                        break;
                    case "time_zone":
                        data.setTimeZone(value);
                        break;
                    case "lat":
                        data.setLat(Double.parseDouble(value));
                        break;
                    case "lon":
                        data.setLon(Double.parseDouble(value));
                        break;
                    case "local_date_time":
                        data.setLocalDateTime(value);
                        break;
                    case "local_date_time_full":
                        data.setLocalDateTimeFull(value);
                        break;
                    case "air_temp":
                        data.setAirTemp(Double.parseDouble(value));
                        break;
                    case "apparent_t":
                        data.setApparentT(Double.parseDouble(value));
                        break;
                    case "cloud":
                        data.setCloud(value);
                        break;
                    case "dewpt":
                        data.setDewpt(Double.parseDouble(value));
                        break;
                    case "press":
                        data.setPress(Double.parseDouble(value));
                        break;
                    case "rel_hum":
                        data.setRelHum(Integer.parseInt(value));
                        break;
                    case "wind_dir":
                        data.setWindDir(value);
                        break;
                    case "wind_spd_kmh":
                        data.setWindSpdKmh(Integer.parseInt(value));
                        break;
                    case "wind_spd_kt":
                        data.setWindSpdKt(Integer.parseInt(value));
                        break;
                    default:
                        System.out.println("Unknown field: " + key + " = " + value);
                }
            }
        }
        if (data.getId() == null || data.getId().isEmpty()) {
            throw new IOException("Weather file missing required 'id' field");
        }

        return data;
    }
}
