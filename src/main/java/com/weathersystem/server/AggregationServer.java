package com.weathersystem.server;

import com.weathersystem.shared.JSONUtils;
import com.weathersystem.shared.WeatherData;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AggregationServer {

    private static final int PORT = 4567;
    private static final String DATA_FILE = "data/weather.json";
    private static final String BACKUP_FILE = "data/weather.json.backup";
    private static final long EXPIRY_TIME_MS = 30 * 1000; // 30 seconds
    private static final long CLEANUP_INTERVAL_MS = 5 * 1000; // 5 seconds
    private static final int THREAD_POOL_SIZE = 10; // Maximum concurrent clients

    // Thread-safe storage with explicit locking
    private static final ConcurrentHashMap<String, WeatherStationEntry> weatherStationStore =
            new ConcurrentHashMap<>();

    // ReadWriteLock for coordinated access
    private static final ReentrantReadWriteLock dataStoreLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock.ReadLock readLock = dataStoreLock.readLock();
    private static final ReentrantReadWriteLock.WriteLock writeLock = dataStoreLock.writeLock();

    // Thread pools
    private static ExecutorService requestHandlerPool;
    private static ScheduledExecutorService cleanupService;

    public static void main(String[] args) {
        System.out.println("Aggregation Server starting on port " + PORT);
        System.out.println("Thread pool size: " + THREAD_POOL_SIZE + " concurrent clients");

        // Initialise thread pools
        requestHandlerPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        cleanupService = Executors.newSingleThreadScheduledExecutor();

        loadDataFromFile();
        startCleanupService();

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server listening on port " + PORT);
            System.out.println("Press Ctrl+C to stop the server");

            // Connection acceptance loop
            while (true) {
                System.out.println("Waiting for client connection...");

                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from: " + clientSocket.getRemoteSocketAddress());

                // Submit request to thread pool instead of processing directly
                requestHandlerPool.submit(new ClientRequestHandler(clientSocket));
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        } finally {
            // Cleanup thread pools
            shutdownGracefully();
        }
    }

    // Client request handler as a Runnable task
    private static class ClientRequestHandler implements Runnable {
        private final Socket clientSocket;

        public ClientRequestHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                handleClientRequest(clientSocket);
            } catch (Exception e) {
                System.out.println("Error in request handler: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                    System.out.println("Client disconnected from thread: " +
                            Thread.currentThread().getName());
                } catch (IOException e) {
                    System.out.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }

    private static void startCleanupService() {
        cleanupService.scheduleAtFixedRate(
                AggregationServer::removeExpiredData,
                CLEANUP_INTERVAL_MS,
                CLEANUP_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
        System.out.println("Started background cleanup service (checking every " +
                (CLEANUP_INTERVAL_MS/1000) + " seconds)");
    }

    private static void removeExpiredData() {
        // Use write lock for data modification
        writeLock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            int initialSize = weatherStationStore.size();

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
                // Note: saveDataToFile() will acquire its own locks
                saveDataToFile();
            }
        } finally {
            writeLock.unlock();
        }
    }

    private static void loadDataFromFile() {
        System.out.println("Loading weather data from persistent storage...");

        File dataFile = new File(DATA_FILE);
        File backupFile = new File(BACKUP_FILE);

        // Use write lock during initial loading
        writeLock.lock();
        try {
            if (dataFile.exists()) {
                if (loadFromFile(dataFile)) {
                    System.out.println("Loaded " + weatherStationStore.size() +
                            " weather stations from " + DATA_FILE);
                    return;
                } else {
                    System.out.println("Primary data file corrupted. Trying the backup file");
                }
            }

            if (backupFile.exists()) {
                if (loadFromFile(backupFile)) {
                    System.out.println("Loaded " + weatherStationStore.size() +
                            " weather stations from the backup");
                    saveDataToFile();
                    return;
                }
            }

            System.out.println("No existing weather data found. Starting with empty database.");
        } finally {
            writeLock.unlock();
        }
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
        // Use read lock to access data for saving
        readLock.lock();
        try {
            WeatherData[] allData = weatherStationStore.values().stream()
                    .map(WeatherStationEntry::getWeatherData)
                    .toArray(WeatherData[]::new);

            String jsonData = JSONUtils.toJSON(allData);

            // File operations don't need the data lock
            readLock.unlock();

            // Perform file operations without holding data lock
            performFileWrite(jsonData);

        } catch (Exception e) {
            System.out.println("Error saving weather data: " + e.getMessage());
        } finally {
            // Ensure lock is released even if exception occurs
            if (readLock.tryLock()) {
                readLock.unlock();
            }
        }
    }

    private static void performFileWrite(String jsonData) throws IOException {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

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
    }

    private static void handlePutRequest(BufferedReader in, PrintWriter out, int contentLength) {
        // Use write lock for data modification
        writeLock.lock();
        try {
            char[] jsonChars = new char[contentLength];
            in.read(jsonChars, 0, contentLength);
            String jsonData = new String(jsonChars);

            System.out.println("Received JSON from thread " + Thread.currentThread().getName() +
                    ": " + jsonData.substring(0, Math.min(50, jsonData.length())) + "...");

            WeatherData weatherData = JSONUtils.fromJSON(jsonData);

            long currentTime = System.currentTimeMillis();
            WeatherStationEntry entry = new WeatherStationEntry(weatherData, currentTime);

            weatherStationStore.put(weatherData.getId(), entry);
            System.out.println("Stored weather data for station: " + weatherData.getId() +
                    " (Thread: " + Thread.currentThread().getName() + ")");
            System.out.println("Total stations: " + weatherStationStore.size());

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            writeLock.unlock();
        }

        // Save to file outside the lock to reduce contention
        saveDataToFile();

        try {
            sendSuccessResponse(out, 201, "Created");
        } catch (Exception e) {
            System.out.println("Error sending response: " + e.getMessage());
        }
    }

    private static void handleGetRequest(PrintWriter out) {
        // Use read lock for data access
        readLock.lock();
        try {
            WeatherData[] allData = weatherStationStore.values().stream()
                    .map(WeatherStationEntry::getWeatherData)
                    .toArray(WeatherData[]::new);

            String jsonResponse = JSONUtils.toJSON(allData);

            System.out.println("Sending weather data for " + allData.length +
                    " stations (Thread: " + Thread.currentThread().getName() + ")");

            sendJsonResponse(out, 200, jsonResponse);

        } catch (Exception e) {
            System.out.println("Error processing GET request: " + e.getMessage());
            sendErrorResponse(out, 500, "Internal Server Error");
        } finally {
            readLock.unlock();
        }
    }

    private static void handleClientRequest(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), false);

            String requestLine = in.readLine();
            System.out.println("Request from " + Thread.currentThread().getName() + ": " + requestLine);

            if (requestLine == null) {
                return;
            }

            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0];

            String headerLine;
            int contentLength = 0;

            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
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

    private static void shutdownGracefully() {
        System.out.println("Shutting down server...");

        if (requestHandlerPool != null) {
            requestHandlerPool.shutdown();
            try {
                if (!requestHandlerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    requestHandlerPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                requestHandlerPool.shutdownNow();
            }
        }

        if (cleanupService != null) {
            cleanupService.shutdown();
        }

        System.out.println("Server shutdown complete.");
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