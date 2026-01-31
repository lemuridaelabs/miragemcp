package com.lemuridaelabs.honeymcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

public class RestClientConfiguration {

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder
                .defaultHeader("Accept", "application/json")
                .build();
    }

}
