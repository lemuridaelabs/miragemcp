package com.lemuridaelabs.honeymcp.modules.dashboard.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class DashboardTokenService {

    private final String DEFAULT_TOKEN = "*RANDOMTOKEN*";

    @Value("${honeymcp.dashboard.access-token}")
    private String accessToken;

    @PostConstruct
    private void setupToken() {

        if (accessToken == null || accessToken.equals(DEFAULT_TOKEN)) {
            accessToken = UUID.randomUUID().toString();
        }

        log.info("Dashboard Access token for /dashboard URL paths is set to: {}", accessToken);
    }

    public String getAccessToken() {
        return accessToken;
    }

}
