package com.lemuridaelabs.miragemcp.interceptors;

import com.lemuridaelabs.miragemcp.modules.events.dto.HoneyEventType;
import com.lemuridaelabs.miragemcp.modules.events.service.EventLoggingService;
import com.lemuridaelabs.miragemcp.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * HTTP request/response logging interceptor for the honeypot.
 *
 * <p>Captures and logs all HTTP requests and responses, including request/response bodies
 * (up to 5000 characters). Each request is recorded as a {@link HoneyEvent} for later
 * analysis. MCP-related requests receive a higher threat score.</p>
 *
 * <p>Static resources and favicon requests are excluded from logging.</p>
 *
 * @see EventLoggingService
 * @since 1.0
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {

    private static final int MAX_BODY_CHARS = 5000;

    private final EventLoggingService eventLoggingService;

    private final JsonMapper jsonMapper;

    /**
     * Intercepts an HTTP request before it reaches the controller.
     * Initial logging happens here, but full request/response body logging occurs in afterCompletion.
     *
     * @param request  the HttpServletRequest object containing the details of the incoming request
     * @param response the HttpServletResponse object for the response
     * @param handler  the handler (or controller) that will process the request
     * @return true to indicate that the request should proceed to the next step in the processing chain
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.debug("Request received, method={}, uri={}", request.getMethod(), request.getRequestURI());
        return true;
    }


    /**
     * Called after the complete request has finished (after response is sent).
     * Logs the full request and response details including bodies.
     *
     * @param request  the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @param handler  the handler that processed the request
     * @param ex       any exception thrown during request processing
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {

        // Skip logging for async request timeouts (normal for SSE connections)
        if (ex instanceof AsyncRequestTimeoutException) {
            return;
        }

        var uri = request.getRequestURI();

        // If we are letting this one go, just ignore it.
        //
        if (isIgnored(uri)) {
            return;
        }

        String requestBody = null;
        String responseBody = null;

        // Skip logging response bodies for dashboard endpoints to prevent recursive logging
        var shouldCaptureResponseBody = !uri.startsWith("/dashboard");

        // Capture request body if available
        if (request instanceof ContentCachingRequestWrapper wrappedRequest
                && shouldCaptureBody(request.getContentType())) {
            var content = wrappedRequest.getContentAsByteArray();
            if (content.length > 0) {
                requestBody = truncateIfNeeded(new String(content, StandardCharsets.UTF_8), MAX_BODY_CHARS);
            }
        }

        // Capture response body if available (but not for dashboard endpoints)
        if (shouldCaptureResponseBody
                && response instanceof ContentCachingResponseWrapper wrappedResponse
                && shouldCaptureBody(response.getContentType())) {
            var content = wrappedResponse.getContentAsByteArray();
            if (content.length > 0) {
                responseBody = truncateIfNeeded(new String(content, StandardCharsets.UTF_8), MAX_BODY_CHARS);
            }
        }

        // Build data JSON string with request and response bodies
        //
        var userAgent = request.getHeader("User-Agent");
        var acceptLanguage = request.getHeader("Accept-Language");
        var data = buildDataJson(request.getMethod(), userAgent, acceptLanguage, requestBody, response.getStatus(), responseBody);

        var isMcp = isMcp(uri);
        var message = String.format("%s %s - Status: %d", request.getMethod(), uri, response.getStatus());
        var remoteIp = RequestUtils.getEffectiveRemoteIp(request);

        // Use minor severity for missing resources (404s), otherwise score based on request target
        if (ex instanceof NoResourceFoundException) {
            eventLoggingService.minorEvent(remoteIp, uri, HoneyEventType.HTTP, isMcp, message, data);
        } else {
            eventLoggingService.logEvent(remoteIp, uri, HoneyEventType.HTTP, isMcp,
                    isMcp ? 25 : 0, message, data);
        }

        // Log to console for immediate visibility (truncated for readability)
        log.info("Request: {} {} | Status: {} | Request Body: {} | Response Body: {}",
                request.getMethod(),
                uri,
                response.getStatus(),
                requestBody != null ? truncateForLog(requestBody) : "N/A",
                responseBody != null ? truncateForLog(responseBody) : "N/A");
    }

    /**
     * Builds a JSON string containing request and response body information.
     *
     * @param method         the HTTP method
     * @param userAgent      the User-Agent header value
     * @param acceptLanguage the Accept-Language header value
     * @param requestBody    the request body content
     * @param status         the HTTP response status code
     * @param responseBody   the response body content
     * @return JSON string with the data, or empty JSON object if serialization fails
     */
    private String buildDataJson(String method, String userAgent, String acceptLanguage,
                                 String requestBody, int status, String responseBody) {

        // Use LinkedHashMap to preserve insertion order for consistent output
        //
        var dataMap = new LinkedHashMap<String, Object>();
        dataMap.put("method", method);
        dataMap.put("userAgent", userAgent);
        dataMap.put("acceptLanguage", acceptLanguage);
        dataMap.put("requestBody", requestBody);
        dataMap.put("responseStatus", status);
        dataMap.put("responseBody", responseBody);

        try {
            return jsonMapper.writeValueAsString(dataMap);
        } catch (JacksonException e) {
            log.warn("Failed to serialize request/response data to JSON", e);
            return "{}";
        }
    }


    /**
     * Truncates a string if it exceeds the specified maximum length.
     * Adds "... [truncated]" suffix if truncation occurs.
     *
     * @param value     the string to truncate
     * @param maxLength maximum allowed length
     * @return truncated string if needed, or original string
     */
    private String truncateIfNeeded(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "... [truncated]";
    }


    /**
     * Truncates a string for console logging to prevent log spam.
     *
     * @param value the string to truncate
     * @return truncated string (max 200 chars)
     */
    private String truncateForLog(String value) {
        return truncateIfNeeded(value, 200);
    }

    private boolean shouldCaptureBody(String contentType) {
        if (contentType == null) {
            return true;
        }
        var normalized = contentType.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("multipart/")) {
            return false;
        }
        return normalized.startsWith("text/")
                || normalized.contains("json")
                || normalized.contains("xml")
                || normalized.contains("x-www-form-urlencoded")
                || normalized.contains("yaml")
                || normalized.contains("csv")
                || normalized.contains("javascript");
    }

    /**
     * Determines if the given URI starts with specific prefixes related to the MCP context.
     *
     * @param uri the URI string to be checked
     * @return true if the URI starts with "/mcp" or "/sse", false otherwise
     */
    private boolean isMcp(String uri) {
        return (uri.startsWith("/mcp") || uri.startsWith("/sse"));
    }

    /**
     * Determines if the provided URI should be ignored based on predefined prefixes.
     * This method is typically used to filter out requests related to static resources,
     * such as favicon or static assets, that do not require specific processing.
     *
     * @param uri the URI string that needs to be checked
     * @return true if the URI starts with "/favicon.ico" or "/static/", false otherwise
     */
    private boolean isIgnored(String uri) {
        return uri.startsWith("/favicon.ico") || uri.startsWith("/static/");
    }

}
