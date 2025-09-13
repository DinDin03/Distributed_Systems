package com.weathersystem.client.common;

import com.weathersystem.shared.clock.LamportClock;
import lombok.Getter;

import java.io.*;
import java.net.Socket;

public abstract class HttpClientBase {

    protected final ClientConfiguration config;
    protected final LamportClock lamportClock;
    protected final ResponseParser responseParser;

    public HttpClientBase(ClientConfiguration config) {
        this.config = config;
        this.lamportClock = new LamportClock();
        this.responseParser = new ResponseParser(lamportClock);
    }

    protected Socket createConnection() throws IOException {
        Socket socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(config.getHost(), config.getPort()),
                config.getConnectionTimeoutMs());
        socket.setSoTimeout(config.getReadTimeoutMs());

        System.out.println("Connected to server at " + config.getServerUrl());
        return socket;
    }

    protected void sendHttpRequest(Socket socket, String method,
                                   String contentType, byte[] content) throws IOException {
        long sendTime = lamportClock.tick();
        System.out.println("Sending " + method + " request (Lamport time: " + sendTime + ")");

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // Send request line and headers
        out.print(method + " " + "/weather.json" + " HTTP/1.1\r\n");
        out.print("Host: " + config.getServerUrl() + "\r\n");
        out.print("User-Agent: " + config.getUserAgent() + "\r\n");
        out.print("Lamport-Time: " + sendTime + "\r\n");

        if (content != null && content.length > 0) {
            out.print("Content-Type: " + contentType + "\r\n");
            out.print("Content-Length: " + content.length + "\r\n");
        }

        out.print("\r\n");
        out.flush();

        // Send body if present
        if (content != null && content.length > 0) {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(content);
            outputStream.flush();
        }
    }

    protected HttpResponse receiveHttpResponse(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return responseParser.parseResponse(in);
    }

    protected void closeConnection(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }

    public long getLamportTime() {
        return lamportClock.getTime();
    }

    @Getter
    public static class HttpResponse {
        private final int statusCode;
        private final String statusText;
        private final String content;
        private final long serverLamportTime;

        public HttpResponse(int statusCode, String statusText, String content, long serverLamportTime) {
            this.statusCode = statusCode;
            this.statusText = statusText;
            this.content = content;
            this.serverLamportTime = serverLamportTime;
        }

        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}