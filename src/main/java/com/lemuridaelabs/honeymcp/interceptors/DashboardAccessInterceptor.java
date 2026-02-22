package com.lemuridaelabs.honeymcp.interceptors;

import com.lemuridaelabs.honeymcp.modules.dashboard.service.DashboardTokenService;
import com.lemuridaelabs.honeymcp.modules.events.dto.HoneyEventType;
import com.lemuridaelabs.honeymcp.modules.events.service.EventLoggingService;
import com.lemuridaelabs.honeymcp.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Security interceptor for protecting dashboard endpoints with token-based access control.
 *
 * <p>Validates the {@code token} query parameter against the configured access token.
 * Invalid or missing tokens result in a 404 response to obscure the existence of the
 * dashboard from attackers. Access attempts without valid tokens are logged as
 * medium-severity events.</p>
 *
 * @see DashboardTokenService
 * @see EventLoggingService
 * @since 1.0
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class DashboardAccessInterceptor implements HandlerInterceptor {

    private final EventLoggingService eventLoggingService;

    private final DashboardTokenService dashboardTokenService;


    /**
     * Intercepts an incoming HTTP request and determines if access to the /dashboard path
     * or its sub-paths should be granted based on the presence and validity of an access token.
     * If the token is missing or invalid, a 404 response is returned to obscure the existence of the resource.
     *
     * @param request  the HttpServletRequest object that contains client request information
     * @param response the HttpServletResponse object to which the interceptor can send a response
     * @param handler  the chosen handler to execute, for type and/or instance evaluation
     * @return true if the request should be forwarded to the handler, false otherwise
     * @throws Exception if an error occurs during request handling
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        var uri = request.getRequestURI();

        // Only apply to /dashboard and its sub-paths
        if (!uri.startsWith("/dashboard")) {
            return true;
        }

        // Get token from query parameter
        var providedToken = request.getParameter("token");

        log.info("Processing Dashboard Request with providedToken={}, configured={}.", providedToken,
                dashboardTokenService.getAccessToken());

        // Check if token is missing or invalid
        //
        if (providedToken == null || !dashboardTokenService.getAccessToken().equals(providedToken)) {

            log.warn("Dashboard access attempt without valid token - URI={}, IP={}, User-Agent={}",
                    uri, getClientIP(request), request.getHeader("User-Agent"));

            eventLoggingService.mediumEvent(
                    RequestUtils.getEffectiveRemoteIp(request),
                    request.getRequestURI(), HoneyEventType.DASHBOARD,
                    false,
                    String.format("Invalid Access Attempt for Dashboard for uri=%s", uri), null);

            // Return 404 to hide dashboard existence
            return sendNotFoundResponse(response);
        }

        // Valid token - allow access
        log.debug("Dashboard access granted with valid token - URI: {}, IP: {}",
                uri, getClientIP(request));

        return true;
    }

    /**
     * Sends a 404 Not Found response to the client and stops further request processing.
     *
     * @param response the HttpServletResponse object used to send the error response
     * @return false to indicate that request processing should be terminated
     * @throws IOException if an I/O error occurs while sending the error response
     */
    private boolean sendNotFoundResponse(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return false;
    }

    /**
     * Retrieves the client's IP address from the given HTTP request. It first checks for the
     * "X-Forwarded-For" header, which contains the originating IP address for requests passing
     * through proxies or load balancers. If the header is present, it extracts the first IP
     * address from the list. Otherwise, it falls back to using the remote address directly
     * from the request object.
     *
     * @param request the HttpServletRequest object from which the client's IP address is retrieved
     * @return the client's IP address as a String; if the "X-Forwarded-For" header is absent,
     * returns the remote address from the request
     */
    private String getClientIP(HttpServletRequest request) {
        var xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

}
