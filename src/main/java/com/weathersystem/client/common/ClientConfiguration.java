package com.weathersystem.client.common;

import lombok.Getter;
import java.util.List;
import java.util.ArrayList;

// Holds all the settings for connecting to servers
@Getter
public class ClientConfiguration {

    private final String host;
    private final int port;
    private final List<String> serverAddresses; 
    private final String userAgent;
    private final int connectionTimeoutMs;
    private final int readTimeoutMs;

    // Default values we use when nothing else is specified
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 4567;
    private static final String DEFAULT_USER_AGENT = "WeatherClient/1.0";
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000;
    private static final int DEFAULT_READ_TIMEOUT = 10000;

    // Creates a config with specific settings
    public ClientConfiguration(String host, int port, String userAgent,
                               int connectionTimeoutMs, int readTimeoutMs) {
        this.host = host;
        this.port = port;
        this.serverAddresses = List.of(host + ":" + port);
        this.userAgent = userAgent;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    // Creates a config that can try multiple servers if one stuffs up
    public ClientConfiguration(List<String> serverAddresses, String userAgent,
                               int connectionTimeoutMs, int readTimeoutMs) {
        if (serverAddresses == null || serverAddresses.isEmpty()) {
            throw new IllegalArgumentException("Server addresses cannot be null or empty");
        }

        // Use first server for backward compatibility
        String firstServer = serverAddresses.get(0);
        String[] parts = firstServer.split(":");
        this.host = parts.length > 0 ? parts[0] : DEFAULT_HOST;
        this.port = parts.length > 1 ? Integer.parseInt(parts[1]) : DEFAULT_PORT;

        this.serverAddresses = new ArrayList<>(serverAddresses);
        this.userAgent = userAgent;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    // Creates a config from a single server address like "localhost:4567"
    public static ClientConfiguration fromServerAddress(String serverAddress) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (serverAddress != null && serverAddress.contains(":")) {
            String[] parts = serverAddress.split(":");
            if (parts.length >= 2) {
                host = parts[0];
                try {
                    port = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid port in server address");
                }
            } else if (parts.length == 1) {
                host = parts[0];
            }
        }

        return new ClientConfiguration(host, port, DEFAULT_USER_AGENT,
                DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    // Creates a config from multiple server addresses separated by commas
    public static ClientConfiguration fromMultipleServers(String serverAddresses) {
        if (serverAddresses == null || serverAddresses.trim().isEmpty()) {
            return fromServerAddress(null); // Use defaults
        }

        // Parse comma separated server addresses
        String[] addresses = serverAddresses.split(",");
        List<String> serverList = new ArrayList<>();

        for (String addr : addresses) {
            addr = addr.trim();
            if (!addr.isEmpty()) {
                // Ensure address has port, add default if missing
                if (!addr.contains(":")) {
                    addr += ":" + DEFAULT_PORT;
                }
                serverList.add(addr);
            }
        }

        if (serverList.isEmpty()) {
            serverList.add(DEFAULT_HOST + ":" + DEFAULT_PORT);
        }

        return new ClientConfiguration(serverList, DEFAULT_USER_AGENT,
                DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    // Returns the server address as a string
    public String getServerUrl() {
        return host + ":" + port;
    }

    // Gets the first server address from the list
    public String getPrimaryServerAddress() {
        return serverAddresses.get(0);
    }

    // Checks if we have more than one server set up
    public boolean hasMultipleServers() {
        return serverAddresses.size() > 1;
    }
}