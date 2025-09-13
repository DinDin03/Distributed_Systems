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

public class ConnectionManager {

    private final int threadPoolSize;
    private final LamportClock lamportClock;
    private final Consumer<TimestampedRequest> requestSubmitter;
    private ExecutorService connectionHandlerPool;

    public ConnectionManager(int threadPoolSize, LamportClock lamportClock,
                             Consumer<TimestampedRequest> requestSubmitter) {
        this.threadPoolSize = threadPoolSize;
        this.lamportClock = lamportClock;
        this.requestSubmitter = requestSubmitter;
    }

    public void start() {
        connectionHandlerPool = Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r, "ConnectionHandler");
            t.setDaemon(false); // Keep JVM alive while handling connections
            return t;
        });
        System.out.println("Connection manager started with " + threadPoolSize + " threads");
    }

    public void handleConnection(Socket clientSocket) {
        if (connectionHandlerPool == null || connectionHandlerPool.isShutdown()) {
            System.out.println("Connection manager not started or already shutdown");
            closeSocket(clientSocket);
            return;
        }

        connectionHandlerPool.submit(new ConnectionHandler(clientSocket));
    }

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

    private class ConnectionHandler implements Runnable {
        private final Socket clientSocket;

        public ConnectionHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            System.out.println("Client connected from: " + clientSocket.getRemoteSocketAddress() +
                    " (Thread: " + Thread.currentThread().getName() + ")");

            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), false)) {

                // Parse HTTP request
                HttpRequestParser parser = new HttpRequestParser();
                HttpRequest httpRequest;

                try {
                    httpRequest = parser.parseRequest(in);
                } catch (Exception e) {
                    System.out.println("Failed to parse HTTP request: " + e.getMessage());
                    return;
                }

                System.out.println("Parsed request: " + httpRequest.getMethod() + " " +
                        httpRequest.getPath() + " (Thread: " + Thread.currentThread().getName() + ")");

                // Update server's Lamport clock
                long updatedTime = updateServerClock(httpRequest.getLamportTime());

                // Create timestamped request and submit for processing
                TimestampedRequest timestampedRequest = new TimestampedRequest(
                        clientSocket, httpRequest.getMethod(), httpRequest.getContentLength(),
                        updatedTime, in, out
                );

                requestSubmitter.accept(timestampedRequest);

            } catch (Exception e) {
                System.out.println("Error in connection handler: " + e.getMessage());
            }
        }

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

    private void closeSocket(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing socket: " + e.getMessage());
        }
    }
}