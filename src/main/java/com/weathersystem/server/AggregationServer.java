package com.weathersystem.server;

import com.weathersystem.server.network.TimestampedRequest;
import com.weathersystem.server.persistence.WeatherStationEntry;
import com.weathersystem.shared.json.JSONUtils;
import com.weathersystem.shared.domain.WeatherData;
import com.weathersystem.shared.clock.LamportClock;

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
    private static final long EXPIRY_TIME_MS = 30 * 1000;
    private static final long CLEANUP_INTERVAL_MS = 5 * 1000;
    private static final int THREAD_POOL_SIZE = 10;

    // Lamport clock for server
    private static final LamportClock lamportClock = new LamportClock();

    // Request ordering queue
    private static final PriorityBlockingQueue<TimestampedRequest> requestQueue =
            new PriorityBlockingQueue<>();

    // Thread-safe storage
    private static final ConcurrentHashMap<String, WeatherStationEntry> weatherStationStore =
            new ConcurrentHashMap<>();

    private static final ReentrantReadWriteLock dataStoreLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock.ReadLock readLock = dataStoreLock.readLock();
    private static final ReentrantReadWriteLock.WriteLock writeLock = dataStoreLock.writeLock();

    // Thread pools
    private static ExecutorService requestHandlerPool;
    private static ExecutorService requestProcessorPool;
    private static ScheduledExecutorService cleanupService;

    public static void main(String[] args) {
        System.out.println("Aggregation Server starting on port " + PORT);
        System.out.println("Initial Lamport clock: " + lamportClock.getTime());

        // Initialize thread pools
        requestHandlerPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        requestProcessorPool = Executors.newSingleThreadExecutor(); // Single processor for ordering
        cleanupService = Executors.newSingleThreadScheduledExecutor();

        loadDataFromFile();
        startCleanupService();
        startRequestProcessor();

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server listening on port " + PORT);
            System.out.println("Request ordering system started");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from: " + clientSocket.getRemoteSocketAddress());

                // Submit connection handling to thread pool
                requestHandlerPool.submit(new ConnectionHandler(clientSocket));
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        } finally {
            shutdownGracefully();
        }
    }

    // Handles initial connection parsing and queuing
    private static class ConnectionHandler implements Runnable {
        private final Socket clientSocket;

        public ConnectionHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), false);

                // Parse HTTP request
                String requestLine = in.readLine();
                if (requestLine == null) {
                    return;
                }

                System.out.println("Parsing request: " + requestLine +
                        " (Thread: " + Thread.currentThread().getName() + ")");

                String[] requestParts = requestLine.split(" ");
                String method = requestParts[0];

                // Read headers
                String headerLine;
                int contentLength = 0;
                long clientLamportTime = -1;

                while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                    if (headerLine.toLowerCase().startsWith("content-length:")) {
                        contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
                    } else if (headerLine.toLowerCase().startsWith("lamport-time:")) {
                        clientLamportTime = Long.parseLong(headerLine.split(":")[1].trim());
                    }
                }

                // Update server's Lamport clock
                long updatedTime;
                if (clientLamportTime != -1) {
                    updatedTime = lamportClock.update(clientLamportTime);
                    System.out.println("Updated server Lamport clock: " + clientLamportTime +
                            " -> " + updatedTime);
                } else {
                    updatedTime = lamportClock.tick();
                    System.out.println("Client without Lamport timestamp, server time: " + updatedTime);
                }

                // Create timestamped request and add to priority queue
                TimestampedRequest timestampedRequest = new TimestampedRequest(
                        clientSocket, method, contentLength, updatedTime, in, out
                );

                requestQueue.offer(timestampedRequest);
                System.out.println("Queued " + method + " request with timestamp: " + updatedTime +
                        " (Queue size: " + requestQueue.size() + ")");

            } catch (Exception e) {
                System.out.println("Error in connection handler: " + e.getMessage());
                try {
                    clientSocket.close();
                } catch (IOException ioException) {
                    System.out.println("Error closing socket: " + ioException.getMessage());
                }
            }
        }
    }

    // Processes requests in Lamport timestamp order
    private static void startRequestProcessor() {
        requestProcessorPool.submit(() -> {
            System.out.println("Request processor started - processing in Lamport timestamp order");

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Take requests in timestamp order (blocking call)
                    TimestampedRequest request = requestQueue.take();

                    System.out.println("Processing " + request.getMethod() +
                            " request with Lamport time: " + request.getLamportTime());

                    // Process the request
                    processTimestampedRequest(request);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.out.println("Error processing request: " + e.getMessage());
                }
            }
        });
    }

    private static void processTimestampedRequest(TimestampedRequest request) {
        try {
            if ("PUT".equals(request.getMethod())) {
                handlePutRequest(request);
            } else if ("GET".equals(request.getMethod())) {
                handleGetRequest(request);
            } else {
                sendErrorResponse(request.getOutputWriter(), 400, "Bad Request",
                        request.getLamportTime());
            }
        } finally {
            try {
                request.getClientSocket().close();
                System.out.println("Client disconnected after processing Lamport time: " +
                        request.getLamportTime());
            } catch (IOException e) {
                System.out.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private static void handlePutRequest(TimestampedRequest request) {
        writeLock.lock();
        try {
            char[] jsonChars = new char[request.getContentLength()];
            request.getInputReader().read(jsonChars, 0, request.getContentLength());
            String jsonData = new String(jsonChars);

            System.out.println("Processing PUT with Lamport time " + request.getLamportTime() +
                    ": " + jsonData.substring(0, Math.min(50, jsonData.length())) + "...");

            WeatherData weatherData = JSONUtils.fromJSON(jsonData);

            // Use current server time for storage timestamp
            long currentTime = System.currentTimeMillis();
            WeatherStationEntry entry = new WeatherStationEntry(weatherData, currentTime);

            weatherStationStore.put(weatherData.getId(), entry);
            System.out.println("Stored weather data for station: " + weatherData.getId() +
                    " (Lamport time: " + request.getLamportTime() + ")");

        } catch (Exception e) {
            System.out.println("Error processing PUT request: " + e.getMessage());
            sendErrorResponse(request.getOutputWriter(), 500, "Internal Server Error",
                    request.getLamportTime());
            return;
        } finally {
            writeLock.unlock();
        }

        // Save to file outside lock
        saveDataToFile();

        // Send response with server's Lamport time
        sendSuccessResponse(request.getOutputWriter(), 201, "Created",
                request.getLamportTime());
    }

    private static void handleGetRequest(TimestampedRequest request) {
        readLock.lock();
        try {
            WeatherData[] allData = weatherStationStore.values().stream()
                    .map(WeatherStationEntry::getWeatherData)
                    .toArray(WeatherData[]::new);

            String jsonResponse = JSONUtils.toJSON(allData);

            System.out.println("Processing GET with Lamport time " + request.getLamportTime() +
                    " - returning " + allData.length + " stations");

            sendJsonResponse(request.getOutputWriter(), 200, jsonResponse,
                    request.getLamportTime());

        } catch (Exception e) {
            System.out.println("Error processing GET request: " + e.getMessage());
            sendErrorResponse(request.getOutputWriter(), 500, "Internal Server Error",
                    request.getLamportTime());
        } finally {
            readLock.unlock();
        }
    }

    // Updated response methods to include Lamport timestamps
    private static void sendSuccessResponse(PrintWriter out, int statusCode, String statusText,
                                            long lamportTime) {
        out.print("HTTP/1.1 " + statusCode + " " + statusText + "\r\n");
        out.print("Lamport-Time: " + lamportTime + "\r\n");
        out.print("Content-Length: 0\r\n");
        out.print("\r\n");
        out.flush();
    }

    private static void sendJsonResponse(PrintWriter out, int statusCode, String jsonData,
                                         long lamportTime) {
        byte[] jsonBytes = jsonData.getBytes();

        out.print("HTTP/1.1 " + statusCode + " OK\r\n");
        out.print("Content-Type: application/json\r\n");
        out.print("Lamport-Time: " + lamportTime + "\r\n");
        out.print("Content-Length: " + jsonBytes.length + "\r\n");
        out.print("\r\n");
        out.print(jsonData);
        out.flush();
    }

    private static void sendErrorResponse(PrintWriter out, int statusCode, String statusText,
                                          long lamportTime) {
        out.print("HTTP/1.1 " + statusCode + " " + statusText + "\r\n");
        out.print("Lamport-Time: " + lamportTime + "\r\n");
        out.print("Content-Length: 0\r\n");
        out.print("\r\n");
        out.flush();
    }

    // Keep all your existing methods (startCleanupService, removeExpiredData,
    // loadDataFromFile, saveDataToFile, etc.) unchanged - they work with the new architecture

    private static void startCleanupService() {
        cleanupService.scheduleAtFixedRate(
                AggregationServer::removeExpiredData,
                CLEANUP_INTERVAL_MS,
                CLEANUP_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
        System.out.println("Started background cleanup service");
    }

    private static void removeExpiredData() {
        writeLock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            int initialSize = weatherStationStore.size();

            weatherStationStore.entrySet().removeIf(entry -> {
                WeatherStationEntry stationEntry = entry.getValue();
                boolean expired = stationEntry.isExpired(currentTime, EXPIRY_TIME_MS);

                if (expired) {
                    System.out.println("Removing expired weather station: " + entry.getKey());
                }
                return expired;
            });

            int removedCount = initialSize - weatherStationStore.size();
            if (removedCount > 0) {
                System.out.println("Removed " + removedCount + " expired stations");
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

        writeLock.lock();
        try {
            if (dataFile.exists()) {
                if (loadFromFile(dataFile)) {
                    System.out.println("Loaded " + weatherStationStore.size() +
                            " weather stations from " + DATA_FILE);
                    return;
                } else {
                    System.out.println("Primary data file corrupted. Trying backup file");
                }
            }

            if (backupFile.exists()) {
                if (loadFromFile(backupFile)) {
                    System.out.println("Loaded " + weatherStationStore.size() +
                            " weather stations from backup");
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
        readLock.lock();
        try {
            WeatherData[] allData = weatherStationStore.values().stream()
                    .map(WeatherStationEntry::getWeatherData)
                    .toArray(WeatherData[]::new);

            String jsonData = JSONUtils.toJSON(allData);
            readLock.unlock();

            performFileWrite(jsonData);

        } catch (Exception e) {
            System.out.println("Error saving weather data: " + e.getMessage());
        } finally {
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

    private static void shutdownGracefully() {
        System.out.println("Shutting down server...");

        if (requestHandlerPool != null) {
            requestHandlerPool.shutdown();
        }
        if (requestProcessorPool != null) {
            requestProcessorPool.shutdown();
        }
        if (cleanupService != null) {
            cleanupService.shutdown();
        }

        System.out.println("Server shutdown complete.");
    }
}