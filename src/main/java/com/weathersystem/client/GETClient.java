package com.weathersystem.client;

import java.io.*;
import java.net.*;

public class GETClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 4567;

    public static void main(String[] args) {
        System.out.println("GET Client starting...");

        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            System.out.println("Connected to server at " + SERVER_HOST + ":" + SERVER_PORT);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("GET request from GET Client");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response = in.readLine();
            System.out.println("Server responded: " + response);

            socket.close();
            System.out.println("Connection closed");

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }

        System.out.println("GET Client finished");
    }
}