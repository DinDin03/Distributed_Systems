package com.weathersystem.client.common;

import com.weathersystem.shared.clock.LamportClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class ResponseParserTest {

    private ResponseParser responseParser;
    private LamportClock lamportClock;

    @BeforeEach
    void setUp() {
        lamportClock = new LamportClock();
        responseParser = new ResponseParser(lamportClock);
    }

    @Test
    // Tests parsing of valid HTTP response with status code, headers, and content
    void testParseValidResponse() throws IOException {
        System.out.println("Testing parsing of valid HTTP response...");
        String responseData = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: 13\r\n" +
                "Lamport-Time: 5\r\n" +
                "\r\n" +
                "Hello, World!";

        BufferedReader reader = new BufferedReader(new StringReader(responseData));
        HttpClientBase.HttpResponse response = responseParser.parseResponse(reader);

        assertEquals(200, response.getStatusCode(), "Status code should be 200");
        assertEquals("OK", response.getStatusText(), "Status text should be OK");
        assertEquals("Hello, World!", response.getContent(), "Content should match");
        assertEquals(5, response.getServerLamportTime(), "Lamport time should be parsed");
        assertTrue(response.isSuccess(), "Response should be successful");
        System.out.println("✓ Valid response parsing test passed");
    }

    @Test
    // Tests parsing various HTTP response types including success, error, and no content responses
    void testParseDifferentResponseTypes() throws IOException {
        System.out.println("Testing parsing of different response types...");
        
        // Test success response with content
        String successData = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: 4\r\n" +
                "Lamport-Time: 3\r\n" +
                "\r\n" +
                "Test";
        BufferedReader reader1 = new BufferedReader(new StringReader(successData));
        HttpClientBase.HttpResponse successResponse = responseParser.parseResponse(reader1);
        assertEquals(200, successResponse.getStatusCode());
        assertEquals("Test", successResponse.getContent());
        assertTrue(successResponse.isSuccess());

        // Test error response
        String errorData = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Length: 9\r\n" +
                "Lamport-Time: 7\r\n" +
                "\r\n" +
                "Not Found";
        BufferedReader reader2 = new BufferedReader(new StringReader(errorData));
        HttpClientBase.HttpResponse errorResponse = responseParser.parseResponse(reader2);
        assertEquals(404, errorResponse.getStatusCode());
        assertEquals("Not Found", errorResponse.getContent());
        assertFalse(errorResponse.isSuccess());

        // Test no content response
        String noContentData = "HTTP/1.1 204 No Content\r\n" +
                "Lamport-Time: 10\r\n" +
                "\r\n";
        BufferedReader reader3 = new BufferedReader(new StringReader(noContentData));
        HttpClientBase.HttpResponse noContentResponse = responseParser.parseResponse(reader3);
        assertEquals(204, noContentResponse.getStatusCode());
        assertNull(noContentResponse.getContent());
        assertTrue(noContentResponse.isSuccess());
        
        System.out.println("✓ Different response types test passed");
    }

    @Test
    // Tests parsing HTTP response without Lamport time header and verifies default handling
    void testParseResponseWithoutLamportTime() throws IOException {
        System.out.println("Testing parsing response without Lamport time...");
        String responseData = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: 4\r\n" +
                "\r\n" +
                "Test";

        BufferedReader reader = new BufferedReader(new StringReader(responseData));
        HttpClientBase.HttpResponse response = responseParser.parseResponse(reader);

        assertEquals(200, response.getStatusCode(), "Status code should be 200");
        assertEquals("Test", response.getContent(), "Content should match");
        assertEquals(-1, response.getServerLamportTime(), "Lamport time should be -1 when not present");
        System.out.println("✓ Response without Lamport time test passed");
    }

    @Test
    // Tests that parsing response updates the Lamport clock with server timestamp
    void testParseResponseUpdatesLamportClock() throws IOException {
        System.out.println("Testing Lamport clock update from response...");
        lamportClock.tick(); // Set to 1
        lamportClock.tick(); // Set to 2

        String responseData = "HTTP/1.1 200 OK\r\n" +
                "Lamport-Time: 5\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(responseData));
        HttpClientBase.HttpResponse response = responseParser.parseResponse(reader);

        // Clock should be updated to max(2, 5) + 1 = 6
        assertTrue(lamportClock.getTime() > 5, "Clock should be updated to value greater than server time");
        assertEquals(5, response.getServerLamportTime(), "Server Lamport time should be preserved");
        System.out.println("✓ Lamport clock update test passed");
    }

    @Test
    // Tests error handling for various invalid HTTP response formats
    void testParseInvalidResponses() {
        System.out.println("Testing parsing of invalid responses...");
        
        // Test empty response
        BufferedReader emptyReader = new BufferedReader(new StringReader(""));
        assertThrows(IOException.class, () -> responseParser.parseResponse(emptyReader),
                "Should throw IOException for empty response");

        // Test invalid status code
        String invalidStatusData = "HTTP/1.1 INVALID OK\r\n\r\n";
        BufferedReader invalidStatusReader = new BufferedReader(new StringReader(invalidStatusData));
        assertThrows(NumberFormatException.class, () -> responseParser.parseResponse(invalidStatusReader),
                "Should throw NumberFormatException for invalid status code");

        // Test invalid content length
        String invalidContentLengthData = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: INVALID\r\n" +
                "\r\n";
        BufferedReader invalidContentLengthReader = new BufferedReader(new StringReader(invalidContentLengthData));
        assertThrows(NumberFormatException.class, () -> responseParser.parseResponse(invalidContentLengthReader),
                "Should throw NumberFormatException for invalid content length");

        // Test invalid Lamport time
        String invalidLamportData = "HTTP/1.1 200 OK\r\n" +
                "Lamport-Time: INVALID\r\n" +
                "\r\n";
        BufferedReader invalidLamportReader = new BufferedReader(new StringReader(invalidLamportData));
        assertThrows(NumberFormatException.class, () -> responseParser.parseResponse(invalidLamportReader),
                "Should throw NumberFormatException for invalid Lamport time");

        // Test incomplete content
        String incompleteContentData = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: 10\r\n" +
                "\r\n" +
                "Short"; // Only 5 chars, but declared 10
        BufferedReader incompleteContentReader = new BufferedReader(new StringReader(incompleteContentData));
        assertThrows(IOException.class, () -> responseParser.parseResponse(incompleteContentReader),
                "Should throw IOException for incomplete content");
        
        System.out.println("✓ Invalid responses handling test passed");
    }

    @Test
    // Tests parsing response with malformed headers and verifies graceful handling
    void testParseResponseWithMalformedHeaders() throws IOException {
        System.out.println("Testing parsing with malformed headers...");
        String responseData = "HTTP/1.1 200 OK\r\n" +
                "Invalid-Header-Without-Colon\r\n" +
                "Content-Length: 4\r\n" +
                "\r\n" +
                "Test";

        BufferedReader reader = new BufferedReader(new StringReader(responseData));
        HttpClientBase.HttpResponse response = responseParser.parseResponse(reader);

        assertEquals(200, response.getStatusCode(), "Should still parse status code");
        assertEquals("Test", response.getContent(), "Should still parse content");
        assertEquals(-1, response.getServerLamportTime(), "Should handle missing Lamport time gracefully");
        System.out.println("✓ Malformed headers handling test passed");
    }

    @Test
    // Tests case insensitive header parsing for Content-Length and Lamport-Time
    void testParseResponseWithCaseInsensitiveHeaders() throws IOException {
        System.out.println("Testing case insensitive header parsing...");
        String responseData = "HTTP/1.1 200 OK\r\n" +
                "CONTENT-LENGTH: 4\r\n" +
                "LAMPORT-TIME: 7\r\n" +
                "\r\n" +
                "Test";

        BufferedReader reader = new BufferedReader(new StringReader(responseData));
        HttpClientBase.HttpResponse response = responseParser.parseResponse(reader);

        assertEquals("Test", response.getContent(), "Should parse content with uppercase headers");
        assertEquals(7, response.getServerLamportTime(), "Should parse Lamport time with uppercase header");
        System.out.println("✓ Case insensitive headers test passed");
    }

    @Test
    // Tests HttpResponse isSuccess method with various HTTP status codes
    void testHttpResponseSuccessMethod() {
        System.out.println("Testing HttpResponse isSuccess method...");
        
        // Test various status codes
        HttpClientBase.HttpResponse response200 = new HttpClientBase.HttpResponse(200, "OK", null, -1);
        HttpClientBase.HttpResponse response201 = new HttpClientBase.HttpResponse(201, "Created", null, -1);
        HttpClientBase.HttpResponse response299 = new HttpClientBase.HttpResponse(299, "Custom", null, -1);
        HttpClientBase.HttpResponse response300 = new HttpClientBase.HttpResponse(300, "Redirect", null, -1);
        HttpClientBase.HttpResponse response404 = new HttpClientBase.HttpResponse(404, "Not Found", null, -1);
        HttpClientBase.HttpResponse response500 = new HttpClientBase.HttpResponse(500, "Error", null, -1);

        assertTrue(response200.isSuccess(), "200 should be success");
        assertTrue(response201.isSuccess(), "201 should be success");
        assertTrue(response299.isSuccess(), "299 should be success");
        assertFalse(response300.isSuccess(), "300 should not be success");
        assertFalse(response404.isSuccess(), "404 should not be success");
        assertFalse(response500.isSuccess(), "500 should not be success");
        
        System.out.println("✓ HttpResponse success method test passed");
    }
}