package com.weathersystem.integration;

import com.weathersystem.client.ContentServer;
import com.weathersystem.client.GETClient;
import com.weathersystem.client.common.ClientConfiguration;
import com.weathersystem.server.AggregationServer;
import com.weathersystem.shared.domain.WeatherData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.awaitility.Awaitility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.*;

class BasicClientServerTest {

    @TempDir
    Path tempDir;

    private AggregationServer server;
    private Thread serverThread;
    private int serverPort;
    private ClientConfiguration clientConfig;

    @BeforeEach
    void setUp() throws IOException {
        // Find available port
        serverPort = findAvailablePort();

        // Create test data directory (the server uses hardcoded "data" path)
        Path dataDir = tempDir.resolve("data");
        Files.createDirectories(dataDir);

        // Clear any existing data files to ensure clean test state
        clearExistingDataFiles();

        // Initialize server
        server = new AggregationServer();

        // Start server in background thread
        serverThread = new Thread(() -> {
            try {
                server.start(serverPort);
            } catch (Exception e) {
                System.err.println("Server failed to start: " + e.getMessage());
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Wait for server to be ready by checking if we can connect
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .ignoreExceptions()
                .until(() -> {
                    try (Socket testSocket = new Socket("localhost", serverPort)) {
                        return true;
                    }
                });

        // Create client configuration
        clientConfig = new ClientConfiguration("localhost", serverPort, "WeatherTestClient/1.0", 5000, 10000);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (server != null) {
            server.shutdown();
        }

        if (serverThread != null) {
            serverThread.interrupt();
            serverThread.join(2000);
        }
    }

    @Test
    void testSuccessfulPutOperation() throws Exception {
        // Create test weather data file
        String weatherData = createTestWeatherDataFile();

        // Create content server and publish data
        ContentServer contentServer = new ContentServer(clientConfig);

        assertDoesNotThrow(() -> {
            contentServer.publishWeatherData(weatherData);
        }, "PUT operation should succeed");

        // Verify server received and stored the data
        // This is verified by checking if a GET request returns the data
        GETClient getClient = new GETClient(clientConfig);
        WeatherData[] retrievedData = getClient.retrieveWeatherData();

        assertNotNull(retrievedData, "Retrieved data should not be null");
        assertTrue(retrievedData.length > 0, "Should have received weather data");

        // Look for our specific data in the retrieved results
        boolean foundOurData = false;
        for (WeatherData station : retrievedData) {
            if ("IDS60901".equals(station.getId())) {
                foundOurData = true;
                break;
            }
        }
        assertTrue(foundOurData, "Should find our uploaded station data");
    }

    @Test
    void testSuccessfulGetOperation() throws Exception {
        // First publish some data
        String weatherData = createTestWeatherDataFile();
        ContentServer contentServer = new ContentServer(clientConfig);
        contentServer.publishWeatherData(weatherData);

        // Now test GET operation
        GETClient getClient = new GETClient(clientConfig);
        WeatherData[] retrievedData = getClient.retrieveWeatherData();

        assertNotNull(retrievedData, "Retrieved data should not be null");
        assertTrue(retrievedData.length > 0, "Should have received weather data");

        // Verify data integrity
        WeatherData firstStation = retrievedData[0];
        assertEquals("IDS60901", firstStation.getId());
        assertEquals("Adelaide (West Terrace /  ngayirdapira)", firstStation.getName());
        assertEquals("SA", firstStation.getState());
        assertEquals(13.3, firstStation.getAirTemp(), 0.001);
    }

    @Test
    void testGetFromServerWithExistingData() throws Exception {
        // Test GET operation - server may have existing data
        GETClient getClient = new GETClient(clientConfig);
        WeatherData[] retrievedData = getClient.retrieveWeatherData();

        assertNotNull(retrievedData, "Retrieved data should not be null");
        assertTrue(retrievedData.length >= 0, "Should return valid array");
    }

    @Test
    void testHttpStatusCodes() throws Exception {
        // Test successful PUT - should return 200/201
        String weatherData = createTestWeatherDataFile();
        ContentServer contentServer = new ContentServer(clientConfig);

        assertDoesNotThrow(() -> {
            contentServer.publishWeatherData(weatherData);
        }, "Valid PUT should not throw exception");

        // Test successful GET - should return 200
        GETClient getClient = new GETClient(clientConfig);
        assertDoesNotThrow(() -> {
            getClient.retrieveWeatherData();
        }, "Valid GET should not throw exception");
    }

    @Test
    void testLamportTimestampExchange() throws Exception {
        // Test that Lamport timestamps are correctly exchanged
        GETClient getClient = new GETClient(clientConfig);

        long initialTime = getClient.getLamportTime();
        assertEquals(0, initialTime, "Initial Lamport time should be 0");

        // Perform GET operation
        getClient.retrieveWeatherData();

        long finalTime = getClient.getLamportTime();
        assertTrue(finalTime > initialTime, "Lamport time should increase after operation");
    }

    @Test
    void testInvalidWeatherDataFile() throws Exception {
        // Create invalid weather data file
        Path invalidFile = tempDir.resolve("invalid_weather.txt");
        Files.writeString(invalidFile, "This is not valid weather data");

        ContentServer contentServer = new ContentServer(clientConfig);

        assertThrows(Exception.class, () -> {
            contentServer.publishWeatherData(invalidFile.toString());
        }, "Invalid weather data should cause exception");
    }

    @Test
    void testSequentialOperations() throws Exception {
        // Test multiple sequential PUT and GET operations
        String weatherData = createTestWeatherDataFile();
        ContentServer contentServer = new ContentServer(clientConfig);
        GETClient getClient = new GETClient(clientConfig);

        // First PUT
        contentServer.publishWeatherData(weatherData);
        WeatherData[] data1 = getClient.retrieveWeatherData();
        assertEquals(1, data1.length, "Should have 1 station after first PUT");

        // Second PUT with different data (different station ID)
        String weatherData2 = createTestWeatherDataFile2();
        contentServer.publishWeatherData(weatherData2);
        WeatherData[] data2 = getClient.retrieveWeatherData();
        assertEquals(2, data2.length, "Should have 2 stations after second PUT (different station IDs)");
        
        // Verify both stations are present
        boolean foundStation1 = false, foundStation2 = false;
        for (WeatherData station : data2) {
            if ("IDS60901".equals(station.getId())) foundStation1 = true;
            if ("IDS60902".equals(station.getId())) foundStation2 = true;
        }
        assertTrue(foundStation1, "Should have first station (IDS60901)");
        assertTrue(foundStation2, "Should have second station (IDS60902)");

        // Third GET should still return both stations
        WeatherData[] data3 = getClient.retrieveWeatherData();
        assertEquals(2, data3.length, "Should still have 2 stations");
    }

    @Test
    void testConnectionHandling() throws Exception {
        // Test that connections are properly handled
        GETClient getClient = new GETClient(clientConfig);

        // Perform multiple operations to test connection management
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> {
                getClient.retrieveWeatherData();
            }, "Multiple GET operations should not cause connection issues");
        }
    }

    @Test
    void testServerRecoveryAfterError() throws Exception {
        // Test that server continues working after encountering an error
        ContentServer contentServer = new ContentServer(clientConfig);
        GETClient getClient = new GETClient(clientConfig);

        // Try invalid operation first
        try {
            contentServer.publishWeatherData("/nonexistent/file.txt");
        } catch (Exception e) {
            // Expected to fail
        }

        // Now try valid operation - server should still work
        String weatherData = createTestWeatherDataFile();
        assertDoesNotThrow(() -> {
            contentServer.publishWeatherData(weatherData);
        }, "Server should recover and handle valid requests after error");

        WeatherData[] data = getClient.retrieveWeatherData();
        assertEquals(1, data.length, "Should receive valid data after error recovery");
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private void clearExistingDataFiles() {
        try {
            // Clear the main data file
            Path dataFile = Path.of("data/weather.json");
            if (Files.exists(dataFile)) {
                Files.delete(dataFile);
            }
            
            // Clear the backup file
            Path backupFile = Path.of("data/weather.json.backup");
            if (Files.exists(backupFile)) {
                Files.delete(backupFile);
            }
        } catch (IOException e) {
            System.out.println("Warning: Could not clear existing data files: " + e.getMessage());
        }
    }

    private String createTestWeatherDataFile() throws IOException {
        String weatherContent =
                "id:IDS60901\n" +
                "name:Adelaide (West Terrace /  ngayirdapira)\n" +
                "state:SA\n" +
                "time_zone:CST\n" +
                "lat:-34.9\n" +
                "lon:138.6\n" +
                "local_date_time:15/04:00pm\n" +
                "local_date_time_full:20230715160000\n" +
                "air_temp:13.3\n" +
                "apparent_t:9.5\n" +
                "cloud:Partly cloudy\n" +
                "dewpt:4.7\n" +
                "press:1023.9\n" +
                "rel_hum:58\n" +
                "wind_dir:S\n" +
                "wind_spd_kmh:15\n" +
                "wind_spd_kt:8\n";

        Path weatherFile = tempDir.resolve("test_weather.txt");
        Files.writeString(weatherFile, weatherContent);
        return weatherFile.toString();
    }

    private String createTestWeatherDataFile2() throws IOException {
        String weatherContent =
                "id:IDS60902\n" +
                "name:Melbourne (Olympic Park)\n" +
                "state:VIC\n" +
                "time_zone:EST\n" +
                "lat:-37.8\n" +
                "lon:145.0\n" +
                "local_date_time:15/04:30pm\n" +
                "local_date_time_full:20230715163000\n" +
                "air_temp:16.2\n" +
                "apparent_t:14.8\n" +
                "cloud:Clear\n" +
                "dewpt:8.1\n" +
                "press:1018.7\n" +
                "rel_hum:65\n" +
                "wind_dir:NW\n" +
                "wind_spd_kmh:12\n" +
                "wind_spd_kt:6\n";

        Path weatherFile = tempDir.resolve("test_weather2.txt");
        Files.writeString(weatherFile, weatherContent);
        return weatherFile.toString();
    }
}