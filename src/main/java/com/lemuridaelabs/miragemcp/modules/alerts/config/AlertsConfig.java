package com.lemuridaelabs.miragemcp.modules.alerts.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "miragemcp.alert")
public record AlertsConfig(Integer delayMinutes,
                           Integer checkPeriodMinutes, AlertThreshold thresholds) {
    public record AlertThreshold(int flagged, int malicious) {
    }
}