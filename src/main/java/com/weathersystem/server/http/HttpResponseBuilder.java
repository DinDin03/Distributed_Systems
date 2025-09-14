package com.weathersystem.server.http;

import java.io.PrintWriter;

// Builder for constructing HTTP responses with Lamport timestamp support
public class HttpResponseBuilder {

    // Sends a simple success response with Lamport timestamp
    public void sendSuccessResponse(PrintWriter out, int statusCode, String statusText, long lamportTime) {
        out.print("HTTP/1.1 " + statusCode + " " + statusText + "\r\n");
        out.print("Lamport-Time: " + lamportTime + "\r\n");
        out.print("Content-Length: 0\r\n");
        out.print("\r\n");
        out.flush();
    }

    // Sends a JSON response with weather data and Lamport timestamp
    public void sendJsonResponse(PrintWriter out, int statusCode, String jsonData, long lamportTime) {
        byte[] jsonBytes = jsonData.getBytes();

        out.print("HTTP/1.1 " + statusCode + " OK\r\n");
        out.print("Content-Type: application/json\r\n");
        out.print("Lamport-Time: " + lamportTime + "\r\n");
        out.print("Content-Length: " + jsonBytes.length + "\r\n");
        out.print("\r\n");
        out.print(jsonData);
        out.flush();
    }

    // Sends an error response with Lamport timestamp
    public void sendErrorResponse(PrintWriter out, int statusCode, String statusText, long lamportTime) {
        out.print("HTTP/1.1 " + statusCode + " " + statusText + "\r\n");
        out.print("Lamport-Time: " + lamportTime + "\r\n");
        out.print("Content-Length: 0\r\n");
        out.print("\r\n");
        out.flush();
    }

}