package com.lemuridaelabs.honeymcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the honeypot server.
 *
 * <p>Configures the security filter chain to permit all requests without authentication.
 * This is intentional for a honeypot as we want to allow attackers to interact with
 * the system while logging their activities. CSRF protection is disabled to allow
 * form submissions from potential attackers.</p>
 *
 * @since 1.0
 */
@Configuration
public class SecurityConfig {

    /**
     * Configures the security filter chain for the application.
     * <p>
     * This method sets up the security configuration to permit all requests
     * without requiring authentication or authorization and disables CSRF protection.
     *
     * @param http the {@link HttpSecurity} object to configure the security behavior of the application
     * @return the configured {@link SecurityFilterChain} instance
     * @throws Exception if an error occurs during the configuration of the security filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

}
