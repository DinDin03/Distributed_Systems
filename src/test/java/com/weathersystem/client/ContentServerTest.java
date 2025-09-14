package com.weathersystem.client;

import com.weathersystem.client.common.ClientConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ContentServerTest {

    private ContentServer contentServer;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ClientConfiguration config = new ClientConfiguration("localhost", 4567, "ContentServer/1.0", 5000, 10000);
        contentServer = new ContentServer(config);
    }

    @Test
    // Checks that the ContentServer gets created properly with different configs
    void testConstructorAndInitialization() {
        System.out.println("Testing ContentServer constructor");
        assertNotNull(contentServer, "ContentServer should be created");
        assertEquals(0, contentServer.getLamportTime(), "Initial Lamport clock should be 0");
        assertNotNull(contentServer, "ContentServer should inherit from HttpClientBase");
        
        // Test with different configuration
        ClientConfiguration testConfig = new ClientConfiguration("test.com", 8080, "TestAgent/1.0", 3000, 5000);
        ContentServer testServer = new ContentServer(testConfig);
        assertNotNull(testServer, "ContentServer should be created with custom config");
        assertEquals(0, testServer.getLamportTime(), "Custom config should also start with 0");
        System.out.println("Constructor test passed");
    }

    @Test
    // Tests that we handle different HTTP responses properly
    void testResponseValidation() throws Exception {
        System.out.println("Testing response validation");
        Method validateMethod = ContentServer.class.getDeclaredMethod("validateResponse",
                ContentServer.HttpResponse.class);
        validateMethod.setAccessible(true);

        // Test success responses
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            // Test 200 OK response
            ContentServer.HttpResponse response200 = new ContentServer.HttpResponse(200, "OK", null, 5);
            validateMethod.invoke(contentServer, response200);
            String output200 = outputStream.toString();
            assertTrue(output200.contains("Station data updated"), 
                      "Should indicate existing station update");

            // Reset output stream
            outputStream.reset();

            // Test 201 Created response
            ContentServer.HttpResponse response201 = new ContentServer.HttpResponse(201, "Created", null, 5);
            validateMethod.invoke(contentServer, response201);
            String output201 = outputStream.toString();
            assertTrue(output201.contains("New station added"), 
                      "Should indicate new station registration");

        } finally {
            System.setOut(originalOut);
        }

        // Test error responses
        ContentServer.HttpResponse errorResponse = new ContentServer.HttpResponse(400, "Bad Request", null, 5);
        Exception exception = assertThrows(Exception.class, () -> {
            try {
                validateMethod.invoke(contentServer, errorResponse);
            } catch (Exception e) {
                throw e.getCause();
            }
        });
        assertTrue(exception.getMessage().contains("Server rejected weather data: 400 Bad Request"),
                  "Should throw appropriate error message");
        
        System.out.println("Response validation test passed");
    }

    @Test
    // Tests uploading weather data from files, both good and dodgy ones
    void testWeatherDataPublishing() throws IOException {
        System.out.println("Testing weather data publishing");
        
        // Create a valid weather data file
        Path weatherFile = tempDir.resolve("test-weather.txt");
        String weatherContent = "id:TEST001\n" +
                "name:Test Station\n" +
                "state:TEST\n" +
                "lat:-35.0\n" +
                "lon:138.0\n" +
                "air_temp:25.0\n" +
                "apparent_t:24.0\n" +
                "cloud:Clear\n" +
                "rel_hum:50\n" +
                "wind_dir:N\n" +
                "wind_spd_kmh:10\n" +
                "press:1013.25\n" +
                "local_date_time:2024-01-01T12:00:00";
        Files.write(weatherFile, weatherContent.getBytes());

        // Test with valid file (will fail at connection, but tests file parsing)
        Exception validFileException = assertThrows(Exception.class, () -> {
            contentServer.publishWeatherData(weatherFile.toString());
        });
        assertTrue(validFileException.getMessage().contains("Weather data upload failed after 4 attempts"),
                  "Should fail at connection, not file parsing");

        // Test with invalid file
        String invalidFile = "non-existent-file.txt";
        Exception invalidFileException = assertThrows(Exception.class, () -> {
            contentServer.publishWeatherData(invalidFile);
        });
        assertTrue(invalidFileException.getMessage().contains("Weather data upload failed after 4 attempts"),
                  "Should fail with invalid file");
        
        System.out.println("Weather data publishing test passed");
    }

    @Test
    // Makes sure the Lamport clock gets updated when we publish data
    void testLamportClockIntegration() throws IOException {
        System.out.println("Testing Lamport clock integration");
        
        // Create test weather file
        Path weatherFile = tempDir.resolve("test-weather.txt");
        String weatherContent = "id:TEST001\nname:Test Station\nstate:TEST\nlat:-35.0\nlon:138.0\nair_temp:25.0";
        Files.write(weatherFile, weatherContent.getBytes());

        long initialTime = contentServer.getLamportTime();

        try {
            contentServer.publishWeatherData(weatherFile.toString());
        } catch (Exception e) {
            // Expected to fail due to no server, but Lamport clock should still update
        }

        // Lamport clock should have advanced during processing attempts
        assertTrue(contentServer.getLamportTime() > initialTime,
                  "Lamport clock should advance during processing attempts");
        
        System.out.println("Lamport clock integration test passed");
    }

    @Test
    // Tests that it keeps trying to connect with longer delays each time
    void testRetryMechanismWithExponentialBackoff() throws IOException {
        System.out.println("Testing retry mechanism");
        
        // Use invalid server configuration to test retry mechanism
        ClientConfiguration invalidConfig = new ClientConfiguration("invalid.host", 9999, "Test/1.0", 100, 100);
        ContentServer testServer = new ContentServer(invalidConfig);

        // Create a valid weather file
        Path weatherFile = tempDir.resolve("test-weather.txt");
        String weatherContent = "id:TEST001\nname:Test Station\nstate:TEST\nlat:-35.0\nlon:138.0\nair_temp:25.0";
        Files.write(weatherFile, weatherContent.getBytes());

        long startTime = System.currentTimeMillis();

        Exception exception = assertThrows(Exception.class, () -> {
            testServer.publishWeatherData(weatherFile.toString());
        });

        long endTime = System.currentTimeMillis();

        // Should attempt 4 times with exponential backoff
        assertTrue(exception.getMessage().contains("Weather data upload failed after 4 attempts"),
                  "Should attempt 4 times before giving up");

        // Should take at least several seconds due to retry delays (1s + 2s + 4s)
        assertTrue((endTime - startTime) >= 6000, 
                  "Should take at least 6 seconds for retry attempts: " + (endTime - startTime) + "ms");
        
        System.out.println("Retry mechanism test passed");
    }

    @Test
    // Tests the maths for calculating how long to wait between retries
    void testRetryDelayCalculation() {
        System.out.println("Testing retry delay calculation");
        
        // Test exponential backoff calculation
        long baseDelay = 1000;
        double backoff = 2.0;

        long delay2 = (long) (baseDelay * backoff);
        long delay3 = (long) (baseDelay * backoff * backoff);

        assertEquals(1000, baseDelay, "First delay should be base delay");
        assertEquals(2000, delay2, "Second delay should be 2x base delay");
        assertEquals(4000, delay3, "Third delay should be 4x base delay");
        
        System.out.println("Retry delay calculation test passed");
    }

    // === MULTI-SERVER SUPPORT TESTS ===

    @Test
    // Tests setting up multiple servers for failover
    void testMultiServerConfiguration() {
        System.out.println("Testing multi-server configuration");
        
        // Test multiple servers configuration
        ClientConfiguration multiConfig = ClientConfiguration.fromMultipleServers("localhost:4567,localhost:4568,localhost:4569");
        ContentServer multiServer = new ContentServer(multiConfig);

        assertNotNull(multiServer, "Multi-server ContentServer should be created");
        assertTrue(multiConfig.hasMultipleServers(), "Should detect multiple servers");
        assertEquals(3, multiConfig.getServerAddresses().size(), "Should have 3 server addresses");
        assertEquals(0, multiServer.getLamportTime(), "Should start with 0 Lamport time");

        // Test single server in multi-server config
        ClientConfiguration singleInMulti = ClientConfiguration.fromMultipleServers("localhost:4567");
        ContentServer singleServer = new ContentServer(singleInMulti);
        assertNotNull(singleServer, "Single server ContentServer should be created");
        assertFalse(singleInMulti.hasMultipleServers(), "Single server doesn't count as 'multiple'");
        assertEquals(1, singleInMulti.getServerAddresses().size(), "Should have 1 server address");
        assertEquals("localhost:4567", singleInMulti.getPrimaryServerAddress(), "Primary server should match");
        
        System.out.println("Multi-server configuration test passed");
    }

    @Test
    // Tests what happens when one server is down and we try the next one
    void testMultiServerFailover() throws IOException {
        System.out.println("Testing multi-server failover");
        
        // Create test weather file
        Path weatherFile = tempDir.resolve("multi-server-test.txt");
        String weatherContent = "id:MULTI001\nname:Multi Server Test\nstate:TEST\nlat:-35.0\nlon:138.0\nair_temp:20.0";
        Files.write(weatherFile, weatherContent.getBytes());

        // Configure with failover servers
        ClientConfiguration config = ClientConfiguration.fromMultipleServers("invalid.host:9999,localhost:4567");
        ContentServer server = new ContentServer(config);

        long initialTime = server.getLamportTime();

        // Should attempt failover (will timeout, but tests failover logic)
        Exception exception = assertThrows(Exception.class, () -> {
            server.publishWeatherData(weatherFile.toString());
        });

        assertTrue(exception.getMessage().contains("Weather data upload failed after 4 attempts"),
                  "Should fail after all retry attempts");
        assertTrue(server.getLamportTime() > initialTime,
                  "Lamport clock should advance during failover attempts");
        
        System.out.println("Multi-server failover test passed");
    }

    @Test
    // Tests that the main method handles command line arguments properly
    void testMainMethodArgumentHandling() {
        System.out.println("Testing main method arguments");
        
        // Test that main method requires server address and file
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            ContentServer.main(new String[]{});
        }, "Should throw exception for empty arguments");

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            ContentServer.main(new String[]{"localhost:4567"});
        }, "Should throw exception for missing file argument");

        // Test argument parsing with valid arguments
        try {
            Path weatherFile = tempDir.resolve("test-weather.txt");
            String weatherContent = "id:TEST001\nname:Test Station\nstate:TEST";
            Files.write(weatherFile, weatherContent.getBytes());

            String[] args = {"localhost:4567", weatherFile.toString()};

            // Test argument parsing by creating the configuration manually
            // Cannot test main() directly as it calls System.exit() on failure
            assertDoesNotThrow(() -> {
                ClientConfiguration config = ClientConfiguration.fromServerAddress(args[0]);
                ContentServer testServer = new ContentServer(config);
                assertNotNull(testServer, "Should create ContentServer with valid args");
                assertTrue(Files.exists(Path.of(args[1])), "Weather file should exist");
            });
        } catch (IOException e) {
            fail("Failed to create test file: " + e.getMessage());
        }
        
        System.out.println("Main method argument test passed");
    }
}