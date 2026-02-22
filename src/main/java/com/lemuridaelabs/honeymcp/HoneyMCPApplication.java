package com.lemuridaelabs.honeymcp;

import com.lemuridaelabs.honeymcp.modules.alerts.config.AlertsConfig;
import com.lemuridaelabs.honeymcp.modules.events.config.EventsConfig;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.security.Security;

/**
 * Main Spring Boot application entry point for the HoneyMCP honeypot server.
 *
 * <p>HoneyMCP is a security research tool that implements a honeypot MCP (Model Context Protocol)
 * server with threat detection, event logging, alert management, and push notification capabilities.
 * It integrates with Spring AI for generating synthetic archive files as deceptive content.</p>
 *
 * @author Lemuridae Labs
 * @since 1.0
 */
@SpringBootApplication
@EnableConfigurationProperties({EventsConfig.class, AlertsConfig.class})
public class HoneyMCPApplication {

    public static void main(String[] args) {
        // Register BouncyCastle security provider for web-push cryptographic operations
        Security.addProvider(new BouncyCastleProvider());

        SpringApplication.run(HoneyMCPApplication.class, args);
    }

}
