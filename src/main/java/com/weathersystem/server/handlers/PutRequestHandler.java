package com.weathersystem.server.handlers;

import com.weathersystem.server.http.HttpRequest;
import com.weathersystem.server.http.HttpResponseBuilder;
import com.weathersystem.server.http.HttpStatusCodes;
import com.weathersystem.server.services.WeatherDataService;
import com.weathersystem.shared.domain.WeatherData;
import com.weathersystem.shared.json.JSONUtils;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class PutRequestHandler implements RequestHandler {

    private final WeatherDataService weatherDataService;
    private final HttpResponseBuilder responseBuilder;
    private final Runnable onDataChanged; // Callback for persistence trigger

    public PutRequestHandler(WeatherDataService weatherDataService, Runnable onDataChanged) {
        this.weatherDataService = weatherDataService;
        this.responseBuilder = new HttpResponseBuilder();
        this.onDataChanged = onDataChanged;
    }

    @Override
    public String getSupportedMethod() {
        return "PUT";
    }

    @Override
    public void handle(HttpRequest httpRequest, BufferedReader inputReader,
                       PrintWriter outputWriter, long lamportTime) throws Exception {

        // Validate request has content
        if (httpRequest.getContentLength() <= 0) {
            System.out.println("PUT request has no content");
            responseBuilder.sendErrorResponse(outputWriter, HttpStatusCodes.NO_CONTENT,
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

        // Parse and validate JSON
        WeatherData weatherData;
        try {
            weatherData = JSONUtils.fromJSON(jsonData);
            validateWeatherData(weatherData);
        } catch (Exception e) {
            System.out.println("Invalid JSON data: " + e.getMessage());
            responseBuilder.sendErrorResponse(outputWriter, HttpStatusCodes.BAD_REQUEST,
                    "Invalid JSON format", lamportTime);
            return;
        }

        // Store weather data
        try {
            boolean isNewStation = weatherDataService.storeWeatherData(weatherData);

            System.out.println("Processing PUT with Lamport time " + lamportTime +
                    " for station: " + weatherData.getId() +
                    " (" + (isNewStation ? "new" : "update") + ")");

            // Trigger persistence callback
            if (onDataChanged != null) {
                onDataChanged.run();
            }

            // Send appropriate response
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