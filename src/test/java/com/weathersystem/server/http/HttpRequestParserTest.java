package com.weathersystem.server.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for HttpRequestParser class.
 * Tests HTTP request parsing, header handling, and various edge cases.
 */
class HttpRequestParserTest {

    private HttpRequestParser parser;

    @BeforeEach
    void setUp() {
        parser = new HttpRequestParser();
    }

    // === CORE FUNCTIONALITY TESTS ===

    @Test
    void testParseValidRequests() throws IOException {
        System.out.println("Testing parsing of valid HTTP requests...");
        
        // Test GET request
        String getRequest = "GET /weather.json HTTP/1.1\r\n" +
                "Host: localhost:4567\r\n" +
                "User-Agent: WeatherClient/1.0\r\n" +
                "Lamport-Time: 5\r\n" +
                "\r\n";

        BufferedReader getReader = new BufferedReader(new StringReader(getRequest));
        HttpRequest getHttpRequest = parser.parseRequest(getReader);

        assertEquals("GET", getHttpRequest.getMethod(), "Method should be GET");
        assertEquals("/weather.json", getHttpRequest.getPath(), "Path should be /weather.json");
        assertEquals("HTTP/1.1", getHttpRequest.getHttpVersion(), "HTTP version should be HTTP/1.1");
        assertEquals(5, getHttpRequest.getLamportTime(), "Lamport time should be 5");
        assertEquals(0, getHttpRequest.getContentLength(), "Content length should be 0 for GET");
        assertEquals("localhost:4567", getHttpRequest.getHeaders().get("host"), "Host header should be correct");
        assertEquals("WeatherClient/1.0", getHttpRequest.getHeaders().get("user-agent"), "User-Agent should be correct");

        // Test PUT request
        String putRequest = "PUT /weather.json HTTP/1.1\r\n" +
                "Host: localhost:4567\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: 150\r\n" +
                "Lamport-Time: 10\r\n" +
                "\r\n";

        BufferedReader putReader = new BufferedReader(new StringReader(putRequest));
        HttpRequest putHttpRequest = parser.parseRequest(putReader);

        assertEquals("PUT", putHttpRequest.getMethod(), "Method should be PUT");
        assertEquals("/weather.json", putHttpRequest.getPath(), "Path should be /weather.json");
        assertEquals("HTTP/1.1", putHttpRequest.getHttpVersion(), "HTTP version should be HTTP/1.1");
        assertEquals(10, putHttpRequest.getLamportTime(), "Lamport time should be 10");
        assertEquals(150, putHttpRequest.getContentLength(), "Content length should be 150");
        assertEquals("application/json", putHttpRequest.getHeaders().get("content-type"), "Content-Type should be correct");
        assertEquals("150", putHttpRequest.getHeaders().get("content-length"), "Content-Length should be correct");
        
        System.out.println("✓ Valid requests test passed");
    }

