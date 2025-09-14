package com.weathersystem.client.common;

import com.weathersystem.shared.clock.LamportClock;

import java.io.BufferedReader;
import java.io.IOException;

// Parser for HTTP responses with Lamport clock synchronization support
public class ResponseParser {

    private final LamportClock lamportClock;

    // Constructor that initialises the parser with Lamport clock for synchronization
    public ResponseParser(LamportClock lamportClock) {
        this.lamportClock = lamportClock;
    }

    // Parses HTTP response from input stream and updates Lamport clock
    public HttpClientBase.HttpResponse parseResponse(BufferedReader in) throws IOException {
        // Read status line
        String statusLine = in.readLine();
        if (statusLine == null) {
            throw new IOException("No response received from server");
        }

        System.out.println("Server response: " + statusLine);

        // Parse status code and text from HTTP status line
        String[] statusParts = statusLine.split(" ", 3);
        int statusCode = Integer.parseInt(statusParts[1]);
        String statusText = statusParts.length > 2 ? statusParts[2] : "";

        // Read headers and extract important values
        String headerLine;
        int contentLength = 0;
        long serverLamportTime = -1;

        while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
            String[] headerParts = headerLine.split(":", 2);
            if (headerParts.length != 2) continue;

            String headerName = headerParts[0].trim().toLowerCase();
            String headerValue = headerParts[1].trim();

            switch (headerName) {
                case "content-length":
                    contentLength = Integer.parseInt(headerValue);
                    break;
                case "lamport-time":
                    serverLamportTime = Long.parseLong(headerValue);
                    break;
            }
        }

        // Update Lamport clock if server sent timestamp
        if (serverLamportTime != -1) {
            long updatedTime = lamportClock.update(serverLamportTime);
            System.out.println("Updated Lamport clock from server response: " +
                    serverLamportTime + " to " + updatedTime);
        }

        // Read content body based on Content-Length header
        String content = null;
        if (contentLength > 0) {
            char[] contentChars = new char[contentLength];
            int totalRead = 0;

            while (totalRead < contentLength) {
                int read = in.read(contentChars, totalRead, contentLength - totalRead);
                if (read == -1) {
                    throw new IOException("Unexpected end of response while reading content");
                }
                totalRead += read;
            }

            content = new String(contentChars);
        }

        return new HttpClientBase.HttpResponse(statusCode, statusText, content, serverLamportTime);
    }
}