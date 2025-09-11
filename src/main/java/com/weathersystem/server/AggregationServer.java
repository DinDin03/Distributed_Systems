package com.weathersystem.server;

import java.io.*;
import java.net.*;

public class AggregationServer {

    private static final int PORT = 4567;

    public static void main(String[] args){

        System.out.println("Aggregation Server starting on port " + PORT);

        try{
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server listening on port " + PORT);

            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected from: " + clientSocket.getRemoteSocketAddress());

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String message = in.readLine();
            System.out.println("Received from client: " + message);

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            out.println("Server received: " + message);

            clientSocket.close();
            serverSocket.close();
        }catch(IOException e){
            System.out.println("Server error: " + e.getMessage());
        }
        System.out.println("Server finished");
    }
}