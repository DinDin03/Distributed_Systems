package com.weathersystem.shared.utils;

import com.weathersystem.shared.exceptions.NetworkException;

import java.io.IOException;
import java.net.*;
import java.util.regex.Pattern;

public class NetworkUtils {

    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?$"
    );

    public static boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }

    public static boolean isValidIpAddress(String ip) {
        return ip != null && IP_ADDRESS_PATTERN.matcher(ip).matches();
    }

    public static boolean isValidHostname(String hostname) {
        if (hostname == null || hostname.isEmpty() || hostname.length() > 253) {
            return false;
        }

        if (hostname.endsWith(".")) {
            hostname = hostname.substring(0, hostname.length() - 1);
        }

        String[] labels = hostname.split("\\.");
        for (String label : labels) {
            if (!HOSTNAME_PATTERN.matcher(label).matches()) {
                return false;
            }
        }

        return true;
    }

    public static boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isHostReachable(String host, int timeoutMs) {
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isReachable(timeoutMs);
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isServerListening(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static ServerAddress parseServerAddress(String address) throws NetworkException {
        if (address == null || address.trim().isEmpty()) {
            throw new NetworkException("Server address cannot be null or empty");
        }

        address = address.trim();

        // Remove protocol if present (http://, https://)
        if (address.startsWith("http://")) {
            address = address.substring(7);
        } else if (address.startsWith("https://")) {
            address = address.substring(8);
        }

        // Split host and port
        String host;
        int port;

        int colonIndex = address.lastIndexOf(':');
        if (colonIndex == -1) {
            throw new NetworkException("Server address must include port (format: host:port)");
        }

        host = address.substring(0, colonIndex);
        String portStr = address.substring(colonIndex + 1);

        // Validate host
        if (host.isEmpty()) {
            throw new NetworkException("Host cannot be empty");
        }

        if (!isValidIpAddress(host) && !isValidHostname(host) && !host.equals("localhost")) {
            throw new NetworkException("Invalid host format: " + host);
        }

        // Validate port
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            throw new NetworkException("Invalid port format: " + portStr);
        }

        if (!isValidPort(port)) {
            throw new NetworkException("Port must be between 1 and 65535: " + port);
        }

        return new ServerAddress(host, port);
    }

    public static String formatServerAddress(String host, int port) {
        return host + ":" + port;
    }

    public static InetAddress getLocalAddress() throws NetworkException {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new NetworkException("Cannot determine local host address", e);
        }
    }

    public static String getLocalHostName() throws NetworkException {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new NetworkException("Cannot determine local host name", e);
        }
    }

    public static class ServerAddress {
        private final String host;
        private final int port;

        public ServerAddress(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() { return host; }
        public int getPort() { return port; }

        public String toUrl() {
            return host + ":" + port;
        }

        public InetSocketAddress toSocketAddress() {
            return new InetSocketAddress(host, port);
        }

        @Override
        public String toString() {
            return toUrl();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ServerAddress that = (ServerAddress) obj;
            return port == that.port && host.equals(that.host);
        }

        @Override
        public int hashCode() {
            return host.hashCode() * 31 + port;
        }
    }
}