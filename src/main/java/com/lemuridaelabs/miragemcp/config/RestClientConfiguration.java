package com.lemuridaelabs.miragemcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for the Spring RestClient.
 *
 * <p>Provides a pre-configured {@link RestClient} bean with default headers
 * for JSON content negotiation.</p>
 *
 * @since 1.0
 */
@Configuration
public class RestClientConfiguration {

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder
                .defaultHeader("Accept", "application/json")
                .build();
    }

}
