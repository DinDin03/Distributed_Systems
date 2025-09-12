package com.weathersystem.client;

import com.weathersystem.shared.JSONUtils;
import com.weathersystem.shared.WeatherData;

import java.io.*;
import java.net.*;

public class GETClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 4567;

    public static void main(String[] args) {
        System.out.println("GET Client starting...");

        try {
            // Step 1: Establish connection to server
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            System.out.println("Connected to server at " + SERVER_HOST + ":" + SERVER_PORT);

            // Step 2: Send HTTP GET request
            sendGetRequest(socket);

            // Step 3: Read and process response
            String jsonResponse = readHttpResponse(socket);

            // Step 4: Parse and display weather data
            displayWeatherData(jsonResponse);

            socket.close();
            System.out.println("Connection closed");

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        System.out.println("GET Client finished");
    }

    private static void sendGetRequest(Socket socket) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // HTTP GET request structure
        out.print("GET /weather.json HTTP/1.1\r\n");
        out.print("Host: " + SERVER_HOST + ":" + SERVER_PORT + "\r\n");
        out.print("User-Agent: WeatherClient/1.0\r\n");
        out.print("\r\n"); // Empty line terminates headers

        out.flush();
        System.out.println("Sent HTTP GET request");
    }

    private static String readHttpResponse(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Read status line
        String statusLine = in.readLine();
        System.out.println("Server response: " + statusLine);

        // Read headers and find Content-Length
        String headerLine;
        int contentLength = 0;

        while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
            System.out.println("Response header: " + headerLine);
            if (headerLine.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
            }
        }

        // Read JSON body
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
            // Parse JSON array to WeatherData objects
            WeatherData[] weatherStations = JSONUtils.fromJSONArray(jsonResponse);

            System.out.println("\n=== CURRENT WEATHER DATA ===");
            System.out.println("Total weather stations: " + weatherStations.length);
            System.out.println();

            // Display each weather station
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
            System.out.println("Raw response: " + jsonResponse);
        }
    }
}