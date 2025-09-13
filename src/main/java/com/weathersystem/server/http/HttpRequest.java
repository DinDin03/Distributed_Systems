package com.weathersystem.server.http;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.Map;

@Getter
@AllArgsConstructor
public class HttpRequest {
    private final String method;           // GET, PUT, etc.
    private final String path;             // /weather.json
    private final String httpVersion;      // HTTP/1.1
    private final Map<String, String> headers;  // All HTTP headers
    private final int contentLength;       // Content-Length header value
    private final long lamportTime;        // Lamport-Time header value (-1 if not present)

}