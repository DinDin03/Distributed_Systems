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
    void testParseValidResponse() throws IOException {
        String responseData = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: 13\r\n" +
                "Lamport-Time: 5\r\n" +
                "\r\n" +
                "Hello, World!";

        BufferedReader reader = new BufferedReader(new StringReader(responseData));
        HttpClientBase.HttpResponse response = responseParser.parseResponse(reader);

        assertEquals(200, response.getStatusCode());
        assertEquals("OK", response.getStatusText());
        assertEquals("Hello, World!", response.getContent());
        assertEquals(5, response.getServerLamportTime());
        assertTrue(response.isSuccess());
    }

    @Test
    void testParseResponseWithoutContent() throws IOException {
        String responseData = "HTTP/1.1 204 No Content\r\n" +
                "Lamport-Time: 10\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(responseData));
        HttpClientBase.HttpResponse response = responseParser.parseResponse(reader);

        assertEquals(204, response.getStatusCode());
        assertEquals("No Content", response.getStatusText());
        assertNull(response.getContent());
        assertEquals(10, response.getServerLamportTime());
        assertTrue(response.isSuccess());
    }

    @Test
    void testParseErrorResponse() throws IOException {
        String responseData = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Length: 9\r\n" +
                "Lamport-Time: 3\r\n" +
                "\r\n" +
                "Not Found";

        BufferedReader reader = new BufferedReader(new StringReader(responseData));
        HttpClientBase.HttpResponse response = responseParser.parseResponse(reader);

        assertEquals(404, response.getStatusCode());
        assertEquals("Not Found", response.getStatusText());
        assertEquals("Not Found", response.getContent());
        assertEquals(3, response.getServerLamportTime());
        assertFalse(response.isSuccess());
    }

    @Test
    void testParseResponseWithoutLamportTime() throws IOException {
        String responseData = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: 4\r\n" +
                "\r\n" +
                "Test";

        BufferedReader reader = new BufferedReader(new StringReader(responseData));
        HttpClientBase.HttpResponse response = responseParser.parseResponse(reader);

        assertEquals(200, response.getStatusCode());
        assertEquals("OK", response.getStatusText());
        assertEquals("Test", response.getContent());
        assertEquals(-1, response.getServerLamportTime());
    }

    @Test
    void testParseResponseUpdatesLamportClock() throws IOException {
        lamportClock.tick(); // Set to 1
        lamportClock.tick(); // Set to 2

        String responseData = "HTTP/1.1 200 OK\r\n" +
                "Lamport-Time: 5\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(responseData));
        HttpClientBase.HttpResponse response = responseParser.parseResponse(reader);

        // Clock should be updated to max(2, 5) + 1 = 6
        assertTrue(lamportClock.getTime() > 5);
        assertEquals(5, response.getServerLamportTime());
    }

    @Test
    void testParseResponseWithMalformedHeaders() throws IOException {
        String responseData = "HTTP/1.1 200 OK\r\n" +
                "Invalid-Header-Without-Colon\r\n" +
                "Content-Length: 4\r\n" +
                "\r\n" +
                "Test";

        BufferedReader reader = new BufferedReader(new StringReader(responseData));
        HttpClientBase.HttpResponse response = responseParser.parseResponse(reader);

        assertEquals(200, response.getStatusCode());
        assertEquals("Test", response.getContent());
        assertEquals(-1, response.getServerLamportTime());
    }

    @Test
    void testParseResponseWithNoStatusText() throws IOException {
        String responseData = "HTTP/1.1 200\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(responseData));
        HttpClientBase.HttpResponse response = responseParser.parseResponse(reader);

        assertEquals(200, response.getStatusCode());
        assertEquals("", response.getStatusText());
        assertNull(response.getContent());
    }

    @Test
    void testParseNullResponse() {
        BufferedReader reader = new BufferedReader(new StringReader(""));

        assertThrows(IOException.class, () -> responseParser.parseResponse(reader),
                "Should throw IOException when no response is received");
    }

    @Test
    void testParseResponseWithInvalidStatusCode() {
        String responseData = "HTTP/1.1 INVALID OK\r\n\r\n";
        BufferedReader reader = new BufferedReader(new StringReader(responseData));

        assertThrows(NumberFormatException.class, () -> responseParser.parseResponse(reader),
                "Should throw NumberFormatException for invalid status code");
    }

    @Test
    void testParseResponseWithInvalidContentLength() {
        String responseData = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: INVALID\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(responseData));

        assertThrows(NumberFormatException.class, () -> responseParser.parseResponse(reader),
                "Should throw NumberFormatException for invalid content length");
    }

    @Test
    void testParseResponseWithInvalidLamportTime() {
        String responseData = "HTTP/1.1 200 OK\r\n" +
                "Lamport-Time: INVALID\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(responseData));

        assertThrows(NumberFormatException.class, () -> responseParser.parseResponse(reader),
                "Should throw NumberFormatException for invalid Lamport time");
    }

    @Test
    void testParseResponseWithIncompleteContent() {
        String responseData = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: 10\r\n" +
                "\r\n" +
                "Short"; // Only 5 chars, but declared 10

        BufferedReader reader = new BufferedReader(new StringReader(responseData));

        assertThrows(IOException.class, () -> responseParser.parseResponse(reader),
                "Should throw IOException when content is shorter than declared length");
    }

    @Test
    void testParseResponseWithCaseInsensitiveHeaders() throws IOException {
        String responseData = "HTTP/1.1 200 OK\r\n" +
                "CONTENT-LENGTH: 4\r\n" +
                "LAMPORT-TIME: 7\r\n" +
                "\r\n" +
                "Test";

        BufferedReader reader = new BufferedReader(new StringReader(responseData));
        HttpClientBase.HttpResponse response = responseParser.parseResponse(reader);

        assertEquals("Test", response.getContent());
        assertEquals(7, response.getServerLamportTime());
    }

    @Test
    void testParseResponseWithExtraWhitespace() throws IOException {
        String responseData = "HTTP/1.1 200 OK\r\n" +
                "Content-Length:   4   \r\n" +
                "Lamport-Time:   8   \r\n" +
                "\r\n" +
                "Test";

        BufferedReader reader = new BufferedReader(new StringReader(responseData));
        HttpClientBase.HttpResponse response = responseParser.parseResponse(reader);

        assertEquals("Test", response.getContent());
        assertEquals(8, response.getServerLamportTime());
    }

    @Test
    void testHttpResponseIsSuccessMethod() {
        HttpClientBase.HttpResponse response200 = new HttpClientBase.HttpResponse(200, "OK", null, -1);
        HttpClientBase.HttpResponse response201 = new HttpClientBase.HttpResponse(201, "Created", null, -1);
        HttpClientBase.HttpResponse response299 = new HttpClientBase.HttpResponse(299, "Custom", null, -1);
        HttpClientBase.HttpResponse response300 = new HttpClientBase.HttpResponse(300, "Redirect", null, -1);
        HttpClientBase.HttpResponse response404 = new HttpClientBase.HttpResponse(404, "Not Found", null, -1);
        HttpClientBase.HttpResponse response500 = new HttpClientBase.HttpResponse(500, "Error", null, -1);

        assertTrue(response200.isSuccess());
        assertTrue(response201.isSuccess());
        assertTrue(response299.isSuccess());
        assertFalse(response300.isSuccess());
        assertFalse(response404.isSuccess());
        assertFalse(response500.isSuccess());
    }
}