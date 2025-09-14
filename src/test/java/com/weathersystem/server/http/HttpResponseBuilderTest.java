package com.weathersystem.server.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for HttpResponseBuilder class.
 * Tests HTTP response building, JSON responses, error responses, and various edge cases.
 */
class HttpResponseBuilderTest {

    private HttpResponseBuilder responseBuilder;
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() {
        responseBuilder = new HttpResponseBuilder();
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
    }

    private String getResponseContent() {
        printWriter.flush();
        return stringWriter.toString();
    }

    // === CORE FUNCTIONALITY TESTS ===

    @Test
    void testSendSuccessResponses() {
        System.out.println("Testing success response building...");
        
        // Test 200 OK
        responseBuilder.sendSuccessResponse(printWriter, HttpStatusCodes.OK, 
                                          HttpStatusCodes.OK_TEXT, 5L);
        String response1 = getResponseContent();
        assertTrue(response1.startsWith("HTTP/1.1 200 OK"), "Should start with correct status line");
        assertTrue(response1.contains("Lamport-Time: 5"), "Should include Lamport time");
        assertTrue(response1.contains("Content-Length: 0"), "Should include content length");
        assertTrue(response1.contains("\r\n\r\n"), "Should have proper header/body separator");

        // Test 201 Created
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        responseBuilder.sendSuccessResponse(printWriter, HttpStatusCodes.CREATED, 
                                          HttpStatusCodes.CREATED_TEXT, 10L);
        String response2 = getResponseContent();
        assertTrue(response2.startsWith("HTTP/1.1 201 Created"), "Should start with 201 Created");
        assertTrue(response2.contains("Lamport-Time: 10"), "Should include Lamport time");
        assertTrue(response2.contains("Content-Length: 0"), "Should include content length");

        // Test 204 No Content
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        responseBuilder.sendSuccessResponse(printWriter, HttpStatusCodes.NO_CONTENT, 
                                          HttpStatusCodes.NO_CONTENT_TEXT, 15L);
        String response3 = getResponseContent();
        assertTrue(response3.startsWith("HTTP/1.1 204 No Content"), "Should start with 204 No Content");
        assertTrue(response3.contains("Lamport-Time: 15"), "Should include Lamport time");
        assertTrue(response3.contains("Content-Length: 0"), "Should include content length");
        
        System.out.println("✓ Success responses test passed");
    }

    @Test
    void testSendJsonResponses() {
        System.out.println("Testing JSON response building...");
        
        // Test basic JSON response
        String jsonData = "{\"id\":\"TEST001\",\"name\":\"Test Station\"}";
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, jsonData, 20L);
        String response1 = getResponseContent();
        assertTrue(response1.startsWith("HTTP/1.1 200 OK"), "Should start with 200 OK");
        assertTrue(response1.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response1.contains("Lamport-Time: 20"), "Should include Lamport time");
        assertTrue(response1.contains("Content-Length: " + jsonData.length()), "Should include correct content length");
        assertTrue(response1.contains(jsonData), "Should include JSON data in body");

