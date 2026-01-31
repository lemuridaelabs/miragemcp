package com.lemuridaelabs.honeymcp.web.error;

import com.lemuridaelabs.honeymcp.modules.events.dto.HoneyEventType;
import com.lemuridaelabs.honeymcp.modules.events.service.EventLoggingService;
import com.lemuridaelabs.honeymcp.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
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

    /**
     * Handles all unhandled exceptions and prevents stack traces from being exposed.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex, HttpServletRequest request) {
        String remoteIp = RequestUtils.getEffectiveRemoteIp(request);
        String uri = request.getRequestURI();

        log.error("Unhandled exception for request from IP: {}, URI: {}", remoteIp, uri, ex);

        // Log as high-severity event
        eventLoggingService.highEvent(
            remoteIp,
            uri,
            HoneyEventType.HTTP,
            false,
            "Internal server error: " + ex.getClass().getSimpleName(),
            String.format("Method: %s, Exception: %s, Message: %s",
                request.getMethod(), ex.getClass().getName(), ex.getMessage())
        );

        Map<String, Object> errorResponse = new HashMap<>();
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
        String remoteIp = RequestUtils.getEffectiveRemoteIp(request);
        String uri = request.getRequestURI();

        log.error("NullPointerException for request from IP: {}, URI: {}", remoteIp, uri, ex);

        eventLoggingService.highEvent(
            remoteIp,
            uri,
            HoneyEventType.HTTP,
            false,
            "NullPointerException occurred",
            String.format("Method: %s, Message: %s", request.getMethod(), ex.getMessage())
        );

        Map<String, Object> errorResponse = new HashMap<>();
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
        String remoteIp = RequestUtils.getEffectiveRemoteIp(request);
        String uri = request.getRequestURI();

        log.warn("IllegalArgumentException for request from IP: {}, URI: {}, Message: {}", remoteIp, uri, ex.getMessage());

        eventLoggingService.mediumEvent(
            remoteIp,
            uri,
            HoneyEventType.HTTP,
            false,
            "Invalid request parameters",
            String.format("Method: %s, Message: %s", request.getMethod(), ex.getMessage())
        );

        Map<String, Object> errorResponse = new HashMap<>();
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
        String remoteIp = RequestUtils.getEffectiveRemoteIp(request);
        String uri = request.getRequestURI();

        log.debug("404 Not Found for request from IP: {}, URI: {}", remoteIp, uri);

        eventLoggingService.lowEvent(
            remoteIp,
            uri,
            HoneyEventType.HTTP,
            false,
            "Resource not found",
            String.format("Method: %s", request.getMethod())
        );

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", "The requested resource was not found");
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("path", uri);

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorResponse);
    }
}
