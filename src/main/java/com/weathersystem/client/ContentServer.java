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

    public ContentServer(ClientConfiguration config) {
        super(config);
    }

    public static void main(String[] args) {

        String serverAddress = args[0];
        String weatherFile = args[1];

        ClientConfiguration config = ClientConfiguration.fromServerAddress(serverAddress);
        ContentServer contentServer = new ContentServer(config);

        System.out.println("Content Server starting");

        try {
            contentServer.publishWeatherData(weatherFile);
            System.out.println("Weather data published successfully");

        } catch (Exception e) {
            System.out.println("Failed to publish weather data: " + e.getMessage());
            System.exit(1);
        }
    }

    public void publishWeatherData(String weatherFile) throws Exception {
        int maxAttempts = 4;
        long retryDelayMs = 1000;
        double backoff = 2.0;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                uploadWeatherFile(weatherFile);
                System.out.println("Weather data published successfully");
                return;
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    throw new Exception("Weather data upload failed after " + maxAttempts + " attempts", e);
                }
                System.out.println("Weather data upload failed (attempt " + attempt + "/" + maxAttempts +
                        "): " + e.getMessage() + ". Retrying in " + retryDelayMs + "ms...");
                Thread.sleep(retryDelayMs);
                retryDelayMs *= (long) backoff;
            }
        }
    }

    private void uploadWeatherFile(String weatherFile) throws Exception {
        long processingTime = lamportClock.tick();
        System.out.println("Processing weather file (Lamport time: " + processingTime + ")");

        WeatherData weatherData = FileUtils.parseWeatherFile(weatherFile);
        String jsonData = JSONUtils.toJSON(weatherData);
        byte[] jsonBytes = jsonData.getBytes(StandardCharsets.UTF_8);

        System.out.println("Uploading weather data for station: " + weatherData.getId());

        Socket socket = null;
        try {
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

    private void validateResponse(HttpResponse response) throws IOException {
        if (response.isSuccess()) {
            throw new IOException("Server rejected weather data: " +
                    response.getStatusCode() + " " + response.getStatusText());
        }

        if (response.getStatusCode() == 201) {
            System.out.println("New weather station registered");
        } else if (response.getStatusCode() == 200) {
            System.out.println("Existing weather station data updated");
        }
    }
}