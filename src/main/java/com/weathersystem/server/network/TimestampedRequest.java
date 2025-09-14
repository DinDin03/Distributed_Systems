package com.weathersystem.server.network;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.*;
import java.net.Socket;

@Getter
@AllArgsConstructor
// Wraps HTTP request with Lamport timestamp for ordered processing
public class TimestampedRequest implements Comparable<TimestampedRequest> {
    private final Socket clientSocket;
    private final String method;
    private final int contentLength;
    private final long lamportTime;
    private final BufferedReader inputReader;
    private final PrintWriter outputWriter;


    // Compares requests by Lamport time so older requests are processed first
    // Compares requests by Lamport time so older requests get processed first
    @Override
    public int compareTo(TimestampedRequest other) {
        return Long.compare(this.lamportTime, other.lamportTime);
    }

}