    @Test
    void testParseRequestHeaders() throws IOException {
        System.out.println("Testing request header parsing...");
        
        // Test mixed case headers
        String mixedCaseRequest = "GET /weather.json HTTP/1.1\r\n" +
                "HOST: localhost:4567\r\n" +
                "Content-TYPE: application/json\r\n" +
                "LAMPORT-TIME: 15\r\n" +
                "\r\n";

        BufferedReader reader1 = new BufferedReader(new StringReader(mixedCaseRequest));
        HttpRequest httpRequest1 = parser.parseRequest(reader1);

        assertEquals("localhost:4567", httpRequest1.getHeaders().get("host"), "Host header should be lowercase");
        assertEquals("application/json", httpRequest1.getHeaders().get("content-type"), "Content-Type header should be lowercase");
        assertEquals(15, httpRequest1.getLamportTime(), "Lamport time should be parsed correctly");

        // Test extra whitespace
        String whitespaceRequest = "GET /weather.json HTTP/1.1\r\n" +
                "Host:   localhost:4567   \r\n" +
                "Lamport-Time:   20   \r\n" +
                "\r\n";

        BufferedReader reader2 = new BufferedReader(new StringReader(whitespaceRequest));
        HttpRequest httpRequest2 = parser.parseRequest(reader2);

        assertEquals("localhost:4567", httpRequest2.getHeaders().get("host"), "Host header value should be trimmed");
        assertEquals(20, httpRequest2.getLamportTime(), "Lamport time should be parsed correctly");

        // Test multiple headers
        String multipleHeadersRequest = "PUT /weather.json HTTP/1.1\r\n" +
                "Host: localhost:4567\r\n" +
                "User-Agent: WeatherClient/1.0\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: 200\r\n" +
                "Accept: application/json\r\n" +
                "Connection: keep-alive\r\n" +
                "Lamport-Time: 25\r\n" +
                "Custom-Header: custom-value\r\n" +
                "\r\n";

        BufferedReader reader3 = new BufferedReader(new StringReader(multipleHeadersRequest));
        HttpRequest httpRequest3 = parser.parseRequest(reader3);

        assertEquals(8, httpRequest3.getHeaders().size(), "Should have 8 headers");
        assertEquals("WeatherClient/1.0", httpRequest3.getHeaders().get("user-agent"), "User-Agent should be correct");
        assertEquals("application/json", httpRequest3.getHeaders().get("accept"), "Accept should be correct");
        assertEquals("keep-alive", httpRequest3.getHeaders().get("connection"), "Connection should be correct");
        assertEquals("custom-value", httpRequest3.getHeaders().get("custom-header"), "Custom header should be correct");
        assertEquals(25, httpRequest3.getLamportTime(), "Lamport time should be correct");
        assertEquals(200, httpRequest3.getContentLength(), "Content length should be correct");
        
        System.out.println("✓ Request headers test passed");
    }

    @Test
    void testParseRequestWithoutOptionalHeaders() throws IOException {
        System.out.println("Testing requests without optional headers...");
        
        String request = "GET /weather.json HTTP/1.1\r\n" +
                "\r\n"; // No headers at all

        BufferedReader reader = new BufferedReader(new StringReader(request));
        HttpRequest httpRequest = parser.parseRequest(reader);

        assertEquals(-1, httpRequest.getLamportTime(), "Lamport time should be -1 when not present");
        assertEquals(0, httpRequest.getContentLength(), "Content length should be 0 when not present");
        assertTrue(httpRequest.getHeaders().isEmpty(), "Headers map should be empty when no headers present");
        
        System.out.println("✓ Optional headers test passed");
    }

    // === ERROR HANDLING TESTS ===

    @Test
    void testParseInvalidRequestLines() {
        System.out.println("Testing parsing of invalid request lines...");
        
        // Test empty request line
        String emptyRequest = "\r\n";
        BufferedReader reader1 = new BufferedReader(new StringReader(emptyRequest));
        assertThrows(IllegalArgumentException.class, () -> {
            parser.parseRequest(reader1);
        }, "Empty request line should throw IllegalArgumentException");

        // Test null request line
        String nullRequest = "";
        BufferedReader reader2 = new BufferedReader(new StringReader(nullRequest));
        assertThrows(IllegalArgumentException.class, () -> {
            parser.parseRequest(reader2);
        }, "Null request line should throw IllegalArgumentException");

        // Test invalid request line format
        String invalidFormatRequest = "INVALID_REQUEST_LINE\r\n\r\n";
        BufferedReader reader3 = new BufferedReader(new StringReader(invalidFormatRequest));
        assertThrows(IllegalArgumentException.class, () -> {
            parser.parseRequest(reader3);
        }, "Invalid request line format should throw IllegalArgumentException");

        // Test request line with two parameters
        String twoParamsRequest = "GET /weather.json\r\n\r\n";
        BufferedReader reader4 = new BufferedReader(new StringReader(twoParamsRequest));
        assertThrows(IllegalArgumentException.class, () -> {
            parser.parseRequest(reader4);
        }, "Request line with only two parameters should throw IllegalArgumentException");

        // Test request line with too many parameters
        String tooManyParamsRequest = "GET /weather.json HTTP/1.1 EXTRA\r\n\r\n";
        BufferedReader reader5 = new BufferedReader(new StringReader(tooManyParamsRequest));
        assertThrows(IllegalArgumentException.class, () -> {
            parser.parseRequest(reader5);
        }, "Request line with too many parameters should throw IllegalArgumentException");
        
        System.out.println("✓ Invalid request lines test passed");
    }

