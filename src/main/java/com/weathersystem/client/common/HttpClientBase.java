package com.weathersystem.client.common;

import com.weathersystem.shared.clock.LamportClock;
import lombok.Getter;

import java.io.*;
import java.net.Socket;

public abstract class HttpClientBase {

    protected final ClientConfiguration config; // Client configuration settings
    protected final LamportClock lamportClock; // Lamport clock for distributed ordering
    protected final ResponseParser responseParser; // Parser for HTTP responses

    // Sets up the client with config, clock, and response parser
    public HttpClientBase(ClientConfiguration config) {
        this.config = config;
        this.lamportClock = new LamportClock();
        this.responseParser = new ResponseParser(lamportClock);
    }

    // Connects to the server using the settings in config
    protected Socket createConnection() throws IOException {
        if (config.hasMultipleServers()) {
            return createConnectionWithFailover();
        } else {
            return createSingleConnection(config.getHost(), config.getPort());
        }
    }

    // Tries to connect to each server until one works
    protected Socket createConnectionWithFailover() throws IOException {
        IOException lastException = null;

        for (String serverAddress : config.getServerAddresses()) {
            String[] parts = serverAddress.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            try {
                Socket socket = createSingleConnection(host, port);
                System.out.println("Connected to " + serverAddress);
                return socket;
            } catch (IOException e) {
                System.out.println("Failed to connect to " + serverAddress + ": " + e.getMessage());
                lastException = e;
            }
        }

        // All servers failed
        throw new IOException("All servers failed to connect. Last error: ");
    }

    // Connects to one specific server
    private Socket createSingleConnection(String host, int port) throws IOException {
        Socket socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(host, port),
                config.getConnectionTimeoutMs());
        socket.setSoTimeout(config.getReadTimeoutMs());

        if (!config.hasMultipleServers()) {
            System.out.println("Connected to " + host + ":" + port);
        }
        return socket;
    }

    // Sends a request to the server with headers and maybe some data
    protected void sendHttpRequest(Socket socket, String method,
                                   String contentType, byte[] content) throws IOException {
        long sendTime = lamportClock.tick();
        System.out.println("Sending " + method + " request (time: " + sendTime + ")");

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

    // Gets the response back from the server and parses it
    protected HttpResponse receiveHttpResponse(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return responseParser.parseResponse(in);
    }

    // Closes the connection properly
    protected void closeConnection(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }

    // Gets the current time from the Lamport clock
    public long getLamportTime() {
        return lamportClock.getTime();
    }

    @Getter
    public static class HttpResponse {
        private final int statusCode; // HTTP status code
        private final String statusText; // HTTP status text
        private final String content; // Response body content
        private final long serverLamportTime; // Server's Lamport clock time

        // Creates a response object with all the bits
        public HttpResponse(int statusCode, String statusText, String content, long serverLamportTime) {
            this.statusCode = statusCode;
            this.statusText = statusText;
            this.content = content;
            this.serverLamportTime = serverLamportTime;
        }

        // Checks if the response was successful 
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}