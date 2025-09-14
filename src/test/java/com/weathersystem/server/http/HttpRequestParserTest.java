package com.weathersystem.server.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class HttpRequestParserTest {

    private HttpRequestParser parser;

    @BeforeEach
    void setUp() {
        parser = new HttpRequestParser();
    }

    @Test
    void testParseValidGETRequest() throws IOException {
        String request = "GET /weather.json HTTP/1.1\r\n" +
                "Host: localhost:4567\r\n" +
                "User-Agent: WeatherClient/1.0\r\n" +
                "Lamport-Time: 5\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        assertEquals("GET", httpRequest.getMethod(), "Method should be GET");
        assertEquals("/weather.json", httpRequest.getPath(), "Path should be /weather.json");
        assertEquals("HTTP/1.1", httpRequest.getHttpVersion(), "HTTP version should be HTTP/1.1");
        assertEquals(5, httpRequest.getLamportTime(), "Lamport time should be 5");
        assertEquals(0, httpRequest.getContentLength(), "Content length should be 0 for GET");

        assertEquals("localhost:4567", httpRequest.getHeaders().get("host"), "Host header should be correct");
        assertEquals("WeatherClient/1.0", httpRequest.getHeaders().get("user-agent"), "User-Agent should be correct");
        assertEquals("5", httpRequest.getHeaders().get("lamport-time"), "Lamport-Time header should be correct");
    }

    @Test
    void testParseValidPUTRequest() throws IOException {
        String request = "PUT /weather.json HTTP/1.1\r\n" +
                "Host: localhost:4567\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: 150\r\n" +
                "Lamport-Time: 10\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        assertEquals("PUT", httpRequest.getMethod(), "Method should be PUT");
        assertEquals("/weather.json", httpRequest.getPath(), "Path should be /weather.json");
        assertEquals("HTTP/1.1", httpRequest.getHttpVersion(), "HTTP version should be HTTP/1.1");
        assertEquals(10, httpRequest.getLamportTime(), "Lamport time should be 10");
        assertEquals(150, httpRequest.getContentLength(), "Content length should be 150");

        assertEquals("application/json", httpRequest.getHeaders().get("content-type"), "Content-Type should be correct");
        assertEquals("150", httpRequest.getHeaders().get("content-length"), "Content-Length should be correct");
    }

    @Test
    void testParseRequestWithoutLamportTime() throws IOException {
        String request = "GET /weather.json HTTP/1.1\r\n" +
                "Host: localhost:4567\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        assertEquals(-1, httpRequest.getLamportTime(), "Lamport time should be -1 when not present");
    }

    @Test
    void testParseRequestWithoutContentLength() throws IOException {
        String request = "GET /weather.json HTTP/1.1\r\n" +
                "Host: localhost:4567\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        assertEquals(0, httpRequest.getContentLength(), "Content length should be 0 when not present");
    }

    @Test
    void testParseRequestWithMixedCaseHeaders() throws IOException {
        String request = "GET /weather.json HTTP/1.1\r\n" +
                "HOST: localhost:4567\r\n" +
                "Content-TYPE: application/json\r\n" +
                "LAMPORT-TIME: 15\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        // Headers should be normalized to lowercase
        assertEquals("localhost:4567", httpRequest.getHeaders().get("host"), "Host header should be lowercase");
        assertEquals("application/json", httpRequest.getHeaders().get("content-type"), "Content-Type header should be lowercase");
        assertEquals(15, httpRequest.getLamportTime(), "Lamport time should be parsed correctly");
    }

    @Test
    void testParseRequestWithExtraWhitespace() throws IOException {
        String request = "GET /weather.json HTTP/1.1\r\n" +
                "Host:   localhost:4567   \r\n" +
                "Lamport-Time:   20   \r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        assertEquals("GET", httpRequest.getMethod(), "Method should be correct");
        assertEquals("/weather.json", httpRequest.getPath(), "Path should be correct");
        assertEquals("HTTP/1.1", httpRequest.getHttpVersion(), "HTTP version should be correct");
        assertEquals("localhost:4567", httpRequest.getHeaders().get("host"), "Host header value should be trimmed");
        assertEquals(20, httpRequest.getLamportTime(), "Lamport time should be parsed correctly");
    }

    @Test
    void testParseRequestWithMultipleHeaders() throws IOException {
        String request = "PUT /weather.json HTTP/1.1\r\n" +
                "Host: localhost:4567\r\n" +
                "User-Agent: WeatherClient/1.0\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: 200\r\n" +
                "Accept: application/json\r\n" +
                "Connection: keep-alive\r\n" +
                "Lamport-Time: 25\r\n" +
                "Custom-Header: custom-value\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        assertEquals(8, httpRequest.getHeaders().size(), "Should have 8 headers");
        assertEquals("WeatherClient/1.0", httpRequest.getHeaders().get("user-agent"));
        assertEquals("application/json", httpRequest.getHeaders().get("accept"));
        assertEquals("keep-alive", httpRequest.getHeaders().get("connection"));
        assertEquals("custom-value", httpRequest.getHeaders().get("custom-header"));
        assertEquals(25, httpRequest.getLamportTime());
        assertEquals(200, httpRequest.getContentLength());
    }

    @Test
    void testParseRequestWithMalformedHeaders() throws IOException {
        String request = "GET /weather.json HTTP/1.1\r\n" +
                "Host: localhost:4567\r\n" +
                "MalformedHeaderWithoutColon\r\n" +
                "Lamport-Time: 30\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        // Should parse successfully, ignoring malformed header
        assertEquals("GET", httpRequest.getMethod());
        assertEquals(30, httpRequest.getLamportTime());
        assertEquals("localhost:4567", httpRequest.getHeaders().get("host"));
        assertFalse(httpRequest.getHeaders().containsKey("malformedheaderwithoutcolon"),
                "Malformed header should be skipped");
    }

    @Test
    void testParseEmptyRequestLine() {
        String request = "\r\n";
        BufferedReader reader = new BufferedReader(new StringReader(request));

        assertThrows(IllegalArgumentException.class, () -> {
            parser.parseRequest(reader);
        }, "Empty request line should throw IllegalArgumentException");
    }

    @Test
    void testParseNullRequestLine() {
        String request = "";
        BufferedReader reader = new BufferedReader(new StringReader(request));

        assertThrows(IllegalArgumentException.class, () -> {
            parser.parseRequest(reader);
        }, "Null request line should throw IllegalArgumentException");
    }

    @Test
    void testParseInvalidRequestLineFormat() {
        String request = "INVALID_REQUEST_LINE\r\n\r\n";
        BufferedReader reader = new BufferedReader(new StringReader(request));

        assertThrows(IllegalArgumentException.class, () -> {
            parser.parseRequest(reader);
        }, "Invalid request line format should throw IllegalArgumentException");
    }

    @Test
    void testParseRequestLineWithTwoParams() {
        String request = "GET /weather.json\r\n\r\n";
        BufferedReader reader = new BufferedReader(new StringReader(request));

        assertThrows(IllegalArgumentException.class, () -> {
            parser.parseRequest(reader);
        }, "Request line with only two parameters should throw IllegalArgumentException");
    }

    @Test
    void testParseRequestLineWithTooManyParams() {
        String request = "GET /weather.json HTTP/1.1 EXTRA\r\n\r\n";
        BufferedReader reader = new BufferedReader(new StringReader(request));

        assertThrows(IllegalArgumentException.class, () -> {
            parser.parseRequest(reader);
        }, "Request line with too many parameters should throw IllegalArgumentException");
    }

    @Test
    void testParseInvalidContentLength() throws IOException {
        String request = "PUT /weather.json HTTP/1.1\r\n" +
                "Content-Length: not_a_number\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        // Should handle invalid content length gracefully
        assertEquals(0, httpRequest.getContentLength(), "Invalid Content-Length should default to 0");
    }

    @Test
    void testParseInvalidLamportTime() throws IOException {
        String request = "GET /weather.json HTTP/1.1\r\n" +
                "Lamport-Time: not_a_number\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        // Should handle invalid lamport time gracefully
        assertEquals(-1, httpRequest.getLamportTime(), "Invalid Lamport-Time should default to -1");
    }

    @Test
    void testParseNegativeLamportTime() throws IOException {
        String request = "GET /weather.json HTTP/1.1\r\n" +
                "Lamport-Time: -5\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        assertEquals(-5, httpRequest.getLamportTime(), "Negative Lamport-Time should be preserved");
    }

    @Test
    void testParseZeroContentLength() throws IOException {
        String request = "PUT /weather.json HTTP/1.1\r\n" +
                "Content-Length: 0\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        assertEquals(0, httpRequest.getContentLength(), "Zero Content-Length should be valid");
    }

    @Test
    void testParseZeroLamportTime() throws IOException {
        String request = "GET /weather.json HTTP/1.1\r\n" +
                "Lamport-Time: 0\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        assertEquals(0, httpRequest.getLamportTime(), "Zero Lamport-Time should be valid");
    }

    @Test
    void testParseRequestWithLargeContentLength() throws IOException {
        String request = "PUT /weather.json HTTP/1.1\r\n" +
                "Content-Length: 2147483647\r\n" + // Max int value
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        assertEquals(Integer.MAX_VALUE, httpRequest.getContentLength(),
                "Large Content-Length should be handled");
    }

    @Test
    void testParseRequestWithLargeLamportTime() throws IOException {
        String request = "GET /weather.json HTTP/1.1\r\n" +
                "Lamport-Time: 9223372036854775807\r\n" + // Max long value
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        assertEquals(Long.MAX_VALUE, httpRequest.getLamportTime(),
                "Large Lamport-Time should be handled");
    }

    @Test
    void testParseRequestWithEmptyHeaderValue() throws IOException {
        String request = "GET /weather.json HTTP/1.1\r\n" +
                "Host: \r\n" +
                "Lamport-Time: 35\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        assertEquals("", httpRequest.getHeaders().get("host"), "Empty header value should be preserved");
        assertEquals(35, httpRequest.getLamportTime());
    }

    @Test
    void testParseRequestWithColonInHeaderValue() throws IOException {
        String request = "GET /weather.json HTTP/1.1\r\n" +
                "User-Agent: Client:1.0:Special\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        assertEquals("Client:1.0:Special", httpRequest.getHeaders().get("user-agent"),
                "Header value with colons should be preserved");
    }

    @Test
    void testParseRequestWithOnlyHeaders() throws IOException {
        String request = "GET /weather.json HTTP/1.1\r\n" +
                "\r\n"; // No additional headers

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        assertTrue(httpRequest.getHeaders().isEmpty(), "Headers map should be empty");
        assertEquals(-1, httpRequest.getLamportTime(), "Lamport time should be -1");
        assertEquals(0, httpRequest.getContentLength(), "Content length should be 0");
    }

    @Test
    void testParseIOException() {
        BufferedReader faultyReader = new BufferedReader(new StringReader("")) {
            @Override
            public String readLine() throws IOException {
                throw new IOException("Simulated IO error");
            }
        };

        assertThrows(IOException.class, () -> {
            parser.parseRequest(faultyReader);
        }, "IOException should be propagated");
    }

    @Test
    void testParseComplexPath() throws IOException {
        String request = "GET /api/v1/weather.json?station=123&format=json HTTP/1.1\r\n" +
                "Host: localhost:4567\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        assertEquals("/api/v1/weather.json?station=123&format=json", httpRequest.getPath(),
                "Complex path with query parameters should be preserved");
    }

    @Test
    void testParseHttpVersion10() throws IOException {
        String request = "GET /weather.json HTTP/1.0\r\n" +
                "Host: localhost:4567\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        assertEquals("HTTP/1.0", httpRequest.getHttpVersion(), "HTTP/1.0 should be supported");
    }

    @Test
    void testParseDifferentMethods() throws IOException {
        String[] methods = {"GET", "PUT", "POST", "DELETE", "HEAD", "OPTIONS"};

        for (String method : methods) {
            String request = method + " /weather.json HTTP/1.1\r\n" +
                    "Host: localhost:4567\r\n" +
                    "\r\n";

            BufferedReader reader = new BufferedReader(new StringReader(request));
            HttpRequest httpRequest = parser.parseRequest(reader);

            assertEquals(method, httpRequest.getMethod(), "Method " + method + " should be supported");
        }
    }
}