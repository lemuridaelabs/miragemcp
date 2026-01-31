package com.lemuridaelabs.honeymcp.interceptors;

import com.lemuridaelabs.honeymcp.modules.events.dto.HoneyEventType;
import com.lemuridaelabs.honeymcp.modules.events.service.EventLoggingService;
import com.lemuridaelabs.honeymcp.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {

    private final EventLoggingService eventLoggingService;

    /**
     * Intercepts an HTTP request before it reaches the controller.
     * Initial logging happens here, but full request/response body logging occurs in afterCompletion.
     *
     * @param request the HttpServletRequest object containing the details of the incoming request
     * @param response the HttpServletResponse object for the response
     * @param handler the handler (or controller) that will process the request
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
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @param handler the handler that processed the request
     * @param ex any exception thrown during request processing
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {

        String uri = request.getRequestURI();

        // If we are letting this one go, just ignore it.
        //
        if (isIgnored(uri)) {
            return;
        }

        String requestBody = null;
        String responseBody = null;

        // Skip logging response bodies for dashboard endpoints to prevent recursive logging
        boolean shouldCaptureResponseBody = !uri.startsWith("/dashboard");

        // Capture request body if available
        if (request instanceof ContentCachingRequestWrapper) {
            var wrappedRequest = (ContentCachingRequestWrapper) request;
            var content = wrappedRequest.getContentAsByteArray();
            if (content.length > 0) {
                requestBody = truncateIfNeeded(new String(content, StandardCharsets.UTF_8), 5000);
            }
        }

        // Capture response body if available (but not for dashboard endpoints)
        if (shouldCaptureResponseBody && response instanceof ContentCachingResponseWrapper) {
            var wrappedResponse = (ContentCachingResponseWrapper) response;
            byte[] content = wrappedResponse.getContentAsByteArray();
            if (content.length > 0) {
                responseBody = truncateIfNeeded(new String(content, StandardCharsets.UTF_8), 5000);
            }
        }

        // Build data JSON string with request and response bodies
        //
        var data = buildDataJson(request.getMethod(), requestBody, response.getStatus(), responseBody);

        var isMcp = isMcp(uri);

        // Log the complete event with a score based on the request target.
        //
        eventLoggingService.logEvent(
                RequestUtils.getEffectiveRemoteIp(request),
                uri,
                HoneyEventType.HTTP,
                isMcp,
                isMcp ? 25 : 0,
                String.format("%s %s - Status: %d", request.getMethod(), uri, response.getStatus()),
                data
        );

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
     * @param method the HTTP method
     * @param requestBody the request body content
     * @param status the HTTP response status code
     * @param responseBody the response body content
     * @return JSON string with the data
     */
    private String buildDataJson(String method, String requestBody, int status, String responseBody) {

        var dataMap = new HashMap<String, Object>();

        dataMap.put("method", method);
        dataMap.put("requestBody", requestBody != null ? requestBody : "N/A");
        dataMap.put("responseStatus", status);
        dataMap.put("responseBody", responseBody != null ? responseBody : "N/A");

        return String.format("{\"method\":\"%s\",\"requestBody\":%s,\"responseStatus\":%d,\"responseBody\":%s}",
                method,
                requestBody != null ? "\"" + escapeJson(requestBody) + "\"" : "null",
                status,
                responseBody != null ? "\"" + escapeJson(responseBody) + "\"" : "null");
    }


    /**
     * Escapes special characters in JSON strings.
     *
     * @param value the string to escape
     * @return escaped string
     */
    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Truncates a string if it exceeds the specified maximum length.
     * Adds "... [truncated]" suffix if truncation occurs.
     *
     * @param value the string to truncate
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
