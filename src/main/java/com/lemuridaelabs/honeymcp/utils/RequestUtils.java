package com.lemuridaelabs.honeymcp.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for HTTP request handling operations.
 *
 * <p>Provides helper methods for extracting client information from HTTP requests,
 * including proper handling of proxy headers like {@code X-Forwarded-For} and
 * {@code CF-Connecting-IP} (Cloudflare).</p>
 *
 * @since 1.0
 */
public final class RequestUtils {

    /**
     * Retrieves the remote client's IP address from the given HttpServletRequest.
     * The method first checks for the CF-Connecting-IP header, then the X-Forwarded-For header
     * (returning the first IP in the list if present), and finally falls back to the remote address.
     *
     * @param request the HttpServletRequest object containing information about the client request
     * @return the remote IP address of the client as a String
     */
    public static String getEffectiveRemoteIp(HttpServletRequest request) {
        var clientIp = request.getHeader("CF-Connecting-IP");
        return clientIp != null ? clientIp
                : request.getHeader("X-Forwarded-For") != null
                ? request.getHeader("X-Forwarded-For").split(",")[0].trim()
                : request.getRemoteAddr();
    }

}
