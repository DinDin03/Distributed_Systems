package com.weathersystem.client;

import com.weathersystem.shared.json.JSONUtils;
import com.weathersystem.shared.domain.WeatherData;
import com.weathersystem.shared.clock.LamportClock;

import java.io.*;
import java.net.*;

public class GETClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 4567;
    private static final LamportClock lamportClock = new LamportClock();

    public static void main(String[] args) {
        System.out.println("GET Client starting...");
        System.out.println("Initial Lamport clock: " + lamportClock.getTime());

        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            System.out.println("Connected to server at " + SERVER_HOST + ":" + SERVER_PORT);

            sendGetRequest(socket);
            String jsonResponse = readHttpResponse(socket);
            displayWeatherData(jsonResponse);

            socket.close();

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        System.out.println("GET Client finished. Final Lamport clock: " + lamportClock.getTime());
    }

    private static void sendGetRequest(Socket socket) throws IOException {
        // Tick clock before sending request
        long sendTime = lamportClock.tick();
        System.out.println("Sending GET request (Lamport time: " + sendTime + ")");

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        out.print("GET /weather.json HTTP/1.1\r\n");
        out.print("Host: " + SERVER_HOST + ":" + SERVER_PORT + "\r\n");
        out.print("User-Agent: WeatherClient/1.0\r\n");
        out.print("Lamport-Time: " + sendTime + "\r\n");  // NEW: Include Lamport timestamp
        out.print("\r\n");

        out.flush();
    }

    private static String readHttpResponse(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String statusLine = in.readLine();
        System.out.println("Server response: " + statusLine);

        String headerLine;
        int contentLength = 0;
        long responseTime = -1;

        while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
            if (headerLine.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
            } else if (headerLine.toLowerCase().startsWith("lamport-time:")) {
                responseTime = Long.parseLong(headerLine.split(":")[1].trim());
            }
        }

        // Update clock with server's timestamp
        if (responseTime != -1) {
            long updatedTime = lamportClock.update(responseTime);
            System.out.println("Updated Lamport clock from server response: " + responseTime +
                    " -> " + updatedTime);
        }

        if (contentLength > 0) {
            char[] jsonChars = new char[contentLength];
            in.read(jsonChars, 0, contentLength);
            return new String(jsonChars);
        }

        return null;
    }

    private static void displayWeatherData(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            System.out.println("No weather data available");
            return;
        }

        try {
            WeatherData[] weatherStations = JSONUtils.fromJSONArray(jsonResponse);

            System.out.println("\n=== CURRENT WEATHER DATA ===");
            System.out.println("Total weather stations: " + weatherStations.length);
            System.out.println();

            for (int i = 0; i < weatherStations.length; i++) {
                WeatherData station = weatherStations[i];

                System.out.println("Station " + (i + 1) + ":");
                System.out.println("  ID: " + station.getId());
                System.out.println("  Name: " + station.getName());
                System.out.println("  State: " + station.getState());
                System.out.println("  Temperature: " + station.getAirTemp() + "°C");
                System.out.println("  Feels like: " + station.getApparentT() + "°C");
                System.out.println("  Conditions: " + station.getCloud());
                System.out.println("  Humidity: " + station.getRelHum() + "%");
                System.out.println("  Wind: " + station.getWindDir() + " " + station.getWindSpdKmh() + " km/h");
                System.out.println("  Pressure: " + station.getPress() + " hPa");
                System.out.println();
            }

        } catch (Exception e) {
            System.out.println("Error parsing weather data: " + e.getMessage());
        }
    }
}