package com.weathersystem.server.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Parser for HTTP requests with support for Lamport timestamps in distributed systems
public class HttpRequestParser {

    // Parses HTTP request from input stream and extracts method, path, headers, and Lamport time
    public HttpRequest parseRequest(BufferedReader input) throws IOException {
        // Read and parse the request line
        String requestLine = input.readLine();
        if (requestLine == null || requestLine.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty request line");
        }

        // Split request line into method, path, and HTTP version components
        String[] requestParts = parseRequestLine(requestLine);
        String method = requestParts[0];
        String path = requestParts[1];
        String httpVersion = requestParts[2];

        // Parse headers
        Map<String, String> headers = parseHeaders(input);

        // Extract important header values for request processing
        int contentLength = extractContentLength(headers);
        long lamportTime = extractLamportTime(headers);

        return new HttpRequest(method, path, httpVersion, headers, contentLength, lamportTime);
    }

    // Parses HTTP request line into method, path, and version components
    private String[] parseRequestLine(String requestLine) {
        String[] parts = requestLine.split(" ");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid request line format: " + requestLine);
        }
        return parts;
    }

    // Parses HTTP headers from input stream into key value map
    private Map<String, String> parseHeaders(BufferedReader input) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String headerLine;

        // Read headers until empty line indicating end of headers
        while ((headerLine = input.readLine()) != null && !headerLine.isEmpty()) {
            int colonIndex = headerLine.indexOf(':');
            if (colonIndex == -1) {
                // Skip malformed headers
                continue;
            }

            // Extract header name and value, normalizing name to lowercase
            String headerName = headerLine.substring(0, colonIndex).trim().toLowerCase();
            String headerValue = headerLine.substring(colonIndex + 1).trim();

            headers.put(headerName, headerValue);
        }

        return headers;
    }

    // Extracts Content-Length header value for request body size validation
    private int extractContentLength(Map<String, String> headers) {
        String contentLengthStr = headers.get("content-length");
        if (contentLengthStr == null) {
            return 0;
        }

        try {
            return Integer.parseInt(contentLengthStr);
        } catch (NumberFormatException e) {
            System.out.println("Invalid Content-Length header: " + contentLengthStr);
            return 0;
        }
    }

    // Extracts Lamport timestamp from headers for distributed system ordering
    private long extractLamportTime(Map<String, String> headers) {
        String lamportTimeStr = headers.get("lamport-time");
        if (lamportTimeStr == null) {
            return -1; // Indicates no Lamport time present
        }

        try {
            return Long.parseLong(lamportTimeStr);
        } catch (NumberFormatException e) {
            System.out.println("Invalid Lamport-Time header: " + lamportTimeStr);
            return -1;
        }
    }
}