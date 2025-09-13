package com.weathersystem.server.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestParser {

    public HttpRequest parseRequest(BufferedReader input) throws IOException {
        // Read and parse the request line
        String requestLine = input.readLine();
        if (requestLine == null || requestLine.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty request line");
        }

        String[] requestParts = parseRequestLine(requestLine);
        String method = requestParts[0];
        String path = requestParts[1];
        String httpVersion = requestParts[2];

        // Parse headers
        Map<String, String> headers = parseHeaders(input);

        // Extract important header values
        int contentLength = extractContentLength(headers);
        long lamportTime = extractLamportTime(headers);

        return new HttpRequest(method, path, httpVersion, headers, contentLength, lamportTime);
    }

    private String[] parseRequestLine(String requestLine) {
        String[] parts = requestLine.split(" ");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid request line format: " + requestLine);
        }
        return parts;
    }

    private Map<String, String> parseHeaders(BufferedReader input) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String headerLine;

        while ((headerLine = input.readLine()) != null && !headerLine.isEmpty()) {
            int colonIndex = headerLine.indexOf(':');
            if (colonIndex == -1) {
                // Skip malformed headers
                continue;
            }

            String headerName = headerLine.substring(0, colonIndex).trim().toLowerCase();
            String headerValue = headerLine.substring(colonIndex + 1).trim();

            headers.put(headerName, headerValue);
        }

        return headers;
    }

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