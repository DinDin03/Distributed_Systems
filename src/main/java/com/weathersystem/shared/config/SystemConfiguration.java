package com.weathersystem.shared.config;

public class SystemConfiguration {

    // Server Configuration
    public static final int DEFAULT_SERVER_PORT = 4567;
    public static final String DEFAULT_SERVER_HOST = "localhost";
    public static final int DEFAULT_THREAD_POOL_SIZE = 10;

    // Data Storage Configuration
    public static final String DEFAULT_DATA_FILE = "data/weather.json";
    public static final String DEFAULT_BACKUP_FILE = "data/weather.json.backup";
    public static final String DATA_DIRECTORY = "data";

    // Timing Configuration
    public static final long DEFAULT_EXPIRY_TIME_MS = 30 * 1000; // 30 seconds
    public static final long DEFAULT_CLEANUP_INTERVAL_MS = 5 * 1000; // 5 seconds
    public static final long DEFAULT_SAVE_DELAY_MS = 1000; // 1 second

    // Network Configuration
    public static final int DEFAULT_CONNECTION_TIMEOUT_MS = 5000; // 5 seconds
    public static final int DEFAULT_READ_TIMEOUT_MS = 10000; // 10 seconds
    public static final String DEFAULT_USER_AGENT = "WeatherClient/1.0";

    // Retry Configuration
    public static final int DEFAULT_MAX_RETRIES = 3;
    public static final long DEFAULT_RETRY_BASE_DELAY_MS = 1000; // 1 second
    public static final double DEFAULT_RETRY_BACKOFF_MULTIPLIER = 2.0;

    // HTTP Configuration
    public static final String WEATHER_ENDPOINT = "/weather.json";
    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String HTTP_VERSION = "HTTP/1.1";
    public static final String LAMPORT_TIME_HEADER = "Lamport-Time";

    // Validation Configuration
    public static final int MAX_STATION_NAME_LENGTH = 100;
    public static final int MAX_STATION_ID_LENGTH = 20;
    public static final double MIN_LATITUDE = -90.0;
    public static final double MAX_LATITUDE = 90.0;
    public static final double MIN_LONGITUDE = -180.0;
    public static final double MAX_LONGITUDE = 180.0;

    // System Limits
    public static final int MAX_WEATHER_STATIONS = 1000;
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    public static final int MAX_JSON_DEPTH = 10;

    private SystemConfiguration() {
        // Utility class - prevent instantiation
    }

    public static class Server {
        public static int getPort() {
            return getIntProperty("weather.server.port", DEFAULT_SERVER_PORT);
        }

        public static String getHost() {
            return getStringProperty("weather.server.host", DEFAULT_SERVER_HOST);
        }

        public static int getThreadPoolSize() {
            return getIntProperty("weather.server.threads", DEFAULT_THREAD_POOL_SIZE);
        }
    }

    public static class Storage {
        public static String getDataFile() {
            return getStringProperty("weather.storage.datafile", DEFAULT_DATA_FILE);
        }

        public static String getBackupFile() {
            return getStringProperty("weather.storage.backupfile", DEFAULT_BACKUP_FILE);
        }

        public static long getExpiryTimeMs() {
            return getLongProperty("weather.storage.expiry.ms", DEFAULT_EXPIRY_TIME_MS);
        }

        public static long getCleanupIntervalMs() {
            return getLongProperty("weather.storage.cleanup.interval.ms", DEFAULT_CLEANUP_INTERVAL_MS);
        }
    }

    public static class Network {
        public static int getConnectionTimeoutMs() {
            return getIntProperty("weather.network.connection.timeout.ms", DEFAULT_CONNECTION_TIMEOUT_MS);
        }

        public static int getReadTimeoutMs() {
            return getIntProperty("weather.network.read.timeout.ms", DEFAULT_READ_TIMEOUT_MS);
        }

        public static String getUserAgent() {
            return getStringProperty("weather.network.useragent", DEFAULT_USER_AGENT);
        }
    }

    public static class Retry {
        public static int getMaxRetries() {
            return getIntProperty("weather.retry.max", DEFAULT_MAX_RETRIES);
        }

        public static long getBaseDelayMs() {
            return getLongProperty("weather.retry.delay.ms", DEFAULT_RETRY_BASE_DELAY_MS);
        }

        public static double getBackoffMultiplier() {
            return getDoubleProperty("weather.retry.backoff", DEFAULT_RETRY_BACKOFF_MULTIPLIER);
        }
    }

    private static String getStringProperty(String key, String defaultValue) {
        return System.getProperty(key, defaultValue);
    }

    private static int getIntProperty(String key, int defaultValue) {
        String value = System.getProperty(key);
        if (value == null) return defaultValue;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.out.println("Invalid integer property " + key + "=" + value + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    private static long getLongProperty(String key, long defaultValue) {
        String value = System.getProperty(key);
        if (value == null) return defaultValue;

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            System.out.println("Invalid long property " + key + "=" + value + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    private static double getDoubleProperty(String key, double defaultValue) {
        String value = System.getProperty(key);
        if (value == null) return defaultValue;

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            System.out.println("Invalid double property " + key + "=" + value + ", using default: " + defaultValue);
            return defaultValue;
        }
    }
}