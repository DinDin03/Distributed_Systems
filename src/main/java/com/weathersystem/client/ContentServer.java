package com.weathersystem.client;

import com.weathersystem.utils.FileUtils;
import com.weathersystem.shared.json.JSONUtils;
import com.weathersystem.shared.domain.WeatherData;
import com.weathersystem.shared.clock.LamportClock;

import java.io.*;
import java.net.*;

public class ContentServer {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 4567;
    private static final LamportClock lamportClock = new LamportClock();

    public static void main(String[] args) {

        String serverAddress = args[0];
        String weatherFile = args[1];

        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (serverAddress.contains(":")) {
            String[] parts = serverAddress.split(":");
            host = parts[0];
            port = Integer.parseInt(parts[1]);
        }

        System.out.println("Content Server starting");
        System.out.println("Initial Lamport clock: " + lamportClock.getTime());

        try {
            // Tick a clock for local processing
            long currentTime = lamportClock.tick();
            System.out.println("Processing weather file (Lamport time: " + currentTime + ")");

            WeatherData weatherData = FileUtils.parseWeatherFile(weatherFile);
            String jsonData = JSONUtils.toJSON(weatherData);

            // Send data with Lamport timestamp
            sendWeatherData(host, port, jsonData);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        System.out.println("Content Server finished. Final Lamport clock: " + lamportClock.getTime());
    }

    private static void sendWeatherData(String host, int port, String jsonData) throws IOException {
        Socket socket = new Socket(host, port);
        System.out.println("Connected to aggregation server");

        // Tick clock before sending request
        long sendTime = lamportClock.tick();
        System.out.println("Sending request (Lamport time: " + sendTime + ")");

        byte[] jsonBytes = jsonData.getBytes("UTF-8");
        int contentLength = jsonBytes.length;

        PrintWriter out = new PrintWriter(socket.getOutputStream(), false);

        // Include Lamport timestamp in HTTP headers
        out.print("PUT /weather.json HTTP/1.1\r\n");
        out.print("Host: " + host + ":" + port + "\r\n");
        out.print("Content-Type: application/json\r\n");
        out.print("Content-Length: " + contentLength + "\r\n");
        out.print("Lamport-Time: " + sendTime + "\r\n");  // NEW: Lamport timestamp
        out.print("\r\n");

        out.flush();

        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(jsonBytes);
        outputStream.flush();

        System.out.println("Sent HTTP PUT request with Lamport timestamp: " + sendTime);

        // Read response and update clock
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String responseLine = in.readLine();

        // Check for Lamport timestamp in response
        String headerLine;
        long responseTime = -1;
        while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
            if (headerLine.toLowerCase().startsWith("lamport-time:")) {
                responseTime = Long.parseLong(headerLine.split(":")[1].trim());
            }
        }

        if (responseTime != -1) {
            long updatedTime = lamportClock.update(responseTime);
            System.out.println("Updated Lamport clock from server response: " + responseTime +
                    " -> " + updatedTime);
        }

        System.out.println("Server response: " + responseLine);
        socket.close();
    }
}