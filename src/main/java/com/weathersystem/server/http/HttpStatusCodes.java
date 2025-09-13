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

    public static final int NOT_FOUND = 404;
    public static final String NOT_FOUND_TEXT = "Not Found";

    public static final int METHOD_NOT_ALLOWED = 405;
    public static final String METHOD_NOT_ALLOWED_TEXT = "Method Not Allowed";

    // Server error codes
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final String INTERNAL_SERVER_ERROR_TEXT = "Internal Server Error";

    public static final int SERVICE_UNAVAILABLE = 503;
    public static final String SERVICE_UNAVAILABLE_TEXT = "Service Unavailable";

    public static String getStatusText(int statusCode) {
        switch (statusCode) {
            case OK: return OK_TEXT;
            case CREATED: return CREATED_TEXT;
            case NO_CONTENT: return NO_CONTENT_TEXT;
            case BAD_REQUEST: return BAD_REQUEST_TEXT;
            case NOT_FOUND: return NOT_FOUND_TEXT;
            case METHOD_NOT_ALLOWED: return METHOD_NOT_ALLOWED_TEXT;
            case INTERNAL_SERVER_ERROR: return INTERNAL_SERVER_ERROR_TEXT;
            case SERVICE_UNAVAILABLE: return SERVICE_UNAVAILABLE_TEXT;
            default: return "Unknown Status";
        }
    }
}