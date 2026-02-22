package com.lemuridaelabs.honeymcp.config;

import com.lemuridaelabs.honeymcp.interceptors.DashboardAccessInterceptor;
import com.lemuridaelabs.honeymcp.interceptors.LoggingInterceptor;
import com.lemuridaelabs.honeymcp.interceptors.MaliciousRequestIdentificationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for registering interceptors.
 *
 * <p>Configures the interceptor chain for handling HTTP requests:</p>
 * <ul>
 *   <li>{@link DashboardAccessInterceptor} - Token-based access control for dashboard endpoints</li>
 *   <li>{@link LoggingInterceptor} - Logs all HTTP requests and responses</li>
 *   <li>{@link MaliciousRequestIdentificationInterceptor} - Detects and flags malicious request patterns</li>
 * </ul>
 *
 * @since 1.0
 */
@RequiredArgsConstructor
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;

    private final MaliciousRequestIdentificationInterceptor maliciousRequestIdentificationInterceptor;

    private final DashboardAccessInterceptor dashboardAccessInterceptor;

    /**
     * Adds interceptors to the application's interceptor registry.
     * <p>
     * The following interceptors are registered:
     * - {@code DashboardAccessInterceptor}: Handles access control for dashboard-related endpoints.
     * - {@code LoggingInterceptor}: Logs all incoming HTTP requests and their corresponding responses.
     * - {@code MaliciousRequestIdentificationInterceptor}: Identifies and handles potentially malicious requests.
     *
     * @param registry the {@code InterceptorRegistry} to register the interceptors with
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(dashboardAccessInterceptor)
                .addPathPatterns("/dashboard", "/dashboard/**");

        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/favicon.ico", "/static/**", "/dashboard", "/dashboard/**");
        registry.addInterceptor(maliciousRequestIdentificationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/favicon.ico", "/static/**");
    }

}