    @Test
    void testParseInvalidHeaderValues() throws IOException {
        System.out.println("Testing parsing of invalid header values...");
        
        // Test invalid content length
        String invalidContentLengthRequest = "PUT /weather.json HTTP/1.1\r\n" +
                "Content-Length: not_a_number\r\n" +
                "\r\n";

        BufferedReader reader1 = new BufferedReader(new StringReader(invalidContentLengthRequest));
        HttpRequest httpRequest1 = parser.parseRequest(reader1);
        assertEquals(0, httpRequest1.getContentLength(), "Invalid Content-Length should default to 0");

        // Test invalid lamport time
        String invalidLamportTimeRequest = "GET /weather.json HTTP/1.1\r\n" +
                "Lamport-Time: not_a_number\r\n" +
                "\r\n";

        BufferedReader reader2 = new BufferedReader(new StringReader(invalidLamportTimeRequest));
        HttpRequest httpRequest2 = parser.parseRequest(reader2);
        assertEquals(-1, httpRequest2.getLamportTime(), "Invalid Lamport-Time should default to -1");

        // Test malformed headers
        String malformedHeadersRequest = "GET /weather.json HTTP/1.1\r\n" +
                "Host: localhost:4567\r\n" +
                "MalformedHeaderWithoutColon\r\n" +
                "Lamport-Time: 30\r\n" +
                "\r\n";

        BufferedReader reader3 = new BufferedReader(new StringReader(malformedHeadersRequest));
        HttpRequest httpRequest3 = parser.parseRequest(reader3);
        assertEquals("GET", httpRequest3.getMethod(), "Method should be correct");
        assertEquals(30, httpRequest3.getLamportTime(), "Lamport time should be correct");
        assertEquals("localhost:4567", httpRequest3.getHeaders().get("host"), "Valid header should be parsed");
        assertFalse(httpRequest3.getHeaders().containsKey("malformedheaderwithoutcolon"),
                "Malformed header should be skipped");
        
        System.out.println("✓ Invalid header values test passed");
    }

    @Test
    void testParseIOException() {
        System.out.println("Testing IO exception handling...");
        
        BufferedReader faultyReader = new BufferedReader(new StringReader("")) {
            @Override
            public String readLine() throws IOException {
                throw new IOException("Simulated IO error");
            }
        };

        assertThrows(IOException.class, () -> {
            parser.parseRequest(faultyReader);
        }, "IOException should be propagated");
        
        System.out.println("✓ IO exception test passed");
    }

    // === EDGE CASES TESTS ===

