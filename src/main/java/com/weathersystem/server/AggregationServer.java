package com.weathersystem.server;

import com.weathersystem.shared.JSONUtils;
import com.weathersystem.shared.WeatherData;
import com.weathersystem.utils.FileUtils;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;

public class AggregationServer {

    private static final int PORT = 4567;
    private static final String DATA_FILE = "data/weather.json";
    private static final String BACKUP_FILE = "data/weather.json.backup";

    // Store weather data by station ID
    private static final ConcurrentHashMap<String, WeatherData> weatherDataStore =
            new ConcurrentHashMap<>();

    public static void main(String[] args){

        System.out.println("Aggregation Server starting on port " + PORT);

        loadDataFromFile();

        try{
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server listening on port " + PORT);

            while (true) {
                System.out.println("Waiting for client connection\n");

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

    private static void loadDataFromFile(){
        System.out.println("Loading weather data from persistent storage");

        File dataFile = new File(DATA_FILE);
        File backupFile = new File(BACKUP_FILE);

        if(dataFile.exists()){
            if(loadFromFile(dataFile)){
                System.out.println("Loaded " + weatherDataStore.size() + " weather stations from " + DATA_FILE);
                return;
            }else{
                System.out.println("Primary data file corrupted. Trying the backup file");
            }
        }

        if(backupFile.exists()){
            if(loadFromFile(backupFile)){
                System.out.println("Loaded " + weatherDataStore.size() + " weather stations from the backup");
                saveDataToFile();
                return;
            }
        }

        System.out.println("No existing weather data found. Starting with empty file.");
    }

    private static boolean loadFromFile(File file){

        try{
            String jsonContent = new String(Files.readAllBytes(file.toPath()));

            if(jsonContent.trim().isEmpty()){
                return true;
            }

            WeatherData[] weatherArray = JSONUtils.fromJSONArray(jsonContent);

            weatherDataStore.clear();
            for(WeatherData weatherData : weatherArray){
                weatherDataStore.put(weatherData.getId(), weatherData);
            }
            return true;
        }catch (Exception e) {
            System.out.println("Error loading from " + file.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private static void saveDataToFile(){
        try{
            WeatherData[] allData = weatherDataStore.values().toArray(WeatherData[]::new);
            String jsonData = JSONUtils.toJSON(allData);

            File tempFile = new File(DATA_FILE + ".tmp");
            File dataFile = new File(DATA_FILE);
            File backupFile = new File(BACKUP_FILE);

            try (FileWriter writer = new FileWriter(tempFile)){
                writer.write(jsonData);
            }

            if(dataFile.exists()){
                Files.copy(dataFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            Files.move(tempFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Weather data saved to persistent storage");

        }catch (IOException e) {
            System.out.println("Error saving weather data: " + e.getMessage());
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

            saveDataToFile();
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