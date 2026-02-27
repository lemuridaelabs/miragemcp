package com.lemuridaelabs.miragemcp.web.error;

import com.lemuridaelabs.miragemcp.modules.events.dto.HoneyEventType;
import com.lemuridaelabs.miragemcp.modules.events.service.EventLoggingService;
import com.lemuridaelabs.miragemcp.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for all REST and Web endpoints.
 * Ensures no stack traces or sensitive error details are exposed to clients.
 * Logs all exceptions for monitoring and debugging.
 */
@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final EventLoggingService eventLoggingService;

    private final JsonMapper jsonMapper;

    /**
     * Builds a JSON string containing request details for event logging.
     */
    private String buildEventData(HttpServletRequest request, String exceptionType, String exceptionMessage) {
        var dataMap = new LinkedHashMap<String, Object>();
        dataMap.put("method", request.getMethod());
        dataMap.put("userAgent", request.getHeader("User-Agent"));
        dataMap.put("acceptLanguage", request.getHeader("Accept-Language"));
        dataMap.put("exceptionType", exceptionType);
        dataMap.put("exceptionMessage", exceptionMessage);

        try {
            return jsonMapper.writeValueAsString(dataMap);
        } catch (JacksonException e) {
            log.warn("Failed to serialize event data to JSON", e);
            return "{}";
        }
    }

    /**
     * Handles async request timeouts (normal for SSE connections).
     * Does not log an event as this is expected behavior.
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseEntity<Void> handleAsyncTimeout(AsyncRequestTimeoutException ex) {
        // No logging - this is normal for SSE/streaming connections
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    /**
     * Handles all unhandled exceptions and prevents stack traces from being exposed.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex, HttpServletRequest request) {
        var remoteIp = RequestUtils.getEffectiveRemoteIp(request);
        var uri = request.getRequestURI();

        log.error("Unhandled exception for request from IP: {}, URI: {}", remoteIp, uri, ex);

        // Log as high-severity event - wrapped in try-catch to prevent cascade failure
        // if the database is unavailable
        try {
            eventLoggingService.highEvent(
                    remoteIp,
                    uri,
                    HoneyEventType.HTTP,
                    false,
                    "Internal server error: " + ex.getClass().getSimpleName(),
                    buildEventData(request, ex.getClass().getName(), ex.getMessage())
            );
        } catch (Exception loggingEx) {
            log.warn("Failed to log event to database: {}", loggingEx.getMessage());
        }

        var errorResponse = new HashMap<String, Object>();
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", "An error occurred processing your request");
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("path", uri);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    /**
     * Handles NullPointerException specifically.
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleNullPointerException(NullPointerException ex, HttpServletRequest request) {
        var remoteIp = RequestUtils.getEffectiveRemoteIp(request);
        var uri = request.getRequestURI();

        log.error("NullPointerException for request from IP: {}, URI: {}", remoteIp, uri, ex);

        try {
            eventLoggingService.highEvent(
                    remoteIp,
                    uri,
                    HoneyEventType.HTTP,
                    false,
                    "NullPointerException occurred",
                    buildEventData(request, ex.getClass().getName(), ex.getMessage())
            );
        } catch (Exception loggingEx) {
            log.warn("Failed to log event to database: {}", loggingEx.getMessage());
        }

        var errorResponse = new HashMap<String, Object>();
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", "An error occurred processing your request");
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("path", uri);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    /**
     * Handles IllegalArgumentException - typically bad input.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        var remoteIp = RequestUtils.getEffectiveRemoteIp(request);
        var uri = request.getRequestURI();

        log.warn("IllegalArgumentException for request from IP: {}, URI: {}, Message: {}", remoteIp, uri, ex.getMessage());

        try {
            eventLoggingService.mediumEvent(
                    remoteIp,
                    uri,
                    HoneyEventType.HTTP,
                    false,
                    "Invalid request parameters",
                    buildEventData(request, ex.getClass().getName(), ex.getMessage())
            );
        } catch (Exception loggingEx) {
            log.warn("Failed to log event to database: {}", loggingEx.getMessage());
        }

        var errorResponse = new HashMap<String, Object>();
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", "Invalid request parameters");
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("path", uri);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * Handles 404 Not Found.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoHandlerFoundException ex, HttpServletRequest request) {
        var remoteIp = RequestUtils.getEffectiveRemoteIp(request);
        var uri = request.getRequestURI();

        log.debug("404 Not Found for request from IP: {}, URI: {}", remoteIp, uri);

        try {
            eventLoggingService.lowEvent(
                    remoteIp,
                    uri,
                    HoneyEventType.HTTP,
                    false,
                    "Resource not found",
                    buildEventData(request, ex.getClass().getName(), ex.getMessage())
            );
        } catch (Exception loggingEx) {
            log.warn("Failed to log event to database: {}", loggingEx.getMessage());
        }

        var errorResponse = new HashMap<String, Object>();
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", "The requested resource was not found");
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("path", uri);

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    /**
     * Handles missing static resources (NoResourceFoundException).
     * Ignores favicon.ico and static resources entirely, logs others as minor events.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        var remoteIp = RequestUtils.getEffectiveRemoteIp(request);
        var uri = request.getRequestURI();

        // Skip logging for favicon and static resources
        if (!uri.startsWith("/favicon") && !uri.startsWith("/static/")) {
            log.debug("Static resource not found for request from IP: {}, URI: {}", remoteIp, uri);

            try {
                eventLoggingService.minorEvent(
                        remoteIp,
                        uri,
                        HoneyEventType.HTTP,
                        false,
                        "Static resource not found",
                        buildEventData(request, ex.getClass().getName(), ex.getResourcePath())
                );
            } catch (Exception loggingEx) {
                log.warn("Failed to log event to database: {}", loggingEx.getMessage());
            }
        }

        var errorResponse = new HashMap<String, Object>();
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", "The requested resource was not found");
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("path", uri);

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }
}
