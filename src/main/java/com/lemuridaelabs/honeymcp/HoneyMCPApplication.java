package com.lemuridaelabs.honeymcp;

import com.lemuridaelabs.honeymcp.modules.alerts.config.AlertsConfig;
import com.lemuridaelabs.honeymcp.modules.events.config.EventsConfig;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.security.Security;

@SpringBootApplication
@EnableConfigurationProperties({EventsConfig.class, AlertsConfig.class})
public class HoneyMCPApplication {

	public static void main(String[] args) {
		// Register BouncyCastle security provider for web-push cryptographic operations
		Security.addProvider(new BouncyCastleProvider());

		SpringApplication.run(HoneyMCPApplication.class, args);
	}

}
