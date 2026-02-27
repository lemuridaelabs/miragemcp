package com.lemuridaelabs.miragemcp.modules.events.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "miragemcp.event")
public record EventsConfig(
        EventWeight weights
) {
    public record EventWeight(int minor, int low, int medium, int high) {
    }
}