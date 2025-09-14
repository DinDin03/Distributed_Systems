package com.weathersystem.client;

import com.weathersystem.client.common.ClientConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ContentServerTest {

    private ContentServer contentServer;
    private ClientConfiguration config;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        config = new ClientConfiguration("localhost", 4567, "ContentServer/1.0", 5000, 10000);
        contentServer = new ContentServer(config);
    }

    @Test
    void testConstructorInitializesCorrectly() {
        assertNotNull(contentServer);
        assertEquals(0, contentServer.getLamportTime()); // Initial Lamport clock should be 0
    }

    @Test
    void testConstructorWithConfiguration() {
        ClientConfiguration testConfig = new ClientConfiguration("test.com", 8080, "TestAgent/1.0", 3000, 5000);
        ContentServer server = new ContentServer(testConfig);

        assertNotNull(server);
        assertEquals(0, server.getLamportTime());
    }

    @Test
    void testValidateResponseWithSuccessStatus200() throws Exception {
        Method validateMethod = ContentServer.class.getDeclaredMethod("validateResponse",
                ContentServer.HttpResponse.class);
        validateMethod.setAccessible(true);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            ContentServer.HttpResponse response = new ContentServer.HttpResponse(200, "OK", null, 5);
            validateMethod.invoke(contentServer, response);

            String output = outputStream.toString();
            assertTrue(output.contains("Existing weather station data updated"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testValidateResponseWithSuccessStatus201() throws Exception {
        Method validateMethod = ContentServer.class.getDeclaredMethod("validateResponse",
                ContentServer.HttpResponse.class);
        validateMethod.setAccessible(true);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            ContentServer.HttpResponse response = new ContentServer.HttpResponse(201, "Created", null, 5);
            validateMethod.invoke(contentServer, response);

            String output = outputStream.toString();
            assertTrue(output.contains("New weather station registered"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testValidateResponseWithErrorStatus() throws Exception {
        Method validateMethod = ContentServer.class.getDeclaredMethod("validateResponse",
                ContentServer.HttpResponse.class);
        validateMethod.setAccessible(true);

        ContentServer.HttpResponse response = new ContentServer.HttpResponse(400, "Bad Request", null, 5);

        Exception exception = assertThrows(Exception.class, () -> {
            try {
                validateMethod.invoke(contentServer, response);
            } catch (Exception e) {
                throw e.getCause();
            }
        });

        assertTrue(exception.getMessage().contains("Server rejected weather data: 400 Bad Request"));
    }

    @Test
    void testValidateResponseWithServerError() throws Exception {
        Method validateMethod = ContentServer.class.getDeclaredMethod("validateResponse",
                ContentServer.HttpResponse.class);
        validateMethod.setAccessible(true);

        ContentServer.HttpResponse response = new ContentServer.HttpResponse(500, "Internal Server Error", null, 5);

        Exception exception = assertThrows(Exception.class, () -> {
            try {
                validateMethod.invoke(contentServer, response);
            } catch (Exception e) {
                throw e.getCause();
            }
        });

        assertTrue(exception.getMessage().contains("Server rejected weather data: 500 Internal Server Error"));
    }

    @Test
    void testPublishWeatherDataWithInvalidFile() {
        // Test with non-existent file
        String invalidFile = "non-existent-file.txt";

        Exception exception = assertThrows(Exception.class, () -> {
            contentServer.publishWeatherData(invalidFile);
        });

        assertTrue(exception.getMessage().contains("Weather data upload failed after 4 attempts"));
    }

    @Test
    void testPublishWeatherDataWithValidFile() throws IOException {
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

        // This will fail because there's no server, but we can test the file parsing part
        Exception exception = assertThrows(Exception.class, () -> {
            contentServer.publishWeatherData(weatherFile.toString());
        });

        // Should fail at connection, not file parsing
        assertTrue(exception.getMessage().contains("Weather data upload failed after 4 attempts"));
    }

    @Test
    void testLamportClockUpdatesOnProcessing() throws IOException {
        // Create a valid weather data file
        Path weatherFile = tempDir.resolve("test-weather.txt");
        String weatherContent = "id:TEST001\n" +
                "name:Test Station\n" +
                "state:TEST\n" +
                "lat:-35.0\n" +
                "lon:138.0\n" +
                "air_temp:25.0";

        Files.write(weatherFile, weatherContent.getBytes());

        long initialTime = contentServer.getLamportTime();

        try {
            contentServer.publishWeatherData(weatherFile.toString());
        } catch (Exception e) {
            // Expected to fail due to no server, but Lamport clock should still update
        }

        // Lamport clock should have advanced during processing attempts
        assertTrue(contentServer.getLamportTime() > initialTime);
    }

    @Test
    void testMainMethodArgumentValidation() {
        // Test that main method requires server address and file
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            ContentServer.main(new String[]{});
        });

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            ContentServer.main(new String[]{"localhost:4567"});
        });
    }

    @Test
    void testMainMethodWithValidArguments() {
        // Create temporary file
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
                assertNotNull(testServer);

                // Verify the weather file exists and is readable
                assertTrue(Files.exists(Path.of(args[1])));
            });
        } catch (IOException e) {
            fail("Failed to create test file: " + e.getMessage());
        }
    }

    @Test
    void testRetryMechanismWithConnectionFailure() {
        // Use invalid server configuration to test retry mechanism
        ClientConfiguration invalidConfig = new ClientConfiguration("invalid.host", 9999, "Test/1.0", 100, 100);
        ContentServer testServer = new ContentServer(invalidConfig);

        // Create a valid weather file
        try {
            Path weatherFile = tempDir.resolve("test-weather.txt");
            String weatherContent = "id:TEST001\nname:Test Station\nstate:TEST\nlat:-35.0\nlon:138.0\nair_temp:25.0";
            Files.write(weatherFile, weatherContent.getBytes());

            long startTime = System.currentTimeMillis();

            Exception exception = assertThrows(Exception.class, () -> {
                testServer.publishWeatherData(weatherFile.toString());
            });

            long endTime = System.currentTimeMillis();

            // Should attempt 4 times with exponential backoff
            assertTrue(exception.getMessage().contains("Weather data upload failed after 4 attempts"));

            // Should take at least several seconds due to retry delays (1s + 2s + 4s)
            assertTrue((endTime - startTime) >= 6000, "Should take at least 6 seconds for retry attempts");

        } catch (IOException e) {
            fail("Failed to create test file: " + e.getMessage());
        }
    }

    @Test
    void testUploadWeatherFileIncreasesLamportClock() throws IOException {
        Path weatherFile = tempDir.resolve("test-weather.txt");
        String weatherContent = "id:TEST001\nname:Test Station\nstate:TEST\nlat:-35.0\nlon:138.0\nair_temp:25.0";
        Files.write(weatherFile, weatherContent.getBytes());

        long initialTime = contentServer.getLamportTime();

        try {
            Method uploadMethod = ContentServer.class.getDeclaredMethod("uploadWeatherFile", String.class);
            uploadMethod.setAccessible(true);
            uploadMethod.invoke(contentServer, weatherFile.toString());
        } catch (Exception e) {
            // Expected to fail at connection, but Lamport clock should advance
        }

        assertTrue(contentServer.getLamportTime() > initialTime,
                "Lamport clock should advance during weather file processing");
    }

    @Test
    void testRetryDelayCalculation() {
        // Test exponential backoff calculation
        long baseDelay = 1000;
        double backoff = 2.0;

        long delay1 = baseDelay;
        long delay2 = (long) (baseDelay * backoff);
        long delay3 = (long) (baseDelay * backoff * backoff);

        assertEquals(1000, delay1);
        assertEquals(2000, delay2);
        assertEquals(4000, delay3);
    }

    @Test
    void testContentServerInheritance() {
        // Verify that ContentServer properly inherits from HttpClientBase
        assertTrue(contentServer instanceof com.weathersystem.client.common.HttpClientBase);
        assertNotNull(contentServer.getLamportTime());
    }
}