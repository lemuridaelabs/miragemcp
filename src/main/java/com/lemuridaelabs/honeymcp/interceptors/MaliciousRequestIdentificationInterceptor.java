package com.lemuridaelabs.honeymcp.interceptors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Security interceptor for detecting and handling malicious HTTP requests.
 *
 * <p>Analyzes incoming requests against a configurable set of URI patterns loaded from
 * a CSV file. Patterns are matched using regular expressions and can trigger different
 * actions based on confidence level:</p>
 * <ul>
 *   <li><b>block</b> - Returns 403 Forbidden and stops request processing</li>
 *   <li><b>flag</b> - Logs the threat but allows the request to continue</li>
 *   <li><b>log</b> - Records the detection for monitoring purposes</li>
 * </ul>
 *
 * <p>Pattern categories align with OWASP security classifications for common attack vectors.</p>
 *
 * @since 1.0
 */
@Component
@Slf4j
public class MaliciousRequestIdentificationInterceptor implements HandlerInterceptor {

    private final List<UriPattern> patterns;

    /**
     * Constructs a new instance of MaliciousRequestIdentificationInterceptor.
     * This constructor initializes the interceptor by loading malicious URI patterns from a specified CSV resource file.
     *
     * @param resource the Resource object pointing to the malicious URL patterns file (e.g., malicious_urls.csv).
     *                 The file is expected to contain malicious URI patterns in a specified format.
     * @throws IOException if there is an issue reading the resource file or parsing its contents.
     */
    public MaliciousRequestIdentificationInterceptor(@Value("classpath:malicious_urls.csv") Resource resource)
            throws IOException {
        this.patterns = loadPatterns(resource);
        log.info("Loaded {} malicious URI patterns", patterns.size());
    }


