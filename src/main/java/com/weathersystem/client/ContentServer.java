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

    // Sets up the content server with the given config
    public ContentServer(ClientConfiguration config) {
        super(config);
    }

    // Main method that gets called when you run this from the command line
    public static void main(String[] args) {

        String serverAddress = args[0];
        String weatherFile = args[1];

        // Support multiple servers, parse server addresses and create configuration
        ClientConfiguration config;
        if (serverAddress.contains(",")) {
            config = ClientConfiguration.fromMultipleServers(serverAddress);
            System.out.println("Using multiple servers: " + config.getServerAddresses());
        } else {
            config = ClientConfiguration.fromServerAddress(serverAddress);
        }

        ContentServer contentServer = new ContentServer(config);

        System.out.println("\nStarting content server");

        try {
            contentServer.publishWeatherData(weatherFile);
            System.out.println("Weather data uploaded ok");

        } catch (Exception e) {
            System.out.println("\nFailed to upload weather data: " + e.getMessage() + "\n");
            System.exit(1);
        }
    }

    // Uploads weather data and keeps trying if it stuffs up
    public void publishWeatherData(String weatherFile) throws Exception {
        int maxAttempts = 4;
        long retryDelayMs = 1000;
        double backoff = 2.0;

        // Retry loop with exponential backoff for failed uploads
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                uploadWeatherFile(weatherFile);
                System.out.println("\nWeather data uploaded ok\n");
                return;
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    throw new Exception("Weather data upload failed after " + maxAttempts + " attempts", e);
                }
                System.out.println("Upload failed, trying again in " + retryDelayMs + "ms");
                Thread.sleep(retryDelayMs);
                retryDelayMs *= (long) backoff;
            }
        }
    }

    // Actually sends the weather file to the server
    private void uploadWeatherFile(String weatherFile) throws Exception {
        long processingTime = lamportClock.tick();
        System.out.println("\nProcessing weather file (time: " + processingTime + ")");

        // Parse weather file and convert to JSON for transmission
        WeatherData weatherData = FileUtils.parseWeatherFile(weatherFile);
        String jsonData = JSONUtils.toJSON(weatherData);
        byte[] jsonBytes = jsonData.getBytes(StandardCharsets.UTF_8);

        System.out.println("Uploading data for station: " + weatherData.getId());

        Socket socket = null;
        try {
            // Create connection and send HTTP PUT request with weather data
            socket = createConnection();
            sendHttpRequest(socket, "PUT", "application/json", jsonBytes);
            HttpResponse response = receiveHttpResponse(socket);

            validateResponse(response);
            System.out.println("Server response: " +
                    response.getStatusCode() + " " + response.getStatusText());

        } finally {
            closeConnection(socket);
        }
    }

    // Checks if the server response is good and tells you what happened
    private void validateResponse(HttpResponse response) throws IOException {
        if (!response.isSuccess()) {
            throw new IOException("Server rejected weather data: " +
                    response.getStatusCode() + " " + response.getStatusText());
        }

        // Provide specific feedback based on HTTP status codes
        if (response.getStatusCode() == 201) {
            System.out.println("New station added");
        } else if (response.getStatusCode() == 200) {
            System.out.println("Station data updated");
        }
    }
}