package com.lemuridaelabs.miragemcp;

import com.lemuridaelabs.miragemcp.modules.alerts.config.AlertsConfig;
import com.lemuridaelabs.miragemcp.modules.events.config.EventsConfig;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.security.Security;

/**
 * Main Spring Boot application entry point for the Miragemcp honeypot server.
 *
 * <p>Miragemcp is a security research tool that implements a honeypot MCP (Model Context Protocol)
 * server with threat detection, event logging, alert management, and push notification capabilities.
 * It integrates with Spring AI for generating synthetic archive files as deceptive content.</p>
 *
 * @author Lemuridae Labs
 * @since 1.0
 */
@SpringBootApplication
@EnableConfigurationProperties({EventsConfig.class, AlertsConfig.class})
public class MirageMcpApplication {

    public static void main(String[] args) {
        // Register BouncyCastle security provider for web-push cryptographic operations
        Security.addProvider(new BouncyCastleProvider());

        SpringApplication.run(MirageMcpApplication.class, args);
    }

}
