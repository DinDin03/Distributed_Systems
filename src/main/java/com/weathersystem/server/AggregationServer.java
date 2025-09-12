package com.weathersystem.server;

import com.weathersystem.shared.JSONUtils;
import com.weathersystem.shared.WeatherData;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class AggregationServer {

    private static final int PORT = 4567;

    // Store weather data by station ID
    private static final ConcurrentHashMap<String, WeatherData> weatherDataStore =
            new ConcurrentHashMap<>();

    public static void main(String[] args){

        System.out.println("Aggregation Server starting on port " + PORT);

        try{
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server listening on port " + PORT);
            System.out.println("Press Ctrl+C to stop the server");

            while (true) {
                System.out.println("Waiting for client connection...");

                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from: " + clientSocket.getRemoteSocketAddress());

                handleClientRequest(clientSocket);

                clientSocket.close();
                System.out.println("Client disconnected\n");
            }

        }catch(IOException e){
            System.out.println("Server error: " + e.getMessage());
        }
    }

    private static void handleClientRequest(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), false);

            // Read HTTP request line
            String requestLine = in.readLine();
            System.out.println("Request: " + requestLine);

            if (requestLine == null) {
                return;
            }

            // Parse HTTP method
            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0];

            // Read headers and find Content-Length
            String headerLine;
            int contentLength = 0;

            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                System.out.println("Header: " + headerLine);
                if (headerLine.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
                }
            }

            if ("PUT".equals(method)) {
                handlePutRequest(in, out, contentLength);
            } else if ("GET".equals(method)) {
                handleGetRequest(out);
            } else {
                sendErrorResponse(out, 400, "Bad Request");
            }

        } catch (IOException e) {
            System.out.println("Error handling client request: " + e.getMessage());
        }
    }

    private static void handlePutRequest(BufferedReader in, PrintWriter out, int contentLength) {
        try {
            // Read JSON body
            char[] jsonChars = new char[contentLength];
            in.read(jsonChars, 0, contentLength);
            String jsonData = new String(jsonChars);

            System.out.println("Received JSON: " + jsonData);

            // Parse JSON to WeatherData
            WeatherData weatherData = JSONUtils.fromJSON(jsonData);

            // Store weather data by station ID
            weatherDataStore.put(weatherData.getId(), weatherData);
            System.out.println("Stored weather data for station: " + weatherData.getId());
            System.out.println("Total stations: " + weatherDataStore.size());

            // Send success response
            sendSuccessResponse(out, 201, "Created");

        } catch (Exception e) {
            System.out.println("Error processing PUT request: " + e.getMessage());
            sendErrorResponse(out, 500, "Internal Server Error");
        }
    }

    private static void handleGetRequest(PrintWriter out) {
        try {
            // Convert all stored weather data to JSON array
            WeatherData[] allData = weatherDataStore.values().toArray(new WeatherData[0]);
            String jsonResponse = JSONUtils.toJSON(allData);

            System.out.println("Sending weather data for " + allData.length + " stations");

            // Send JSON response
            sendJsonResponse(out, 200, jsonResponse);

        } catch (Exception e) {
            System.out.println("Error processing GET request: " + e.getMessage());
            sendErrorResponse(out, 500, "Internal Server Error");
        }
    }

    private static void sendSuccessResponse(PrintWriter out, int statusCode, String statusText) {
        out.print("HTTP/1.1 " + statusCode + " " + statusText + "\r\n");
        out.print("Content-Length: 0\r\n");
        out.print("\r\n");
        out.flush();
    }

    private static void sendJsonResponse(PrintWriter out, int statusCode, String jsonData) {
        byte[] jsonBytes = jsonData.getBytes();

        out.print("HTTP/1.1 " + statusCode + " OK\r\n");
        out.print("Content-Type: application/json\r\n");
        out.print("Content-Length: " + jsonBytes.length + "\r\n");
        out.print("\r\n");
        out.print(jsonData);
        out.flush();
    }

    private static void sendErrorResponse(PrintWriter out, int statusCode, String statusText) {
        out.print("HTTP/1.1 " + statusCode + " " + statusText + "\r\n");
        out.print("Content-Length: 0\r\n");
        out.print("\r\n");
        out.flush();
    }
}