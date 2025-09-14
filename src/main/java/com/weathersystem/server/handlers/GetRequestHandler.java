package com.weathersystem.server.handlers;

import com.weathersystem.server.http.HttpRequest;
import com.weathersystem.server.http.HttpResponseBuilder;
import com.weathersystem.server.http.HttpStatusCodes;
import com.weathersystem.server.services.WeatherDataService;
import com.weathersystem.shared.domain.WeatherData;
import com.weathersystem.shared.json.JSONUtils;

import java.io.BufferedReader;
import java.io.PrintWriter;

// Handler for processing HTTP GET requests to retrieve weather data
public class GetRequestHandler implements RequestHandler {

    private final WeatherDataService weatherDataService;
    private final HttpResponseBuilder responseBuilder;

    // Constructor that initializes the handler with weather data service
    public GetRequestHandler(WeatherDataService weatherDataService) {
        this.weatherDataService = weatherDataService;
        this.responseBuilder = new HttpResponseBuilder();
    }

    // Handles GET requests to retrieve and return all weather data as JSON
    @Override
    public void handle(HttpRequest httpRequest, BufferedReader inputReader,
                       PrintWriter outputWriter, long lamportTime) throws Exception {

        try {
            // Retrieve all active weather data
            WeatherData[] allData = weatherDataService.getAllWeatherData();

            // Convert to JSON format
            String jsonResponse = JSONUtils.toJSON(allData);

            System.out.println("Processing GET with Lamport time " + lamportTime +
                    " ,returning " + allData.length + " stations");

            // Send JSON response
            responseBuilder.sendJsonResponse(outputWriter, HttpStatusCodes.OK,
                    jsonResponse, lamportTime);

            // Log statistics for monitoring
            logRequestStatistics(allData);

        } catch (Exception e) {
            System.out.println("Error processing GET request: " + e.getMessage());

            responseBuilder.sendErrorResponse(outputWriter, HttpStatusCodes.INTERNAL_SERVER_ERROR,
                    HttpStatusCodes.INTERNAL_SERVER_ERROR_TEXT, lamportTime);
        }
    }

    // Logs request statistics for monitoring and debugging purposes
    private void logRequestStatistics(WeatherData[] weatherData) {
        if (weatherData.length == 0) {
            System.out.println("No weather data available for clients");
            return;
        }

        // Count stations by state for basic monitoring
        java.util.Map<String, Integer> stateCount = new java.util.HashMap<>();
        for (WeatherData data : weatherData) {
            if (data.getState() != null) {
                stateCount.merge(data.getState(), 1, Integer::sum);
            }
        }

        System.out.println("Weather data served, Total stations: " + weatherData.length +
                ", States: " + stateCount);
    }
}