package com.weathersystem.server.services;

import com.weathersystem.server.network.TimestampedRequest;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
            requestProcessorPool.submit(this::processRequestsInOrder);
            System.out.println("Request ordering service started - processing in Lamport timestamp order");
        }
    }

    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            if (requestProcessorPool != null && !requestProcessorPool.isShutdown()) {
                requestProcessorPool.shutdown();
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

    private void processRequestsInOrder() {
        while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // Take requests in timestamp order (blocking call)
                TimestampedRequest request = requestQueue.take();

                System.out.println("Processing " + request.getMethod() +
                        " request with Lamport time: " + request.getLamportTime());

                // Process the request using the provided processor function
                requestProcessor.accept(request);

            } catch (InterruptedException e) {
                // Thread was interrupted, likely during shutdown
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Log error but continue processing other requests
                System.out.println("Error processing request: " + e.getMessage());
            }
        }

        System.out.println("Request ordering service processing loop ended");
    }

}