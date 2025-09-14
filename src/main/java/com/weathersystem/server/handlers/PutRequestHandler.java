package com.weathersystem.server.handlers;

import com.weathersystem.server.http.HttpRequest;
import com.weathersystem.server.http.HttpResponseBuilder;
import com.weathersystem.server.http.HttpStatusCodes;
import com.weathersystem.server.services.WeatherDataService;
import com.weathersystem.shared.domain.WeatherData;
import com.weathersystem.shared.json.JSONUtils;

import java.io.BufferedReader;
import java.io.PrintWriter;

// Handler for processing HTTP PUT requests to store weather data
public class PutRequestHandler implements RequestHandler {

    private final WeatherDataService weatherDataService;
    private final HttpResponseBuilder responseBuilder;
    private final Runnable onDataChanged;

    // Constructor that initializes the handler with weather data service and change callback
    public PutRequestHandler(WeatherDataService weatherDataService, Runnable onDataChanged) {
        this.weatherDataService = weatherDataService;
        this.responseBuilder = new HttpResponseBuilder();
        this.onDataChanged = onDataChanged;
    }

    // Handles PUT requests to store weather data with validation and error handling
    @Override
    public void handle(HttpRequest httpRequest, BufferedReader inputReader,
                       PrintWriter outputWriter, long lamportTime) throws Exception {

        // Handle no content case (assignment requirement: return 204)
        if (httpRequest.getContentLength() <= 0) {
            System.out.println("PUT request has no content, returning 204 No Content");
            responseBuilder.sendSuccessResponse(outputWriter, HttpStatusCodes.NO_CONTENT,
                    HttpStatusCodes.NO_CONTENT_TEXT, lamportTime);
            return;
        }

        // Read JSON data from request body
        String jsonData;
        try {
            jsonData = readRequestBody(inputReader, httpRequest.getContentLength());
        } catch (Exception e) {
            System.out.println("Error reading request body: " + e.getMessage());
            responseBuilder.sendErrorResponse(outputWriter, HttpStatusCodes.BAD_REQUEST,
                    HttpStatusCodes.BAD_REQUEST_TEXT, lamportTime);
            return;
        }

        // Check for empty or whitespace-only content
        if (jsonData.trim().isEmpty()) {
            System.out.println("PUT request has empty content, returning 204 No Content");
            responseBuilder.sendSuccessResponse(outputWriter, HttpStatusCodes.NO_CONTENT,
                    HttpStatusCodes.NO_CONTENT_TEXT, lamportTime);
            return;
        }

        // Parse and validate JSON weather data
        WeatherData weatherData;
        try {
            weatherData = JSONUtils.fromJSON(jsonData);
            validateWeatherData(weatherData);
        } catch (Exception e) {
            System.out.println("Invalid JSON data: " + e.getMessage());
            responseBuilder.sendErrorResponse(outputWriter, HttpStatusCodes.INTERNAL_SERVER_ERROR,
                    HttpStatusCodes.INTERNAL_SERVER_ERROR_TEXT, lamportTime);
            return;
        }

        // Store weather data and send appropriate response
        try {
            boolean isNewStation = weatherDataService.storeWeatherData(weatherData);

            // Trigger persistence callback
            if (onDataChanged != null) {
                onDataChanged.run();
            }

            // Send appropriate response based on assignment requirements
            if (isNewStation) {
                responseBuilder.sendSuccessResponse(outputWriter, HttpStatusCodes.CREATED,
                        HttpStatusCodes.CREATED_TEXT, lamportTime);
            } else {
                responseBuilder.sendSuccessResponse(outputWriter, HttpStatusCodes.OK,
                        HttpStatusCodes.OK_TEXT, lamportTime);
            }

        } catch (Exception e) {
            System.out.println("Error storing weather data: " + e.getMessage());
            responseBuilder.sendErrorResponse(outputWriter, HttpStatusCodes.INTERNAL_SERVER_ERROR,
                    HttpStatusCodes.INTERNAL_SERVER_ERROR_TEXT, lamportTime);
        }
    }

    // Reads request body content from input stream with proper length handling
    private String readRequestBody(BufferedReader inputReader, int contentLength) throws Exception {
        char[] jsonChars = new char[contentLength];
        int totalRead = 0;

        while (totalRead < contentLength) {
            int read = inputReader.read(jsonChars, totalRead, contentLength - totalRead);
            if (read == -1) {
                throw new Exception("Unexpected end of stream while reading request body");
            }
            totalRead += read;
        }

        return new String(jsonChars);
    }

    // Validates weather data for required fields and data integrity
    private void validateWeatherData(WeatherData weatherData) throws Exception {
        if (weatherData == null) {
            throw new Exception("Weather data is null");
        }

        if (weatherData.getId() == null || weatherData.getId().trim().isEmpty()) {
            throw new Exception("Weather data missing required 'id' field");
        }

        if (weatherData.getName() == null || weatherData.getName().trim().isEmpty()) {
            throw new Exception("Weather data missing required 'name' field");
        }
    }
}