package com.weathersystem.server;

import com.weathersystem.shared.JSONUtils;
import com.weathersystem.shared.WeatherData;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AggregationServer {

    private static final int PORT = 4567;
    private static final String DATA_FILE = "data/weather.json";
    private static final String BACKUP_FILE = "data/weather.json.backup";
    private static final long EXPIRY_TIME_MS = 30 * 1000; // 30 seconds in milliseconds
    private static final long CLEANUP_INTERVAL_MS = 5 * 1000; // 5 seconds

    // Store weather station entries (data + timestamp) by station ID
    private static final ConcurrentHashMap<String, WeatherStationEntry> weatherStationStore =
            new ConcurrentHashMap<>();

    // Background thread for cleanup
    private static ScheduledExecutorService cleanupService;

    public static void main(String[] args) {
        System.out.println("Aggregation Server starting on port " + PORT);

        loadDataFromFile();
        startCleanupService();

        try {
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

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        } finally {
            // Cleanup thread pool on shutdown
            if (cleanupService != null) {
                cleanupService.shutdown();
            }
        }
    }

    private static void startCleanupService() {
        cleanupService = Executors.newSingleThreadScheduledExecutor();
        cleanupService.scheduleAtFixedRate(
                AggregationServer::removeExpiredData,  // Method to call
                CLEANUP_INTERVAL_MS,                   // Initial delay
                CLEANUP_INTERVAL_MS,                   // Period between executions
                TimeUnit.MILLISECONDS                  // Time unit
        );
        System.out.println("Started background cleanup service (checking every " +
                (CLEANUP_INTERVAL_MS/1000) + " seconds)");
    }

    private static void removeExpiredData() {
        long currentTime = System.currentTimeMillis();
        int initialSize = weatherStationStore.size();

        // Find expired stations
        weatherStationStore.entrySet().removeIf(entry -> {
            WeatherStationEntry stationEntry = entry.getValue();
            boolean expired = stationEntry.isExpired(currentTime, EXPIRY_TIME_MS);

            if (expired) {
                System.out.println("Removing expired weather station: " + entry.getKey() +
                        " (last update: " +
                        ((currentTime - stationEntry.getLastUpdateTime()) / 1000) +
                        " seconds ago)");
            }
            return expired;
        });

        int removedCount = initialSize - weatherStationStore.size();
        if (removedCount > 0) {
            System.out.println("Removed " + removedCount + " expired stations. " +
                    "Active stations: " + weatherStationStore.size());
            // Save updated data to file
            saveDataToFile();
        }
    }

    // Updated file loading to handle timestamps
    private static void loadDataFromFile() {
        System.out.println("Loading weather data from persistent storage");

        File dataFile = new File(DATA_FILE);
        File backupFile = new File(BACKUP_FILE);

        if (dataFile.exists()) {
            if (loadFromFile(dataFile)) {
                System.out.println("Loaded " + weatherStationStore.size() + " weather stations from " + DATA_FILE);
                return;
            } else {
                System.out.println("Primary data file corrupted. Trying the backup file");
            }
        }

        if (backupFile.exists()) {
            if (loadFromFile(backupFile)) {
                System.out.println("Loaded " + weatherStationStore.size() + " weather stations from the backup");
                saveDataToFile();
                return;
            }
        }

        System.out.println("No existing weather data found. Starting with empty file.");
    }

    private static boolean loadFromFile(File file) {
        try {
            String jsonContent = new String(Files.readAllBytes(file.toPath()));

            if (jsonContent.trim().isEmpty()) {
                return true;
            }

            WeatherData[] weatherArray = JSONUtils.fromJSONArray(jsonContent);

            weatherStationStore.clear();
            long currentTime = System.currentTimeMillis();

            // Load data with current timestamp (assume all loaded data is fresh)
            for (WeatherData weatherData : weatherArray) {
                WeatherStationEntry entry = new WeatherStationEntry(weatherData, currentTime);
                weatherStationStore.put(weatherData.getId(), entry);
            }
            return true;
        } catch (Exception e) {
            System.out.println("Error loading from " + file.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private static void saveDataToFile() {
        try {
            // Extract just the weather data (not timestamps) for file storage
            WeatherData[] allData = weatherStationStore.values().stream()
                    .map(WeatherStationEntry::getWeatherData)
                    .toArray(WeatherData[]::new);

            String jsonData = JSONUtils.toJSON(allData);

            File tempFile = new File(DATA_FILE + ".tmp");
            File dataFile = new File(DATA_FILE);
            File backupFile = new File(BACKUP_FILE);

            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(jsonData);
            }

            if (dataFile.exists()) {
                Files.copy(dataFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            Files.move(tempFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Weather data saved to persistent storage");

        } catch (IOException e) {
            System.out.println("Error saving weather data: " + e.getMessage());
        }
    }

    // Update PUT handler to record timestamps
    private static void handlePutRequest(BufferedReader in, PrintWriter out, int contentLength) {
        try {
            char[] jsonChars = new char[contentLength];
            in.read(jsonChars, 0, contentLength);
            String jsonData = new String(jsonChars);

            System.out.println("Received JSON: " + jsonData);

            WeatherData weatherData = JSONUtils.fromJSON(jsonData);

            // Create entry with current timestamp
            long currentTime = System.currentTimeMillis();
            WeatherStationEntry entry = new WeatherStationEntry(weatherData, currentTime);

            // Store weather station entry (data + timestamp)
            weatherStationStore.put(weatherData.getId(), entry);
            System.out.println("Stored weather data for station: " + weatherData.getId());
            System.out.println("Total stations: " + weatherStationStore.size());

            // Persist to file immediately
            saveDataToFile();

            sendSuccessResponse(out);

        } catch (Exception e) {
            System.out.println("Error processing PUT request: " + e.getMessage());
            sendErrorResponse(out, 500, "Internal Server Error");
        }
    }

    // Update GET handler to extract weather data
    private static void handleGetRequest(PrintWriter out) {
        try {
            // Extract just the weather data from station entries
            WeatherData[] allData = weatherStationStore.values().stream()
                    .map(WeatherStationEntry::getWeatherData)
                    .toArray(WeatherData[]::new);

            String jsonResponse = JSONUtils.toJSON(allData);

            System.out.println("Sending weather data for " + allData.length + " stations");

            sendJsonResponse(out, jsonResponse);

        } catch (Exception e) {
            System.out.println("Error processing GET request: " + e.getMessage());
            sendErrorResponse(out, 500, "Internal Server Error");
        }
    }

    // Keep your existing HTTP response methods unchanged
    private static void handleClientRequest(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), false);

            String requestLine = in.readLine();
            System.out.println("Request: " + requestLine);

            if (requestLine == null) {
                return;
            }

            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0];

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

    private static void sendSuccessResponse(PrintWriter out) {
        out.print("HTTP/1.1 " + 201 + " " + "Created" + "\r\n");
        out.print("Content-Length: 0\r\n");
        out.print("\r\n");
        out.flush();
    }

    private static void sendJsonResponse(PrintWriter out, String jsonData) {
        byte[] jsonBytes = jsonData.getBytes();

        out.print("HTTP/1.1 " + 200 + " OK\r\n");
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