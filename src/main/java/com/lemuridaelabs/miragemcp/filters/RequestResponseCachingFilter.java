package com.lemuridaelabs.miragemcp.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Filter that wraps HTTP requests and responses with caching wrappers.
 * This allows the request and response bodies to be read multiple times,
 * which is necessary for logging interceptors.
 * <p>
 * Excludes MCP and SSE endpoints as they require streaming.
 */
@Component
public class RequestResponseCachingFilter implements Filter {

    private static final int MAX_BODY_BYTES = 5000;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        var httpRequest = (HttpServletRequest) request;
        var uri = httpRequest.getRequestURI();

        // Skip wrapping for SSE endpoints (it needs streaming)
        //
        if (shouldSkipCaching(uri)) {
            chain.doFilter(request, response);
            return;
        }

        // Wrap the request and response with caching wrappers
        //
        var wrappedRequest = new ContentCachingRequestWrapper(httpRequest, MAX_BODY_BYTES);
        var wrappedResponse = new ContentCachingResponseWrapper((HttpServletResponse) response);

        try {
            // Continue the filter chain with wrapped request/response
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            // Copy the cached response body to the actual response
            // This is crucial - without this, the response body won't be sent to the client
            wrappedResponse.copyBodyToResponse();
        }
    }


    /**
     * Determines if caching should be skipped for the given URI.
     *
     * @param uri the request URI
     * @return true if caching should be skipped
     */
    private boolean shouldSkipCaching(String uri) {
        return uri.startsWith("/sse")
                || uri.startsWith("/chat/stream")
                || uri.startsWith("/auth/login")
                || uri.equals("/")
                || uri.endsWith("png");
    }
}
