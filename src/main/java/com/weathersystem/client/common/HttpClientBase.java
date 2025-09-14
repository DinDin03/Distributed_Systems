package com.weathersystem.client.common;

import com.weathersystem.shared.clock.LamportClock;
import lombok.Getter;

import java.io.*;
import java.net.Socket;

public abstract class HttpClientBase {

    protected final ClientConfiguration config; // Client configuration settings
    protected final LamportClock lamportClock; // Lamport clock for distributed ordering
    protected final ResponseParser responseParser; // Parser for HTTP responses

    // Constructor initializes client configuration, Lamport clock, and response parser
    public HttpClientBase(ClientConfiguration config) {
        this.config = config;
        this.lamportClock = new LamportClock();
        this.responseParser = new ResponseParser(lamportClock);
    }

    // Creates a socket connection to the server with configured timeouts
    protected Socket createConnection() throws IOException {
        Socket socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(config.getHost(), config.getPort()),
                config.getConnectionTimeoutMs());
        socket.setSoTimeout(config.getReadTimeoutMs());

        System.out.println("Connected to server at " + config.getServerUrl());
        return socket;
    }

    // Sends HTTP request with headers and optional body content
    protected void sendHttpRequest(Socket socket, String method,
                                   String contentType, byte[] content) throws IOException {
        long sendTime = lamportClock.tick();
        System.out.println("Sending " + method + " request (Lamport time: " + sendTime + ")");

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // Send request line and headers
        out.print(method + " " + "/weather.json" + " HTTP/1.1\r\n");
        out.print("Host: " + config.getServerUrl() + "\r\n");
        out.print("User-Agent: " + config.getUserAgent() + "\r\n");
        out.print("Lamport-Time: " + sendTime + "\r\n");

        if (content != null && content.length > 0) {
            out.print("Content-Type: " + contentType + "\r\n");
            out.print("Content-Length: " + content.length + "\r\n");
        }

        out.print("\r\n");
        out.flush();

        // Send body if present
        if (content != null && content.length > 0) {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(content);
            outputStream.flush();
        }
    }

    // Receives and parses HTTP response from the server
    protected HttpResponse receiveHttpResponse(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return responseParser.parseResponse(in);
    }

    // Safely closes the socket connection
    protected void closeConnection(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }

    // Returns the current Lamport clock time
    public long getLamportTime() {
        return lamportClock.getTime();
    }

    @Getter
    public static class HttpResponse {
        private final int statusCode; // HTTP status code
        private final String statusText; // HTTP status text
        private final String content; // Response body content
        private final long serverLamportTime; // Server's Lamport clock time

        // Constructor creates HTTP response object with all fields
        public HttpResponse(int statusCode, String statusText, String content, long serverLamportTime) {
            this.statusCode = statusCode;
            this.statusText = statusText;
            this.content = content;
            this.serverLamportTime = serverLamportTime;
        }

        // Checks if the response indicates success (2xx status codes)
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}