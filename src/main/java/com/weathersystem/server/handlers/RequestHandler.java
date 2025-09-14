package com.weathersystem.server.handlers;

import com.weathersystem.server.http.HttpRequest;
import java.io.BufferedReader;
import java.io.PrintWriter;

// Interface defining the contract for handling HTTP requests in the weather system
public interface RequestHandler {

    // Handles incoming HTTP requests with Lamport timestamp for distributed ordering
    void handle(HttpRequest httpRequest, BufferedReader inputReader,
                PrintWriter outputWriter, long lamportTime) throws Exception;

}