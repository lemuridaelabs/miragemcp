package com.lemuridaelabs.honeymcp.modules.dashboard.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Service for managing dashboard access tokens.
 *
 * <p>Generates and provides access tokens for the security dashboard. If no token
 * is configured in application properties, a random UUID is generated on startup.
 * The token is logged at startup to allow authorized access to the dashboard.</p>
 *
 * @see com.lemuridaelabs.honeymcp.interceptors.DashboardAccessInterceptor
 * @since 1.0
 */
@Service
@Slf4j
public class DashboardTokenService {

    private final String DEFAULT_TOKEN = "*RANDOMTOKEN*";

    @Value("${honeymcp.server.base-url:http://localhost:8989}")
    private String baseUrl;

    @Value("${honeymcp.dashboard.access-token:*RANDOMTOKEN*}")
    private String accessToken;

    @PostConstruct
    private void setupToken() {

        var isGenerated = false;
        if (accessToken == null || accessToken.equals(DEFAULT_TOKEN)) {
            log.warn("No dashboard access token configured; generating random token.");
            accessToken = UUID.randomUUID().toString();
            isGenerated = true;
        }

        // Log only masked token and hash for verification, never the full token
        var maskedToken = maskToken(accessToken);
        var tokenHash = hashToken(accessToken);
        log.info("Dashboard access token configured: masked={}, sha256={}, generated={}",
                maskedToken, tokenHash, isGenerated);

        // Output full token to console only on startup (not to log files)
        // This mimics Spring Security's approach for generated passwords
        //
        System.out.println();
        System.out.println("=========================================");
        System.out.println("HONEYMCP DASHBOARD ACCESS TOKEN");
        System.out.println("-----------------------------------------");
        System.out.println("Token: " + accessToken);
        System.out.println("URL: " + baseUrl + "/dashboard?token=" + accessToken);
        System.out.println("=========================================");
        System.out.println();
    }


    /**
     * Masks a token for safe logging, showing only first 4 and last 4 characters.
     *
     * @param token the token to mask
     * @return masked token string
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }


    /**
     * Generates a SHA-256 hash of the token for verification purposes.
     *
     * @param token the token to hash
     * @return first 16 characters of hex-encoded SHA-256 hash
     */
    private String hashToken(String token) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "unavailable";
        }
    }


    /**
     * Retrieves the current access token used for secure access to the dashboard.
     * The access token may be configured via application properties or generated
     * automatically at runtime if no token is provided.
     *
     * @return the access token as a string
     */
    public String getAccessToken() {
        return accessToken;
    }

}
