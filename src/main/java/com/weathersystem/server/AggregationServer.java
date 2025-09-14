package com.weathersystem.server;

import com.weathersystem.server.handlers.GetRequestHandler;
import com.weathersystem.server.handlers.PutRequestHandler;
import com.weathersystem.server.network.ConnectionManager;
import com.weathersystem.server.network.RequestDispatcher;
import com.weathersystem.server.persistence.WeatherDataRepository;
import com.weathersystem.server.services.DataExpiryService;
import com.weathersystem.server.services.RequestOrderingService;
import com.weathersystem.server.services.WeatherDataService;
import com.weathersystem.shared.clock.LamportClock;
import com.weathersystem.shared.domain.WeatherData;

import java.io.IOException;
import java.net.ServerSocket;

// Main server that handles weather data and client connections
public class AggregationServer {

    // Configuration constants
    private static final int DEFAULT_PORT = 4567;
    private static final String DATA_FILE = "data/weather.json";
    private static final String BACKUP_FILE = "data/weather.json.backup";
    private static final long EXPIRY_TIME_MS = 30 * 1000; // 30 seconds
    private static final long CLEANUP_INTERVAL_MS = 5 * 1000; // 5 seconds
    private static final int THREAD_POOL_SIZE = 10;

    // Core services - injected dependencies
    private final LamportClock lamportClock;
    private final WeatherDataService weatherDataService;
    private final WeatherDataRepository weatherDataRepository;
    private final ConnectionManager connectionManager;
    private final RequestOrderingService requestOrderingService;
    private final RequestDispatcher requestDispatcher;
    private final DataExpiryService dataExpiryService;

    // Server state
    private ServerSocket serverSocket;
    private volatile boolean isRunning = false;

    // Sets up all the services and handlers when the server starts
    public AggregationServer() {
        // Initialize core services
        this.lamportClock = new LamportClock();
        this.weatherDataService = new WeatherDataService();
        this.weatherDataRepository = new WeatherDataRepository(DATA_FILE, BACKUP_FILE);

        // Initialize request processing pipeline
        this.requestDispatcher = new RequestDispatcher();
        this.requestOrderingService = new RequestOrderingService(this.requestDispatcher::processRequest);
        this.connectionManager = new ConnectionManager(THREAD_POOL_SIZE, lamportClock,
                this.requestOrderingService::submitRequest);

        // Initialize data expiry service
        this.dataExpiryService = new DataExpiryService(
                EXPIRY_TIME_MS, CLEANUP_INTERVAL_MS,
                weatherDataService.getInternalStorage(),
                weatherDataService.getWriteLock(),
                this::saveDataToFile
        );

        // Register request handlers
        setupRequestHandlers();
    }

    // Main method that gets called when you run the server
    public static void main(String[] args) {
        int port = parsePortFromArgs(args);

        AggregationServer server = new AggregationServer();

        // Setup shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutdown signal received");
            server.shutdown();
        }));

        try {
            server.start(port);
        } catch (Exception e) {
            System.out.println("Server failed to start: " + e.getMessage());
            System.exit(1);
        }
    }

    // Starts the server on the given port and begins accepting connections
    public void start(int port) throws IOException {
        if (isRunning) {
            System.out.println("Server already running");
            return;
        }

        System.out.println("\nStarting aggregation server");
        System.out.println("Starting on port " + port);
        System.out.println("Initial time: " + lamportClock.getTime());

        // Initialize all services
        initializeServices();

        // Load existing data
        loadPersistedData();

        // Start server socket
        serverSocket = new ServerSocket(port);
        isRunning = true;

        System.out.println("Server listening on port " + port);
        System.out.println("Press Ctrl+C to stop\n");

        // Main server loop
        runServerLoop();
    }

    // Shuts down the server properly and saves all data
    public void shutdown() {
        if (!isRunning) {
            return;
        }

        System.out.println("\nShutting down server");
        isRunning = false;

        // Stop accepting new connections
        closeServerSocket();

        // Shutdown services in reverse dependency order
        shutdownServices();

        // Final data save
        saveDataToFile();

        System.out.println("Server shutdown complete\n");
    }

    // Sets up the handlers for PUT and GET requests
    private void setupRequestHandlers() {
        // Create handlers with dependencies
        PutRequestHandler putHandler = new PutRequestHandler(weatherDataService, this::saveDataToFile);
        GetRequestHandler getHandler = new GetRequestHandler(weatherDataService);

        // Register handlers with dispatcher
        requestDispatcher.registerHandler("PUT", putHandler);
        requestDispatcher.registerHandler("GET", getHandler);

        System.out.println("Handlers configured for " +
                requestDispatcher.getHandlerCount() + " HTTP methods");
    }

    // Starts all the background services
    private void initializeServices() {
        connectionManager.start();
        requestOrderingService.start();
        dataExpiryService.start();

        System.out.println("All services started");
    }

    // Loads weather data from the file when the server starts
    private void loadPersistedData() {
        System.out.println("Loading weather data from file");

        WeatherData[] weatherData = weatherDataRepository.load();
        if (weatherData.length > 0) {
            long currentTime = System.currentTimeMillis();
            weatherDataService.loadWeatherData(weatherData, currentTime);
        }

        System.out.println("Loaded " + weatherDataService.getStationCount() +
                " weather stations from " + DATA_FILE);
    }

    // Main loop that accepts client connections
    private void runServerLoop() {
        while (isRunning && !serverSocket.isClosed()) {
            try {
                var clientSocket = serverSocket.accept();
                connectionManager.handleConnection(clientSocket);

            } catch (IOException e) {
                if (isRunning) {
                    System.out.println("Error accepting connection: " + e.getMessage());
                }
                // If not running, this is expected during shutdown
            }
        }
    }

    // Saves current weather data to the file
    private void saveDataToFile() {
        try {
            WeatherData[] allData = weatherDataService.getAllWeatherData();
            weatherDataRepository.saveAsync(allData);
        } catch (Exception e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }

    // Stops all services in the right order when shutting down
    private void shutdownServices() {
        // Stop services that generate new work first
        dataExpiryService.stop();
        requestOrderingService.stop();
        connectionManager.shutdown();

        // Close repository last
        weatherDataRepository.shutdown();
    }

    // Closes the server socket properly
    private void closeServerSocket() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing socket: " + e.getMessage());
        }
    }

    // Gets the port number from command line arguments
    private static int parsePortFromArgs(String[] args) {
        if (args.length > 0) {
            try {
                return Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port " + args[0] + ", using default " + DEFAULT_PORT);
            }
        }
        return DEFAULT_PORT;
    }


}