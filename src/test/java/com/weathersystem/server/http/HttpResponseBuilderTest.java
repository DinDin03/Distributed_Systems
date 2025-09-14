package com.weathersystem.server.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void testSendSuccessResponse() {
        responseBuilder.sendSuccessResponse(printWriter, HttpStatusCodes.OK, 
                                          HttpStatusCodes.OK_TEXT, 5L);

        String response = getResponseContent();
        assertTrue(response.startsWith("HTTP/1.1 200 OK"), "Should start with correct status line");
        assertTrue(response.contains("Lamport-Time: 5"), "Should include Lamport time");
        assertTrue(response.contains("Content-Length: 0"), "Should include content length");
        assertTrue(response.contains("\r\n\r\n"), "Should have proper header/body separator");
    }

    @Test
    void testSendSuccessResponseCreated() {
        responseBuilder.sendSuccessResponse(printWriter, HttpStatusCodes.CREATED, 
                                          HttpStatusCodes.CREATED_TEXT, 10L);

        String response = getResponseContent();
        assertTrue(response.startsWith("HTTP/1.1 201 Created"), "Should start with 201 Created");
        assertTrue(response.contains("Lamport-Time: 10"), "Should include Lamport time");
        assertTrue(response.contains("Content-Length: 0"), "Should include content length");
    }

    @Test
    void testSendSuccessResponseNoContent() {
        responseBuilder.sendSuccessResponse(printWriter, HttpStatusCodes.NO_CONTENT, 
                                          HttpStatusCodes.NO_CONTENT_TEXT, 15L);

        String response = getResponseContent();
        assertTrue(response.startsWith("HTTP/1.1 204 No Content"), "Should start with 204 No Content");
        assertTrue(response.contains("Lamport-Time: 15"), "Should include Lamport time");
        assertTrue(response.contains("Content-Length: 0"), "Should include content length");
    }

    @Test
    void testSendJsonResponse() {
        String jsonData = "{\"id\":\"TEST001\",\"name\":\"Test Station\"}";
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, jsonData, 20L);

        String response = getResponseContent();
        assertTrue(response.startsWith("HTTP/1.1 200 OK"), "Should start with 200 OK");
        assertTrue(response.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response.contains("Lamport-Time: 20"), "Should include Lamport time");
        assertTrue(response.contains("Content-Length: " + jsonData.length()), "Should include correct content length");
        assertTrue(response.contains(jsonData), "Should include JSON data in body");
    }

    @Test
    void testSendJsonResponseWithEmptyData() {
        String jsonData = "[]";
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, jsonData, 25L);

        String response = getResponseContent();
        assertTrue(response.startsWith("HTTP/1.1 200 OK"), "Should start with 200 OK");
        assertTrue(response.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response.contains("Lamport-Time: 25"), "Should include Lamport time");
        assertTrue(response.contains("Content-Length: 2"), "Should include correct content length for empty array");
        assertTrue(response.contains("[]"), "Should include empty JSON array");
    }

    @Test
    void testSendJsonResponseWithLargeData() {
        StringBuilder largeJson = new StringBuilder();
        largeJson.append("[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) largeJson.append(",");
            largeJson.append("{\"id\":\"STATION").append(i).append("\",\"name\":\"Station ").append(i).append("\"}");
        }
        largeJson.append("]");
        
        String jsonData = largeJson.toString();
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, jsonData, 30L);

        String response = getResponseContent();
        assertTrue(response.startsWith("HTTP/1.1 200 OK"), "Should start with 200 OK");
        assertTrue(response.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response.contains("Lamport-Time: 30"), "Should include Lamport time");
        assertTrue(response.contains("Content-Length: " + jsonData.length()), "Should include correct content length");
        assertTrue(response.contains("STATION0"), "Should include first station");
        assertTrue(response.contains("STATION99"), "Should include last station");
    }

    @Test
    void testSendJsonResponseWithSpecialCharacters() {
        String jsonData = "{\"id\":\"SPECIAL001\",\"name\":\"Station with special chars: ñáéíóú & symbols\",\"description\":\"Partly \\\"cloudy\\\" with mixed conditions\"}";
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, jsonData, 35L);

        String response = getResponseContent();
        assertTrue(response.startsWith("HTTP/1.1 200 OK"), "Should start with 200 OK");
        assertTrue(response.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response.contains("Lamport-Time: 35"), "Should include Lamport time");
        assertTrue(response.contains("Content-Length:"), "Should include content length header");
        assertTrue(response.contains("ñáéíóú"), "Should preserve special characters");
        assertTrue(response.contains("cloudy"), "Should preserve cloud description");
    }

    @Test
    void testSendErrorResponse() {
        responseBuilder.sendErrorResponse(printWriter, HttpStatusCodes.BAD_REQUEST, 
                                        HttpStatusCodes.BAD_REQUEST_TEXT, 40L);

        String response = getResponseContent();
        assertTrue(response.startsWith("HTTP/1.1 400 Bad Request"), "Should start with 400 Bad Request");
        assertTrue(response.contains("Lamport-Time: 40"), "Should include Lamport time");
        assertTrue(response.contains("Content-Length: 0"), "Should include content length");
        assertTrue(response.contains("\r\n\r\n"), "Should have proper header/body separator");
    }

    @Test
    void testSendErrorResponseInternalServerError() {
        responseBuilder.sendErrorResponse(printWriter, HttpStatusCodes.INTERNAL_SERVER_ERROR, 
                                        HttpStatusCodes.INTERNAL_SERVER_ERROR_TEXT, 45L);

        String response = getResponseContent();
        assertTrue(response.startsWith("HTTP/1.1 500 Internal Server Error"), "Should start with 500 Internal Server Error");
        assertTrue(response.contains("Lamport-Time: 45"), "Should include Lamport time");
        assertTrue(response.contains("Content-Length: 0"), "Should include content length");
    }

    @Test
    void testSendErrorResponseWithCustomMessage() {
        String customMessage = "Custom Error Message";
        responseBuilder.sendErrorResponse(printWriter, 422, customMessage, 50L);

        String response = getResponseContent();
        assertTrue(response.startsWith("HTTP/1.1 422 " + customMessage), "Should start with custom status line");
        assertTrue(response.contains("Lamport-Time: 50"), "Should include Lamport time");
        assertTrue(response.contains("Content-Length: 0"), "Should include content length");
    }

    @Test
    void testResponseFormatting() {
        responseBuilder.sendSuccessResponse(printWriter, HttpStatusCodes.OK, 
                                          HttpStatusCodes.OK_TEXT, 55L);

        String response = getResponseContent();
        
        // Test that response follows proper HTTP format
        String[] lines = response.split("\r\n");
        assertTrue(lines.length >= 3, "Should have at least 3 lines (status + headers + empty line)");
        
        // First line should be status line
        assertTrue(lines[0].startsWith("HTTP/1.1"), "First line should be status line");
        
        // Should have headers
        boolean hasLamportTime = false;
        boolean hasContentLength = false;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].startsWith("Lamport-Time:")) hasLamportTime = true;
            if (lines[i].startsWith("Content-Length:")) hasContentLength = true;
        }
        assertTrue(hasLamportTime, "Should have Lamport-Time header");
        assertTrue(hasContentLength, "Should have Content-Length header");
    }

    @Test
    void testJsonResponseFormatting() {
        String jsonData = "{\"test\":\"data\"}";
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, jsonData, 60L);

        String response = getResponseContent();
        
        // Test that response follows proper HTTP format
        String[] lines = response.split("\r\n");
        assertTrue(lines.length >= 4, "Should have at least 4 lines (status + headers + empty line + body)");
        
        // First line should be status line
        assertTrue(lines[0].startsWith("HTTP/1.1"), "First line should be status line");
        
        // Should have proper headers
        boolean hasContentType = false;
        boolean hasLamportTime = false;
        boolean hasContentLength = false;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].startsWith("Content-Type:")) hasContentType = true;
            if (lines[i].startsWith("Lamport-Time:")) hasLamportTime = true;
            if (lines[i].startsWith("Content-Length:")) hasContentLength = true;
        }
        assertTrue(hasContentType, "Should have Content-Type header");
        assertTrue(hasLamportTime, "Should have Lamport-Time header");
        assertTrue(hasContentLength, "Should have Content-Length header");
        
        // Should have JSON data in body
        assertTrue(response.contains(jsonData), "Should contain JSON data in body");
    }

    @Test
    void testZeroLamportTime() {
        responseBuilder.sendSuccessResponse(printWriter, HttpStatusCodes.OK, 
                                          HttpStatusCodes.OK_TEXT, 0L);

        String response = getResponseContent();
        assertTrue(response.contains("Lamport-Time: 0"), "Should handle zero Lamport time");
    }

    @Test
    void testNegativeLamportTime() {
        responseBuilder.sendSuccessResponse(printWriter, HttpStatusCodes.OK, 
                                          HttpStatusCodes.OK_TEXT, -1L);

        String response = getResponseContent();
        assertTrue(response.contains("Lamport-Time: -1"), "Should handle negative Lamport time");
    }

    @Test
    void testLargeLamportTime() {
        long largeTime = Long.MAX_VALUE;
        responseBuilder.sendSuccessResponse(printWriter, HttpStatusCodes.OK, 
                                          HttpStatusCodes.OK_TEXT, largeTime);

        String response = getResponseContent();
        assertTrue(response.contains("Lamport-Time: " + largeTime), "Should handle large Lamport time");
    }

    @Test
    void testJsonResponseWithUnicodeCharacters() {
        String jsonData = "{\"id\":\"UNICODE001\",\"name\":\"Station with Unicode: 中文 日本語 한국어\",\"description\":\"Weather station with international characters\"}";
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, jsonData, 65L);

        String response = getResponseContent();
        assertTrue(response.startsWith("HTTP/1.1 200 OK"), "Should start with 200 OK");
        assertTrue(response.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response.contains("Lamport-Time: 65"), "Should include Lamport time");
        assertTrue(response.contains("中文"), "Should preserve Chinese characters");
        assertTrue(response.contains("日本語"), "Should preserve Japanese characters");
        assertTrue(response.contains("한국어"), "Should preserve Korean characters");
    }

    @Test
    void testJsonResponseWithNumericPrecision() {
        String jsonData = "{\"id\":\"PRECISION001\",\"temperature\":13.123456789,\"latitude\":-34.987654321,\"longitude\":138.123456789}";
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, jsonData, 70L);

        String response = getResponseContent();
        assertTrue(response.startsWith("HTTP/1.1 200 OK"), "Should start with 200 OK");
        assertTrue(response.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response.contains("Lamport-Time: 70"), "Should include Lamport time");
        assertTrue(response.contains("13.123456789"), "Should preserve numeric precision");
        assertTrue(response.contains("-34.987654321"), "Should preserve negative numeric precision");
        assertTrue(response.contains("138.123456789"), "Should preserve positive numeric precision");
    }

    @Test
    void testJsonResponseWithEmptyObject() {
        String jsonData = "{}";
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, jsonData, 75L);

        String response = getResponseContent();
        assertTrue(response.startsWith("HTTP/1.1 200 OK"), "Should start with 200 OK");
        assertTrue(response.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response.contains("Lamport-Time: 75"), "Should include Lamport time");
        assertTrue(response.contains("Content-Length: 2"), "Should include correct content length");
        assertTrue(response.contains("{}"), "Should include empty JSON object");
    }

    @Test
    void testJsonResponseWithNestedObjects() {
        String jsonData = "{\"weather\":{\"temperature\":20.5,\"humidity\":60},\"station\":{\"id\":\"NESTED001\",\"name\":\"Nested Station\"}}";
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, jsonData, 80L);

        String response = getResponseContent();
        assertTrue(response.startsWith("HTTP/1.1 200 OK"), "Should start with 200 OK");
        assertTrue(response.contains("Content-Type: application/json"), "Should include JSON content type");
        assertTrue(response.contains("Lamport-Time: 80"), "Should include Lamport time");
        assertTrue(response.contains("NESTED001"), "Should include nested station ID");
        assertTrue(response.contains("Nested Station"), "Should include nested station name");
        assertTrue(response.contains("20.5"), "Should include nested temperature");
        assertTrue(response.contains("60"), "Should include nested humidity");
    }

    @Test
    void testMultipleResponses() {
        // Test multiple responses to same PrintWriter
        responseBuilder.sendSuccessResponse(printWriter, HttpStatusCodes.OK, 
                                          HttpStatusCodes.OK_TEXT, 85L);
        
        String response1 = getResponseContent();
        assertTrue(response1.contains("HTTP/1.1 200 OK"), "First response should be OK");
        assertTrue(response1.contains("Lamport-Time: 85"), "First response should have correct Lamport time");
        
        // Reset for second response
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        
        responseBuilder.sendErrorResponse(printWriter, HttpStatusCodes.BAD_REQUEST, 
                                        HttpStatusCodes.BAD_REQUEST_TEXT, 90L);
        
        String response2 = getResponseContent();
        assertTrue(response2.contains("HTTP/1.1 400 Bad Request"), "Second response should be Bad Request");
        assertTrue(response2.contains("Lamport-Time: 90"), "Second response should have correct Lamport time");
    }

    @Test
    void testResponseHeadersOrder() {
        responseBuilder.sendJsonResponse(printWriter, HttpStatusCodes.OK, "{\"test\":\"data\"}", 95L);

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
    }
}
