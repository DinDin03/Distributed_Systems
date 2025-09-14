package com.weathersystem.client.common;

import lombok.Getter;

@Getter
public class ClientConfiguration {

    private final String host;
    private final int port;
    private final String userAgent;
    private final int connectionTimeoutMs;
    private final int readTimeoutMs;

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 4567;
    private static final String DEFAULT_USER_AGENT = "WeatherClient/1.0";
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000;
    private static final int DEFAULT_READ_TIMEOUT = 10000;

    // Constructor to create client configuration with custom parameters
    public ClientConfiguration(String host, int port, String userAgent,
                               int connectionTimeoutMs, int readTimeoutMs) {
        this.host = host;
        this.port = port;
        this.userAgent = userAgent;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    // Factory method to create configuration from server address string
    public static ClientConfiguration fromServerAddress(String serverAddress) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (serverAddress != null && serverAddress.contains(":")) {
            String[] parts = serverAddress.split(":");
            host = parts[0];
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port in server address");
            }
        }

        return new ClientConfiguration(host, port, DEFAULT_USER_AGENT,
                DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    // Returns the server URL as a string (in my case it is localhost:4567)
    public String getServerUrl() {
        return host + ":" + port;
    }
}