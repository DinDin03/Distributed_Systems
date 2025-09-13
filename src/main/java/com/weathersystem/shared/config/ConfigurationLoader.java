package com.weathersystem.shared.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationLoader {

    private static final String DEFAULT_CONFIG_FILE = "weather-system.properties";
    private static Properties properties;
    private static boolean loaded = false;

    public static void loadConfiguration() {
        loadConfiguration(DEFAULT_CONFIG_FILE);
    }

    public static void loadConfiguration(String configFile) {
        if (loaded) {
            return;
        }

        properties = new Properties();

        // Try to load from classpath
        try (InputStream is = ConfigurationLoader.class.getClassLoader().getResourceAsStream(configFile)) {
            if (is != null) {
                properties.load(is);
                System.out.println("Loaded configuration from: " + configFile);

                // Set system properties for SystemConfiguration to use
                for (String key : properties.stringPropertyNames()) {
                    if (System.getProperty(key) == null) {
                        System.setProperty(key, properties.getProperty(key));
                    }
                }
            } else {
                System.out.println("Configuration file not found: " + configFile + " (using defaults)");
            }
        } catch (IOException e) {
            System.out.println("Error loading configuration: " + e.getMessage() + " (using defaults)");
        }

        loaded = true;
    }

    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    public static String getProperty(String key, String defaultValue) {
        if (!loaded) {
            loadConfiguration();
        }
        return properties.getProperty(key, defaultValue);
    }

    public static void setProperty(String key, String value) {
        if (!loaded) {
            loadConfiguration();
        }
        properties.setProperty(key, value);
        System.setProperty(key, value);
    }

    public static void printLoadedConfiguration() {
        if (!loaded) {
            loadConfiguration();
        }

        System.out.println("=== Weather System Configuration ===");
        System.out.println("Server Port: " + SystemConfiguration.Server.getPort());
        System.out.println("Thread Pool Size: " + SystemConfiguration.Server.getThreadPoolSize());
        System.out.println("Data File: " + SystemConfiguration.Storage.getDataFile());
        System.out.println("Expiry Time: " + SystemConfiguration.Storage.getExpiryTimeMs() / 1000 + "s");
        System.out.println("Connection Timeout: " + SystemConfiguration.Network.getConnectionTimeoutMs() + "ms");
        System.out.println("Max Retries: " + SystemConfiguration.Retry.getMaxRetries());
        System.out.println("=====================================");
    }
}