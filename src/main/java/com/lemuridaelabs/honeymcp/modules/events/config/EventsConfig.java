package com.lemuridaelabs.honeymcp.modules.events.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "honeymcp.event")
public record EventsConfig(
        EventWeight weights
) {
    public record EventWeight(int minor, int low, int medium, int high) {}
}