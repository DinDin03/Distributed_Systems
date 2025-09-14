package com.weathersystem.client;

import com.weathersystem.client.common.ClientConfiguration;
import com.weathersystem.client.common.HttpClientBase;
import com.weathersystem.shared.domain.WeatherData;
import com.weathersystem.shared.json.JSONUtils;

import java.io.IOException;
import java.net.Socket;

// Client that gets weather data from the server
public class GETClient extends HttpClientBase {

    // Sets up the GET client with the given config
    public GETClient(ClientConfiguration config) {
        super(config);
    }

    // Main method that gets called when you run this from the command line
    public static void main(String[] args) {
        String serverAddress = args.length > 0 ? args[0] : "localhost:4567";

        // Support multiple servers, parse server addresses and create configuration
        ClientConfiguration config;
        if (serverAddress.contains(",")) {
            config = ClientConfiguration.fromMultipleServers(serverAddress);
            System.out.println("Using multiple servers " + config.getServerAddresses());
        } else {
            config = ClientConfiguration.fromServerAddress(serverAddress);
        }

        GETClient getClient = new GETClient(config);

        System.out.println("Starting GET client");
        try {
            WeatherData[] weatherData = getClient.retrieveWeatherData();
            getClient.displayWeatherData(weatherData);

        } catch (Exception e) {
            System.out.println("Failed to get weather data: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("GET client done. Final time: " + getClient.getLamportTime());
    }

    // Gets weather data and keeps trying if it stuffs up
    public WeatherData[] retrieveWeatherData() throws Exception {
        int maxAttempts = 4;
        long retryDelayMs = 1000;
        double backoff = 2.0;

        // Retry loop with exponential backoff for failed requests
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return requestWeatherData();
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    throw new Exception("Weather data retrieval failed after " + maxAttempts + " attempts", e);
                }
                System.out.println("Get failed, trying again in " + retryDelayMs + "ms");
                Thread.sleep(retryDelayMs);
                retryDelayMs *= (long) backoff;
            }
        }
        return new WeatherData[0]; // Should never reach here
    }

    // Actually asks the server for weather data
    private WeatherData[] requestWeatherData() throws Exception {
        try (Socket socket = createConnection()) {
            sendHttpRequest(socket, "GET", null, null);
            HttpResponse response = receiveHttpResponse(socket);

            if (!response.isSuccess()) {
                throw new IOException("Server returned error: " +
                        response.getStatusCode() + " " + response.getStatusText());
            }

            return parseWeatherResponse(response);
        }
    }

    // Converts the JSON response into weather data objects
    private WeatherData[] parseWeatherResponse(HttpResponse response) throws Exception {
        String jsonContent = response.getContent();

        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            System.out.println("No weather data from server");
            return new WeatherData[0];
        }

        try {
            return JSONUtils.fromJSONArray(jsonContent);
        } catch (Exception e) {
            throw new Exception("Failed to parse weather data from server: " + e.getMessage());
        }
    }

    // Shows the weather data on screen in a nice format
    private void displayWeatherData(WeatherData[] weatherStations) {
        System.out.println("\nCURRENT WEATHER DATA");

        if (weatherStations.length == 0) {
            System.out.println("No weather stations reporting");
            return;
        }

        System.out.println("Total stations: " + weatherStations.length);
        System.out.println();

        for (int i = 0; i < weatherStations.length; i++) {
            displayStationData(weatherStations[i], i + 1);
        }
    }

    // Shows all the details for one weather station
    private void displayStationData(WeatherData station, int stationNumber) {
        System.out.println("Station " + stationNumber + ":");
        System.out.println("  ID: " + station.getId());
        System.out.println("  Name: " + station.getName());
        System.out.println("  State: " + station.getState());
        System.out.println("  Location: " + station.getLat() + "°, " + station.getLon() + "°");
        System.out.println("  Temperature: " + station.getAirTemp() + "°C");
        System.out.println("  Feels like: " + station.getApparentT() + "°C");
        System.out.println("  Conditions: " + station.getCloud());
        System.out.println("  Humidity: " + station.getRelHum() + "%");
        System.out.println("  Wind: " + station.getWindDir() + " " + station.getWindSpdKmh() + " km/h");
        System.out.println("  Pressure: " + station.getPress() + " hPa");
        System.out.println("  Time: " + station.getLocalDateTime());
        System.out.println();
    }
}
