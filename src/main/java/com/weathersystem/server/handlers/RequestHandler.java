package com.weathersystem.server.handlers;

import com.weathersystem.server.http.HttpRequest;
import java.io.BufferedReader;
import java.io.PrintWriter;

public interface RequestHandler {

    void handle(HttpRequest httpRequest, BufferedReader inputReader,
                PrintWriter outputWriter, long lamportTime) throws Exception;

    String getSupportedMethod();

    default boolean canHandle(HttpRequest httpRequest) {
        return getSupportedMethod().equalsIgnoreCase(httpRequest.getMethod());
    }
}