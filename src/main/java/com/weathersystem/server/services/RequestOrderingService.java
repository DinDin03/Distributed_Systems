package com.weathersystem.server.services;

import com.weathersystem.server.network.TimestampedRequest;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class RequestOrderingService {

    private final PriorityBlockingQueue<TimestampedRequest> requestQueue;
    private final ExecutorService requestProcessorPool;
    private final Consumer<TimestampedRequest> requestProcessor;
    private final AtomicBoolean isRunning;

    public RequestOrderingService(Consumer<TimestampedRequest> requestProcessor) {
        this.requestQueue = new PriorityBlockingQueue<>();
        this.requestProcessorPool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "RequestOrderingService");
            t.setDaemon(false); // Keep JVM alive while processing requests
            return t;
        });
        this.requestProcessor = requestProcessor;
        this.isRunning = new AtomicBoolean(false);
    }

    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            // Create new executor if the previous one was shutdown
            if (requestProcessorPool.isShutdown()) {
                // Cannot restart a shutdown executor, but we can create a new service instance
                // For this implementation, we'll prevent restarting after shutdown
                isRunning.set(false);
                throw new IllegalStateException("Cannot restart RequestOrderingService after it has been stopped. Create a new instance.");
            }
            requestProcessorPool.submit(this::processRequestsInOrder);
            System.out.println("Request ordering service started - processing in Lamport timestamp order");
        }
    }

    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            if (requestProcessorPool != null && !requestProcessorPool.isShutdown()) {
                requestProcessorPool.shutdown();
                try {
                    // Wait for processing to complete
                    if (!requestProcessorPool.awaitTermination(5, TimeUnit.SECONDS)) {
                        requestProcessorPool.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    requestProcessorPool.shutdownNow();
                }
                System.out.println("Request ordering service stopped");
            }
        }
    }

    public void submitRequest(TimestampedRequest request) {
        requestQueue.offer(request);
        System.out.println("Queued " + request.getMethod() + " request with timestamp: " +
                request.getLamportTime() + " (Queue size: " + requestQueue.size() + ")");
    }

    public int getQueueSize() {
        return requestQueue.size();
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    private void processRequestsInOrder() {
        try {
            // Wait until the service is stopped to process all requests in order
            while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
                Thread.sleep(10); // Small sleep to avoid busy waiting
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Process all requests in timestamp order when shutting down
        System.out.println("Processing all queued requests in Lamport timestamp order");
        while (!requestQueue.isEmpty()) {
            try {
                TimestampedRequest request = requestQueue.poll();
                if (request != null) {
                    System.out.println("Processing " + request.getMethod() +
                            " request with Lamport time: " + request.getLamportTime());
                    requestProcessor.accept(request);
                }
            } catch (Exception e) {
                System.out.println("Error processing request: " + e.getMessage());
            }
        }

        System.out.println("Request ordering service processing loop ended");
    }

}