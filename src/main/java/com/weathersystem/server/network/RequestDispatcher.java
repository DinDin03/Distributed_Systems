package com.weathersystem.server.network;

import com.weathersystem.server.handlers.RequestHandler;
import com.weathersystem.server.http.HttpRequest;
import com.weathersystem.server.http.HttpRequestParser;
import com.weathersystem.server.http.HttpResponseBuilder;
import com.weathersystem.server.http.HttpStatusCodes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RequestDispatcher {

    private final Map<String, RequestHandler> handlers;
    private final HttpResponseBuilder responseBuilder;

    public RequestDispatcher() {
        this.handlers = new HashMap<>();
        this.responseBuilder = new HttpResponseBuilder();
    }

    public void registerHandler(String method, RequestHandler handler) {
        if (method == null || handler == null) {
            throw new IllegalArgumentException("Method and handler cannot be null");
        }

        handlers.put(method.toUpperCase(), handler);
        System.out.println("Registered handler for " + method.toUpperCase() + " requests: " +
                handler.getClass().getSimpleName());
    }

    public void processRequest(TimestampedRequest request) {
        try {
            // Parse the HTTP request from the timestamped request
            HttpRequestParser parser = new HttpRequestParser();

            // For already parsed requests, we need to reconstruct from method
            // This is a bit of architectural debt - ideally we'd pass HttpRequest through
            String method = request.getMethod().toUpperCase();

            // Find appropriate handler
            RequestHandler handler = handlers.get(method);

            if (handler == null) {
                System.out.println("No handler found for method: " + method);
                responseBuilder.sendErrorResponse(request.getOutputWriter(),
                        HttpStatusCodes.METHOD_NOT_ALLOWED,
                        HttpStatusCodes.METHOD_NOT_ALLOWED_TEXT,
                        request.getLamportTime());
                return;
            }

            // Create a minimal HttpRequest for the handler
            // This is a temporary solution - in a full refactor we'd pass HttpRequest objects
            HttpRequest httpRequest = createHttpRequestFromTimestampedRequest(request);

            System.out.println("Dispatching " + method + " request to " +
                    handler.getClass().getSimpleName());

            // Dispatch to handler
            handler.handle(httpRequest, request.getInputReader(),
                    request.getOutputWriter(), request.getLamportTime());

        } catch (Exception e) {
            System.out.println("Error dispatching request: " + e.getMessage());
            e.printStackTrace();

            try {
                responseBuilder.sendErrorResponse(request.getOutputWriter(),
                        HttpStatusCodes.INTERNAL_SERVER_ERROR,
                        HttpStatusCodes.INTERNAL_SERVER_ERROR_TEXT,
                        request.getLamportTime());
            } catch (Exception responseError) {
                System.out.println("Failed to send error response: " + responseError.getMessage());
            }

        } finally {
            // Close the client socket after processing
            closeClientSocket(request);
        }
    }

    public int getHandlerCount() {
        return handlers.size();
    }

    private HttpRequest createHttpRequestFromTimestampedRequest(TimestampedRequest request) {
        // Create minimal headers map
        Map<String, String> headers = new HashMap<>();
        headers.put("content-length", String.valueOf(request.getContentLength()));
        headers.put("lamport-time", String.valueOf(request.getLamportTime()));

        // Create HttpRequest with available information
        return new com.weathersystem.server.http.HttpRequest(
                request.getMethod(),
                "/weather.json", // Default path - this would come from proper HTTP parsing
                "HTTP/1.1",     // Default version
                headers,
                request.getContentLength(),
                request.getLamportTime()
        );
    }

    private void closeClientSocket(TimestampedRequest request) {
        try {
            if (request.getClientSocket() != null && !request.getClientSocket().isClosed()) {
                request.getClientSocket().close();
                System.out.println("Client disconnected after processing Lamport time: " +
                        request.getLamportTime());
            }
        } catch (IOException e) {
            System.out.println("Error closing client socket: " + e.getMessage());
        }
    }
}