package com.weathersystem.server.network;

import com.weathersystem.server.http.HttpRequestParser;
import com.weathersystem.server.http.HttpRequest;
import com.weathersystem.shared.clock.LamportClock;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

// Manages client connections using a thread pool and handles HTTP requests
public class ConnectionManager {

    private final int threadPoolSize;
    private final LamportClock lamportClock;
    private final Consumer<TimestampedRequest> requestSubmitter;
    private ExecutorService connectionHandlerPool;

    // Sets up the connection manager with thread pool size, clock, and request submitter
    public ConnectionManager(int threadPoolSize, LamportClock lamportClock,
                             Consumer<TimestampedRequest> requestSubmitter) {
        this.threadPoolSize = threadPoolSize;
        this.lamportClock = lamportClock;
        this.requestSubmitter = requestSubmitter;
    }

    // Starts the connection manager by creating a thread pool
    public void start() {
        connectionHandlerPool = Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r, "ConnectionHandler");
            t.setDaemon(false); // Keep JVM alive while handling connections
            return t;
        });
        System.out.println("Connection manager started with " + threadPoolSize + " threads");
    }

    // Handles a new client connection by giving it to a thread
    public void handleConnection(Socket clientSocket) {
        if (connectionHandlerPool == null || connectionHandlerPool.isShutdown()) {
            System.out.println("Connection manager not started or already shutdown");
            closeSocket(clientSocket);
            return;
        }

        connectionHandlerPool.submit(new ConnectionHandler(clientSocket));
    }

    // Shuts down the connection manager and waits for threads to finish
    public void shutdown() {
        if (connectionHandlerPool != null && !connectionHandlerPool.isShutdown()) {
            connectionHandlerPool.shutdown();
            try {
                if (!connectionHandlerPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    connectionHandlerPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                connectionHandlerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("Connection manager shutdown complete");
        }
    }

    // Handles individual client connections in separate threads
    private class ConnectionHandler implements Runnable {
        private final Socket clientSocket;

        // Sets up the connection handler with a client socket
        public ConnectionHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        // Main method that processes the client connection
        @Override
        public void run() {
            System.out.println("Client connected from: " + clientSocket.getRemoteSocketAddress());

            BufferedReader in = null;
            PrintWriter out = null;

            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                // Parse HTTP request from client input stream
                HttpRequestParser parser = new HttpRequestParser();
                HttpRequest httpRequest;

                try {
                    httpRequest = parser.parseRequest(in);
                } catch (Exception e) {
                    System.out.println("Failed to parse HTTP request: " + e.getMessage());
                    closeStreams(in, out);
                    return;
                }

                System.out.println("Parsed request: " + httpRequest.getMethod() + " " +
                        httpRequest.getPath() + " (Thread: " + Thread.currentThread().getName() + ")");

                // Update server's Lamport clock based on client timestamp
                long updatedTime = updateServerClock(httpRequest.getLamportTime());

                // Create timestamped request and submit for processing
                TimestampedRequest timestampedRequest = new TimestampedRequest(
                        clientSocket, httpRequest.getMethod(), httpRequest.getContentLength(),
                        updatedTime, in, out
                );

                requestSubmitter.accept(timestampedRequest);

            } catch (Exception e) {
                System.out.println("Error in connection handler: " + e.getMessage());
                closeStreams(in, out);
            }
        }

        // Updates the server clock based on the client timestamp
        private long updateServerClock(long clientLamportTime) {
            long updatedTime;

            if (clientLamportTime != -1) {
                updatedTime = lamportClock.update(clientLamportTime);
                System.out.println("Updated server Lamport clock: " + clientLamportTime +
                        " -> " + updatedTime);
            } else {
                updatedTime = lamportClock.tick();
                System.out.println("Client without Lamport timestamp, server time: " + updatedTime);
            }

            return updatedTime;
        }
    }

    // Closes the client socket properly
    private void closeSocket(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing socket: " + e.getMessage());
        }
    }

    // Closes the input and output streams properly
    private void closeStreams(BufferedReader in, PrintWriter out) {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing input stream: " + e.getMessage());
        }

        try {
            if (out != null) {
                out.close();
            }
        } catch (Exception e) {
            System.out.println("Error closing output stream: " + e.getMessage());
        }
    }
}