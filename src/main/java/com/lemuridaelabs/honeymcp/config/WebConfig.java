package com.lemuridaelabs.honeymcp.config;

import com.lemuridaelabs.honeymcp.interceptors.DashboardAccessInterceptor;
import com.lemuridaelabs.honeymcp.interceptors.LoggingInterceptor;
import com.lemuridaelabs.honeymcp.interceptors.MaliciousRequestIdentificationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;

    private final MaliciousRequestIdentificationInterceptor maliciousRequestIdentificationInterceptor;

    private final DashboardAccessInterceptor dashboardAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(dashboardAccessInterceptor)
                .addPathPatterns("/dashboard", "/dashboard/**");

        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**");
        registry.addInterceptor(maliciousRequestIdentificationInterceptor)
                .addPathPatterns("/**");
    }

}
