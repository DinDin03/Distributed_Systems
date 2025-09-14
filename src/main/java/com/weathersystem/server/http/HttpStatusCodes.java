package com.weathersystem.server.http;

public class HttpStatusCodes {

    // Success codes
    public static final int OK = 200;
    public static final String OK_TEXT = "OK";

    public static final int CREATED = 201;
    public static final String CREATED_TEXT = "Created";

    public static final int NO_CONTENT = 204;
    public static final String NO_CONTENT_TEXT = "No Content";

    // Client error codes
    public static final int BAD_REQUEST = 400;
    public static final String BAD_REQUEST_TEXT = "Bad Request";

    // Server error codes
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final String INTERNAL_SERVER_ERROR_TEXT = "Internal Server Error";

}