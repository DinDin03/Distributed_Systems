package com.weathersystem.client.common;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.function.Supplier;

public class RetryManager {

    private final int maxRetries;
    private final long baseDelayMs;
    private final double backoffMultiplier;

    public RetryManager(int maxRetries, long baseDelayMs, double backoffMultiplier) {
        this.maxRetries = Math.max(0, maxRetries);
        this.baseDelayMs = Math.max(100, baseDelayMs);
        this.backoffMultiplier = Math.max(1.0, backoffMultiplier);
    }

    public static RetryManager defaultRetry() {
        return new RetryManager(3, 1000, 2.0);
    }

    public <T> T executeWithRetry(Supplier<T> operation, String operationName) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                return operation.get();

            } catch (Exception e) {
                lastException = e;

                if (attempt <= maxRetries && isRetryable(e)) {
                    long delay = calculateDelay(attempt);
                    System.out.println(operationName + " failed (attempt " + attempt + "/" +
                            (maxRetries + 1) + "): " + e.getMessage() +
                            ". Retrying in " + delay + "ms...");

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Operation interrupted during retry", ie);
                    }
                } else {
                    break;
                }
            }
        }

        System.out.println(operationName + " failed after " + (maxRetries + 1) + " attempts");
        throw lastException;
    }

    private boolean isRetryable(Exception e) {
        return e instanceof IOException ||
                e instanceof SocketTimeoutException ||
                (e instanceof RuntimeException && e.getCause() instanceof IOException);
    }

    private long calculateDelay(int attempt) {
        return (long) (baseDelayMs * Math.pow(backoffMultiplier, attempt - 1));
    }
}