package com.lemuridaelabs.honeymcp.modules.alerts.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "honeymcp.alert")
public record AlertsConfig(Integer delayMinutes,
                           Integer checkPeriodMinutes, AlertThreshold thresholds) {
    public record AlertThreshold(int flagged, int malicious) {
    }
}