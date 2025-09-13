package com.weathersystem.server.network;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.*;
import java.net.Socket;

@Getter
@AllArgsConstructor
public class TimestampedRequest implements Comparable<TimestampedRequest> {
    private final Socket clientSocket;
    private final String method;
    private final int contentLength;
    private final long lamportTime;
    private final BufferedReader inputReader;
    private final PrintWriter outputWriter;


    // Compare based on Lamport timestamp for priority queue ordering
    @Override
    public int compareTo(TimestampedRequest other) {
        return Long.compare(this.lamportTime, other.lamportTime);
    }

}