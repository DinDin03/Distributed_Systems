package com.weathersystem.shared.exceptions;

public class NetworkException extends WeatherSystemException {

    private final String serverAddress;
    private final int statusCode;

    public NetworkException(String message) {
        super("NETWORK_ERROR", message);
        this.serverAddress = null;
        this.statusCode = -1;
    }

    public NetworkException(String message, Throwable cause) {
        super("NETWORK_ERROR", message, cause);
        this.serverAddress = null;
        this.statusCode = -1;
    }

    public NetworkException(String serverAddress, String message) {
        super("NETWORK_ERROR", message + " (server: " + serverAddress + ")");
        this.serverAddress = serverAddress;
        this.statusCode = -1;
    }

    public NetworkException(String serverAddress, String message, Throwable cause) {
        super("NETWORK_ERROR", message + " (server: " + serverAddress + ")", cause);
        this.serverAddress = serverAddress;
        this.statusCode = -1;
    }

    public NetworkException(String serverAddress, int statusCode, String message) {
        super("NETWORK_ERROR", message + " (server: " + serverAddress + ", status: " + statusCode + ")");
        this.serverAddress = serverAddress;
        this.statusCode = statusCode;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean hasServerAddress() {
        return serverAddress != null;
    }

    public boolean hasStatusCode() {
        return statusCode != -1;
    }

    public static NetworkException connectionTimeout(String serverAddress) {
        return new NetworkException(serverAddress, "Connection timeout");
    }

    public static NetworkException connectionRefused(String serverAddress) {
        return new NetworkException(serverAddress, "Connection refused");
    }

    public static NetworkException httpError(String serverAddress, int statusCode, String message) {
        return new NetworkException(serverAddress, statusCode, "HTTP error: " + message);
    }

    public static NetworkException readTimeout(String serverAddress) {
        return new NetworkException(serverAddress, "Read timeout");
    }
}