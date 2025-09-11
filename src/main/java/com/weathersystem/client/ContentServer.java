package com.weathersystem.client;

import com.weathersystem.utils.FileUtils;
import com.weathersystem.shared.JSONUtils;
import com.weathersystem.shared.WeatherData;

import java.io.*;
import java.net.*;

public class ContentServer {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 4567;

    public static void main(String[] args) {
        // Parse command line arguments
        if (args.length < 2) {
            System.out.println("Usage: java ContentServer <server:port> <weather_file>");
            System.out.println("Example: java ContentServer localhost:4567 input/weather1.txt");
            return;
        }

        String serverAddress = args[0];
        String weatherFile = args[1];

        // Parse server address
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (serverAddress.contains(":")) {
            String[] parts = serverAddress.split(":");
            host = parts[0];
            port = Integer.parseInt(parts[1]);
        }

        System.out.println("Content Server starting...");
        System.out.println("Target server: " + host + ":" + port);
        System.out.println("Weather file: " + weatherFile);

        try {
            System.out.println("Reading weather data from file...");
            WeatherData weatherData = FileUtils.parseWeatherFile(weatherFile);
            System.out.println("Parsed data: " + weatherData);

            String jsonData = JSONUtils.toJSON(weatherData);
            System.out.println("JSON data: " + jsonData);

            sendWeatherData(host, port, jsonData);

        } catch (FileNotFoundException e) {
            System.out.println("Error: Weather file not found: " + weatherFile);
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        System.out.println("Content Server finished");
    }

    private static void sendWeatherData(String host, int port, String jsonData) throws IOException {
        Socket socket = new Socket(host, port);
        System.out.println("Connected to aggregation server");

        byte[] jsonBytes = jsonData.getBytes("UTF-8");
        int contentLength = jsonBytes.length;

        PrintWriter out = new PrintWriter(socket.getOutputStream(), false);

        out.print("PUT /weather.json HTTP/1.1\r\n");
        out.print("Host: " + host + ":" + port + "\r\n");
        out.print("Content-Type: application/json\r\n");
        out.print("Content-Length: " + contentLength + "\r\n");
        out.print("\r\n"); // Empty line separates headers from body

        out.flush();

        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(jsonBytes);
        outputStream.flush();

        System.out.println("Sent HTTP PUT request with weather data");

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String responseLine = in.readLine();
        System.out.println("Server response: " + responseLine);

        socket.close();
        System.out.println("Connection closed");
    }
}