        // Test empty JSON array
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        String emptyJsonData = "[]";
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, emptyJsonData, 25L);
        String response2 = getResponseContent();
        assertTrue(response2.startsWith("HTTP/1.1 200 OK"), "Should start with 200 OK");
        assertTrue(response2.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response2.contains("Lamport-Time: 25"), "Should include Lamport time");
        assertTrue(response2.contains("Content-Length: 2"), "Should include correct content length for empty array");
        assertTrue(response2.contains("[]"), "Should include empty JSON array");

        // Test empty JSON object
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        String emptyObjectJson = "{}";
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, emptyObjectJson, 30L);
        String response3 = getResponseContent();
        assertTrue(response3.startsWith("HTTP/1.1 200 OK"), "Should start with 200 OK");
        assertTrue(response3.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response3.contains("Lamport-Time: 30"), "Should include Lamport time");
        assertTrue(response3.contains("Content-Length: 2"), "Should include correct content length");
        assertTrue(response3.contains("{}"), "Should include empty JSON object");
        
        System.out.println("✓ JSON responses test passed");
    }

    @Test
    void testSendErrorResponses() {
        System.out.println("Testing error response building...");
        
        // Test 400 Bad Request
        responseBuilder.sendErrorResponse(printWriter, HttpStatusCodes.BAD_REQUEST, 
                                        HttpStatusCodes.BAD_REQUEST_TEXT, 40L);
        String response1 = getResponseContent();
        assertTrue(response1.startsWith("HTTP/1.1 400 Bad Request"), "Should start with 400 Bad Request");
        assertTrue(response1.contains("Lamport-Time: 40"), "Should include Lamport time");
        assertTrue(response1.contains("Content-Length: 0"), "Should include content length");
        assertTrue(response1.contains("\r\n\r\n"), "Should have proper header/body separator");

        // Test 500 Internal Server Error
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        responseBuilder.sendErrorResponse(printWriter, HttpStatusCodes.INTERNAL_SERVER_ERROR, 
                                        HttpStatusCodes.INTERNAL_SERVER_ERROR_TEXT, 45L);
        String response2 = getResponseContent();
        assertTrue(response2.startsWith("HTTP/1.1 500 Internal Server Error"), "Should start with 500 Internal Server Error");
        assertTrue(response2.contains("Lamport-Time: 45"), "Should include Lamport time");
        assertTrue(response2.contains("Content-Length: 0"), "Should include content length");

        // Test custom error message
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        String customMessage = "Custom Error Message";
        responseBuilder.sendErrorResponse(printWriter, 422, customMessage, 50L);
        String response3 = getResponseContent();
        assertTrue(response3.startsWith("HTTP/1.1 422 " + customMessage), "Should start with custom status line");
        assertTrue(response3.contains("Lamport-Time: 50"), "Should include Lamport time");
        assertTrue(response3.contains("Content-Length: 0"), "Should include content length");
        
        System.out.println("✓ Error responses test passed");
    }

    // === EDGE CASES TESTS ===

    @Test
    void testSpecialLamportTimes() {
        System.out.println("Testing special Lamport time values...");
        
        // Test zero Lamport time
        responseBuilder.sendSuccessResponse(printWriter, HttpStatusCodes.OK, 
                                          HttpStatusCodes.OK_TEXT, 0L);
        String response1 = getResponseContent();
        assertTrue(response1.contains("Lamport-Time: 0"), "Should handle zero Lamport time");

        // Test negative Lamport time
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        responseBuilder.sendSuccessResponse(printWriter, HttpStatusCodes.OK, 
                                          HttpStatusCodes.OK_TEXT, -1L);
        String response2 = getResponseContent();
        assertTrue(response2.contains("Lamport-Time: -1"), "Should handle negative Lamport time");

        // Test large Lamport time
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        long largeTime = Long.MAX_VALUE;
        responseBuilder.sendSuccessResponse(printWriter, HttpStatusCodes.OK, 
                                          HttpStatusCodes.OK_TEXT, largeTime);
        String response3 = getResponseContent();
        assertTrue(response3.contains("Lamport-Time: " + largeTime), "Should handle large Lamport time");
        
        System.out.println("✓ Special Lamport times test passed");
    }

    @Test
    void testJsonResponseWithSpecialData() {
        System.out.println("Testing JSON responses with special data...");
        
        // Test special characters
        String specialJsonData = "{\"id\":\"SPECIAL001\",\"name\":\"Station with special chars: ñáéíóú & symbols\",\"description\":\"Partly \\\"cloudy\\\" with mixed conditions\"}";
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, specialJsonData, 35L);
        String response1 = getResponseContent();
        assertTrue(response1.startsWith("HTTP/1.1 200 OK"), "Should start with 200 OK");
        assertTrue(response1.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response1.contains("Lamport-Time: 35"), "Should include Lamport time");
        assertTrue(response1.contains("Content-Length:"), "Should include content length header");
        assertTrue(response1.contains("ñáéíóú"), "Should preserve special characters");
        assertTrue(response1.contains("cloudy"), "Should preserve cloud description");

        // Test Unicode characters
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        String unicodeJsonData = "{\"id\":\"UNICODE001\",\"name\":\"Station with Unicode: 中文 日本語 한국어\",\"description\":\"Weather station with international characters\"}";
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, unicodeJsonData, 40L);
        String response2 = getResponseContent();
        assertTrue(response2.startsWith("HTTP/1.1 200 OK"), "Should start with 200 OK");
        assertTrue(response2.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response2.contains("Lamport-Time: 40"), "Should include Lamport time");
        assertTrue(response2.contains("中文"), "Should preserve Chinese characters");
        assertTrue(response2.contains("日本語"), "Should preserve Japanese characters");
        assertTrue(response2.contains("한국어"), "Should preserve Korean characters");

        // Test numeric precision
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        String precisionJsonData = "{\"id\":\"PRECISION001\",\"temperature\":13.123456789,\"latitude\":-34.987654321,\"longitude\":138.123456789}";
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, precisionJsonData, 45L);
        String response3 = getResponseContent();
        assertTrue(response3.startsWith("HTTP/1.1 200 OK"), "Should start with 200 OK");
        assertTrue(response3.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response3.contains("Lamport-Time: 45"), "Should include Lamport time");
        assertTrue(response3.contains("13.123456789"), "Should preserve numeric precision");
        assertTrue(response3.contains("-34.987654321"), "Should preserve negative numeric precision");
        assertTrue(response3.contains("138.123456789"), "Should preserve positive numeric precision");
        
        System.out.println("✓ Special data JSON responses test passed");
    }

    @Test
    void testJsonResponseWithComplexData() {
        System.out.println("Testing JSON responses with complex data structures...");
        
        // Test large JSON data
        StringBuilder largeJson = new StringBuilder();
        largeJson.append("[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) largeJson.append(",");
            largeJson.append("{\"id\":\"STATION").append(i).append("\",\"name\":\"Station ").append(i).append("\"}");
        }
        largeJson.append("]");
        
        String largeJsonData = largeJson.toString();
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, largeJsonData, 50L);
        String response1 = getResponseContent();
        assertTrue(response1.startsWith("HTTP/1.1 200 OK"), "Should start with 200 OK");
        assertTrue(response1.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response1.contains("Lamport-Time: 50"), "Should include Lamport time");
        assertTrue(response1.contains("Content-Length: " + largeJsonData.length()), "Should include correct content length");
        assertTrue(response1.contains("STATION0"), "Should include first station");
        assertTrue(response1.contains("STATION99"), "Should include last station");

        // Test nested objects
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        String nestedJsonData = "{\"weather\":{\"temperature\":20.5,\"humidity\":60},\"station\":{\"id\":\"NESTED001\",\"name\":\"Nested Station\"}}";
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, nestedJsonData, 55L);
        String response2 = getResponseContent();
        assertTrue(response2.startsWith("HTTP/1.1 200 OK"), "Should start with 200 OK");
        assertTrue(response2.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response2.contains("Lamport-Time: 55"), "Should include Lamport time");
        assertTrue(response2.contains("NESTED001"), "Should include nested station ID");
        assertTrue(response2.contains("Nested Station"), "Should include nested station name");
        assertTrue(response2.contains("20.5"), "Should include nested temperature");
        assertTrue(response2.contains("60"), "Should include nested humidity");
        
        System.out.println("✓ Complex data JSON responses test passed");
    }

    // === INTEGRATION TESTS ===

    @Test
    void testResponseFormatting() {
        System.out.println("Testing HTTP response formatting...");
        
        // Test success response formatting
        responseBuilder.sendSuccessResponse(printWriter, HttpStatusCodes.OK, 
                                          HttpStatusCodes.OK_TEXT, 60L);
        String response1 = getResponseContent();
        
        String[] lines1 = response1.split("\r\n");
        assertTrue(lines1.length >= 3, "Should have at least 3 lines (status + headers + empty line)");
        assertTrue(lines1[0].startsWith("HTTP/1.1"), "First line should be status line");
        
        boolean hasLamportTime1 = false;
        boolean hasContentLength1 = false;
        for (int i = 1; i < lines1.length; i++) {
            if (lines1[i].startsWith("Lamport-Time:")) hasLamportTime1 = true;
            if (lines1[i].startsWith("Content-Length:")) hasContentLength1 = true;
        }
        assertTrue(hasLamportTime1, "Should have Lamport-Time header");
        assertTrue(hasContentLength1, "Should have Content-Length header");

        // Test JSON response formatting
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        String jsonData = "{\"test\":\"data\"}";
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, jsonData, 65L);
        String response2 = getResponseContent();
        
        String[] lines2 = response2.split("\r\n");
        assertTrue(lines2.length >= 4, "Should have at least 4 lines (status + headers + empty line + body)");
        assertTrue(lines2[0].startsWith("HTTP/1.1"), "First line should be status line");
        
        boolean hasContentType = false;
        boolean hasLamportTime2 = false;
        boolean hasContentLength2 = false;
        for (int i = 1; i < lines2.length; i++) {
            if (lines2[i].startsWith("Content-Type:")) hasContentType = true;
            if (lines2[i].startsWith("Lamport-Time:")) hasLamportTime2 = true;
            if (lines2[i].startsWith("Content-Length:")) hasContentLength2 = true;
        }
        assertTrue(hasContentType, "Should have Content-Type header");
        assertTrue(hasLamportTime2, "Should have Lamport-Time header");
        assertTrue(hasContentLength2, "Should have Content-Length header");
        assertTrue(response2.contains(jsonData), "Should contain JSON data in body");
        
        System.out.println("✓ Response formatting test passed");
    }

    @Test
    void testResponseHeadersOrder() {
        System.out.println("Testing response headers order...");
        
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, "{\"test\":\"data\"}", 70L);
        String response = getResponseContent();
        String[] lines = response.split("\r\n");
        
        // Find the order of headers
        int statusLineIndex = 0;
        int contentTypeIndex = -1;
        int lamportTimeIndex = -1;
        int contentLengthIndex = -1;
        int emptyLineIndex = -1;
        
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("HTTP/1.1")) statusLineIndex = i;
            else if (lines[i].startsWith("Content-Type:")) contentTypeIndex = i;
            else if (lines[i].startsWith("Lamport-Time:")) lamportTimeIndex = i;
            else if (lines[i].startsWith("Content-Length:")) contentLengthIndex = i;
            else if (lines[i].isEmpty()) emptyLineIndex = i;
        }
        
        // Verify order
        assertTrue(statusLineIndex == 0, "Status line should be first");
        assertTrue(contentTypeIndex > statusLineIndex, "Content-Type should come after status line");
        assertTrue(lamportTimeIndex > statusLineIndex, "Lamport-Time should come after status line");
        assertTrue(contentLengthIndex > statusLineIndex, "Content-Length should come after status line");
        assertTrue(emptyLineIndex > statusLineIndex, "Empty line should come after headers");
        
        System.out.println("✓ Response headers order test passed");
    }

    @Test
    void testMultipleResponses() {
        System.out.println("Testing multiple responses...");
        
        // Test multiple responses to same PrintWriter
        responseBuilder.sendSuccessResponse(printWriter, HttpStatusCodes.OK, 
                                          HttpStatusCodes.OK_TEXT, 75L);
        
        String response1 = getResponseContent();
        assertTrue(response1.contains("HTTP/1.1 200 OK"), "First response should be OK");
        assertTrue(response1.contains("Lamport-Time: 75"), "First response should have correct Lamport time");
        
        // Reset for second response
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        
        responseBuilder.sendErrorResponse(printWriter, HttpStatusCodes.BAD_REQUEST, 
                                        HttpStatusCodes.BAD_REQUEST_TEXT, 80L);
        
        String response2 = getResponseContent();
        assertTrue(response2.contains("HTTP/1.1 400 Bad Request"), "Second response should be Bad Request");
        assertTrue(response2.contains("Lamport-Time: 80"), "Second response should have correct Lamport time");
        
        System.out.println("✓ Multiple responses test passed");
    }
}
