package com.weathersystem.client;

import com.weathersystem.client.common.ClientConfiguration;
import com.weathersystem.client.common.HttpClientBase;
import com.weathersystem.shared.domain.WeatherData;
import com.weathersystem.shared.json.JSONUtils;

import java.io.IOException;
import java.net.Socket;

// Client for retrieving weather data from the aggregation server via HTTP GET requests
public class GETClient extends HttpClientBase {

    // Constructor that initialises the GET client with configuration
    public GETClient(ClientConfiguration config) {
        super(config);
    }

    // Main entry point for GET client command-line interface
    public static void main(String[] args) {
        String serverAddress = args.length > 0 ? args[0] : "localhost:4567";

        // Support multiple servers, parse server addresses and create configuration
        ClientConfiguration config;
        if (serverAddress.contains(",")) {
            config = ClientConfiguration.fromMultipleServers(serverAddress);
            System.out.println("Configured with multiple servers " + config.getServerAddresses());
        } else {
            config = ClientConfiguration.fromServerAddress(serverAddress);
        }

        GETClient getClient = new GETClient(config);

        System.out.println("GET Client starting");
        try {
            WeatherData[] weatherData = getClient.retrieveWeatherData();
            getClient.displayWeatherData(weatherData);

        } catch (Exception e) {
            System.out.println("Failed to retrieve weather data: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("GET Client finished. Final Lamport clock: " + getClient.getLamportTime());
    }

    // Retrieves weather data with retry logic and exponential backoff
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
                System.out.println("Weather data retrieval failed. Retrying in " + retryDelayMs + "ms");
                Thread.sleep(retryDelayMs);
                retryDelayMs *= (long) backoff;
            }
        }
        return new WeatherData[0]; // Should never reach here
    }

    // Makes HTTP GET request to retrieve weather data from server
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

    // Parses JSON response from server into WeatherData array
    private WeatherData[] parseWeatherResponse(HttpResponse response) throws Exception {
        String jsonContent = response.getContent();

        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            System.out.println("No weather data available from server");
            return new WeatherData[0];
        }

        try {
            return JSONUtils.fromJSONArray(jsonContent);
        } catch (Exception e) {
            throw new Exception("Failed to parse weather data from server: " + e.getMessage());
        }
    }

    // Displays weather data in a formatted, human-readable format
    private void displayWeatherData(WeatherData[] weatherStations) {
        System.out.println("\n=== CURRENT WEATHER DATA ===");

        if (weatherStations.length == 0) {
            System.out.println("No weather stations currently reporting data");
            return;
        }

        System.out.println("Total weather stations: " + weatherStations.length);
        System.out.println();

        for (int i = 0; i < weatherStations.length; i++) {
            displayStationData(weatherStations[i], i + 1);
        }
    }

    // Displays individual weather station data with all available fields
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
