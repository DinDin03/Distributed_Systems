package com.weathersystem.client;

import com.weathersystem.client.common.ClientConfiguration;
import com.weathersystem.client.common.HttpClientBase;
import com.weathersystem.shared.domain.WeatherData;
import com.weathersystem.shared.json.JSONUtils;
import com.weathersystem.utils.FileUtils;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ContentServer extends HttpClientBase {

    // Constructor that initializes the content server with client configuration
    public ContentServer(ClientConfiguration config) {
        super(config);
    }

    // Main entry point for content server command-line interface
    public static void main(String[] args) {

        String serverAddress = args[0];
        String weatherFile = args[1];

        // Support multiple servers, parse server addresses and create configuration
        ClientConfiguration config;
        if (serverAddress.contains(",")) {
            config = ClientConfiguration.fromMultipleServers(serverAddress);
            System.out.println("Configured with multiple servers: " + config.getServerAddresses());
        } else {
            config = ClientConfiguration.fromServerAddress(serverAddress);
        }

        ContentServer contentServer = new ContentServer(config);

        System.out.println("\nContent Server Starting");

        try {
            contentServer.publishWeatherData(weatherFile);
            System.out.println("Weather data published successfully");

        } catch (Exception e) {
            System.out.println("\nFailed to publish weather data: " + e.getMessage() + "\n");
            System.exit(1);
        }
    }

    // Publishes weather data with retry logic and exponential backoff
    public void publishWeatherData(String weatherFile) throws Exception {
        int maxAttempts = 4;
        long retryDelayMs = 1000;
        double backoff = 2.0;

        // Retry loop with exponential backoff for failed uploads
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                uploadWeatherFile(weatherFile);
                System.out.println("\nWeather data published successfully\n");
                return;
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    throw new Exception("Weather data upload failed after " + maxAttempts + " attempts", e);
                }
                System.out.println("Weather data upload failed. Retrying in " + retryDelayMs + "ms");
                Thread.sleep(retryDelayMs);
                retryDelayMs *= (long) backoff;
            }
        }
    }

    // Uploads weather file data to the server via HTTP PUT request
    private void uploadWeatherFile(String weatherFile) throws Exception {
        long processingTime = lamportClock.tick();
        System.out.println("\nProcessing weather file (Lamport time: " + processingTime + ")");

        // Parse weather file and convert to JSON for transmission
        WeatherData weatherData = FileUtils.parseWeatherFile(weatherFile);
        String jsonData = JSONUtils.toJSON(weatherData);
        byte[] jsonBytes = jsonData.getBytes(StandardCharsets.UTF_8);

        System.out.println("Uploading weather data for station: " + weatherData.getId());

        Socket socket = null;
        try {
            // Create connection and send HTTP PUT request with weather data
            socket = createConnection();
            sendHttpRequest(socket, "PUT", "application/json", jsonBytes);
            HttpResponse response = receiveHttpResponse(socket);

            validateResponse(response);
            System.out.println("Server accepted weather data with status: " +
                    response.getStatusCode() + " " + response.getStatusText());

        } finally {
            closeConnection(socket);
        }
    }

    // Validates HTTP response and provides appropriate feedback based on status code
    private void validateResponse(HttpResponse response) throws IOException {
        if (!response.isSuccess()) {
            throw new IOException("Server rejected weather data: " +
                    response.getStatusCode() + " " + response.getStatusText());
        }

        // Provide specific feedback based on HTTP status codes
        if (response.getStatusCode() == 201) {
            System.out.println("New weather station registered");
        } else if (response.getStatusCode() == 200) {
            System.out.println("Existing weather station data updated");
        }
    }
}