    /**
     * Loads a list of URI patterns from the specified resource file.
     * The resource file is expected to be a CSV where each line after the header
     * represents a URI pattern with its associated metadata.
     *
     * @param resource the resource containing the CSV data to load patterns from
     * @return a list of {@code UriPattern} objects parsed from the resource
     * @throws IOException if an I/O error occurs while reading the resource
     */
    private List<UriPattern> loadPatterns(Resource resource) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {

            return reader.lines()
                    .skip(1) // Skip header
                    .filter(line -> !line.trim().isEmpty())
                    .map(line -> {
                        var parts = line.split(",", -1);
                        var patternString = parts[1];
                        Pattern compiledPattern = null;
                        try {
                            compiledPattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
                        } catch (PatternSyntaxException e) {
                            log.error("Invalid regex pattern during loading, will skip: {}", patternString, e);
                        }
                        return new UriPattern(
                                parts[0], // category
                                patternString, // original pattern string
                                compiledPattern, // pre-compiled pattern
                                parts[2], // description
                                parts[3], // confidence_level
                                parts[4], // action
                                parts.length > 5 ? parts[5] : "" // owasp_category
                        );
                    })
                    .filter(p -> p.compiledPattern() != null) // Filter out patterns that failed to compile
                    .toList();
        }
    }

    /**
     * Intercepts HTTP requests before they reach the controller to identify and handle potentially
     * malicious requests based on predefined URI patterns.
     *
     * This method checks the full URI of the incoming request against a set of patterns to
     * determine if the request exhibits malicious behavior. If a match is found, it delegates to
     * {@code handleThreat()} to determine and execute the appropriate response.
     *
     * @param request the {@link HttpServletRequest} representing the incoming HTTP request
     * @param response the {@link HttpServletResponse} representing the HTTP response
     * @param handler the chosen handler to process the request, or {@code null} if none
     * @return {@code true} if the request is safe and processing should continue,
     *         {@code false} if the request is blocked due to a potential security issue
     * @throws Exception if an error occurs while processing the request
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        var uri = request.getRequestURI();
        var queryString = request.getQueryString();
        var fullUri = queryString != null ? uri + "?" + queryString : uri;

        var matches = patterns.stream()
                .filter(p -> matchesPattern(p, fullUri))
                .toList();

        if (!matches.isEmpty()) {
            return handleThreat(matches, request, response, fullUri);
        }

        return true; // Continue processing
    }

    /**
     * Checks if the given full URI matches the pre-compiled pattern in the provided UriPattern.
     * Uses the pre-compiled regex for efficient matching without recompilation overhead.
     *
     * @param uriPattern the {@code UriPattern} containing the pre-compiled pattern to match against
     * @param fullUri the full URI string to be checked
     * @return {@code true} if the full URI matches the pattern; {@code false} otherwise
     */
    private boolean matchesPattern(UriPattern uriPattern, String fullUri) {
        var compiledPattern = uriPattern.compiledPattern();
        if (compiledPattern == null) {
            return false;
        }
        return compiledPattern.matcher(fullUri).find();
    }


    /**
     * Handles threats by analyzing URI patterns and determining if the incoming request should be blocked,
     * flagged, or logged. Logs the threat details and blocks the request if necessary.
     *
     * @param matches a list of URI patterns that matched the incoming request
     * @param request the HTTP request object containing the details of the incoming request
     * @param response the HTTP response object to modify and send the appropriate response if the request is blocked
     * @param fullUri the full URI of the incoming request
     * @return true if the request should be allowed to continue, false if it should be blocked
     * @throws IOException if an issue occurs while writing to the response
     */
    private boolean handleThreat(List<UriPattern> matches,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 String fullUri) throws IOException {

        // Check if any match requires blocking
        var shouldBlock = matches.stream()
                .anyMatch(p -> "block".equals(p.action()));

        var isCritical = matches.stream()
                .anyMatch(p -> "critical".equals(p.confidenceLevel()));

        log.info("URI status, uri={}, shouldBlock={}, isCritical={}.", fullUri, shouldBlock, isCritical);

        // Log all matches
        matches.forEach(match -> {
            String logLevel = shouldBlock ? "BLOCKED" : "FLAGGED";
            log.warn("[{}] Malicious request detected - Category: {}, Description: {}, " +
                            "Confidence: {}, OWASP: {}, URI: {}, IP: {}, User-Agent: {}",
                    logLevel,
                    match.category(),
                    match.description(),
                    match.confidenceLevel(),
                    match.owaspCategory(),
                    fullUri,
                    getClientIP(request),
                    request.getHeader("User-Agent"));
        });

        if (shouldBlock || isCritical) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                    "error": "Forbidden",
                    "message": "Request blocked due to security policy",
                    "status": 403
                }
                """);
            return false; // Stop request processing
        }

        // For "flag" or "log" actions, allow through but log for monitoring
        return true;
    }

    /**
     * Retrieves the IP address of the client from the given HTTP request.
     * It first checks the "X-Forwarded-For" header for the client's IP address,
     * which may be set by proxies or load balancers. If the header is not present
     * or is empty, it falls back to the IP address provided by {@link HttpServletRequest#getRemoteAddr()}.
     *
     * @param request the HttpServletRequest object containing the details of the client's request
     * @return the IP address of the client as a String
     */
    private String getClientIP(HttpServletRequest request) {
        var xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Record representing a URI pattern with pre-compiled regex for efficient matching.
     *
     * @param category the category of the malicious pattern (e.g., "SQL_INJECTION")
     * @param patternString the original regex pattern string
     * @param compiledPattern the pre-compiled regex pattern for efficient matching (null if pattern is invalid)
     * @param description human-readable description of what the pattern detects
     * @param confidenceLevel confidence level of the detection (e.g., "high", "medium", "critical")
     * @param action the action to take when matched (e.g., "block", "flag", "log")
     * @param owaspCategory the OWASP category this pattern relates to
     */
    record UriPattern(String category, String patternString, Pattern compiledPattern, String description,
                      String confidenceLevel, String action, String owaspCategory) {}
}