    @Test
    void testParseSpecialValues() throws IOException {
        System.out.println("Testing parsing of special values...");
        
        // Test negative lamport time
        String negativeLamportRequest = "GET /weather.json HTTP/1.1\r\n" +
                "Lamport-Time: -5\r\n" +
                "\r\n";

        BufferedReader reader1 = new BufferedReader(new StringReader(negativeLamportRequest));
        HttpRequest httpRequest1 = parser.parseRequest(reader1);
        assertEquals(-5, httpRequest1.getLamportTime(), "Negative Lamport-Time should be preserved");

        // Test zero values
        String zeroValuesRequest = "PUT /weather.json HTTP/1.1\r\n" +
                "Content-Length: 0\r\n" +
                "Lamport-Time: 0\r\n" +
                "\r\n";

        BufferedReader reader2 = new BufferedReader(new StringReader(zeroValuesRequest));
        HttpRequest httpRequest2 = parser.parseRequest(reader2);
        assertEquals(0, httpRequest2.getContentLength(), "Zero Content-Length should be valid");
        assertEquals(0, httpRequest2.getLamportTime(), "Zero Lamport-Time should be valid");

        // Test large values
        String largeValuesRequest = "PUT /weather.json HTTP/1.1\r\n" +
                "Content-Length: 2147483647\r\n" + // Max int value
                "Lamport-Time: 9223372036854775807\r\n" + // Max long value
                "\r\n";

        BufferedReader reader3 = new BufferedReader(new StringReader(largeValuesRequest));
        HttpRequest httpRequest3 = parser.parseRequest(reader3);
        assertEquals(Integer.MAX_VALUE, httpRequest3.getContentLength(), "Large Content-Length should be handled");
        assertEquals(Long.MAX_VALUE, httpRequest3.getLamportTime(), "Large Lamport-Time should be handled");
        
        System.out.println("✓ Special values test passed");
    }

    @Test
    void testParseSpecialHeaderScenarios() throws IOException {
        System.out.println("Testing special header scenarios...");
        
        // Test empty header value
        String emptyHeaderValueRequest = "GET /weather.json HTTP/1.1\r\n" +
                "Host: \r\n" +
                "Lamport-Time: 35\r\n" +
                "\r\n";

        BufferedReader reader1 = new BufferedReader(new StringReader(emptyHeaderValueRequest));
        HttpRequest httpRequest1 = parser.parseRequest(reader1);
        assertEquals("", httpRequest1.getHeaders().get("host"), "Empty header value should be preserved");
        assertEquals(35, httpRequest1.getLamportTime(), "Lamport time should be correct");

        // Test header value with colons
        String colonHeaderValueRequest = "GET /weather.json HTTP/1.1\r\n" +
                "User-Agent: Client:1.0:Special\r\n" +
                "\r\n";

        BufferedReader reader2 = new BufferedReader(new StringReader(colonHeaderValueRequest));
        HttpRequest httpRequest2 = parser.parseRequest(reader2);
        assertEquals("Client:1.0:Special", httpRequest2.getHeaders().get("user-agent"),
                "Header value with colons should be preserved");
        
        System.out.println("✓ Special header scenarios test passed");
    }

    @Test
    void testParseDifferentMethodsAndVersions() throws IOException {
        System.out.println("Testing different HTTP methods and versions...");
        
        // Test different HTTP methods
        String[] methods = {"GET", "PUT", "POST", "DELETE", "HEAD", "OPTIONS"};
        for (String method : methods) {
            String request = method + " /weather.json HTTP/1.1\r\n" +
                    "Host: localhost:4567\r\n" +
                    "\r\n";

            BufferedReader reader = new BufferedReader(new StringReader(request));
            HttpRequest httpRequest = parser.parseRequest(reader);
            assertEquals(method, httpRequest.getMethod(), "Method " + method + " should be supported");
        }

        // Test HTTP/1.0
        String http10Request = "GET /weather.json HTTP/1.0\r\n" +
                "Host: localhost:4567\r\n" +
                "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(http10Request));
        HttpRequest httpRequest = parser.parseRequest(reader);
        assertEquals("HTTP/1.0", httpRequest.getHttpVersion(), "HTTP/1.0 should be supported");

        // Test complex path
        String complexPathRequest = "GET /api/v1/weather.json?station=123&format=json HTTP/1.1\r\n" +
                "Host: localhost:4567\r\n" +
                "\r\n";

        BufferedReader complexReader = new BufferedReader(new StringReader(complexPathRequest));
        HttpRequest complexHttpRequest = parser.parseRequest(complexReader);
        assertEquals("/api/v1/weather.json?station=123&format=json", complexHttpRequest.getPath(),
                "Complex path with query parameters should be preserved");
        
        System.out.println("✓ Different methods and versions test passed");
    }
}