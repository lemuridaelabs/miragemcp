package com.lemuridaelabs.honeymcp.modules.events.listener;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.logs.Severity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Listener for Spring application lifecycle events.
 *
 * <p>Logs application startup, shutdown, and other lifecycle events for monitoring
 * and audit trail purposes.</p>
 *
 * @since 1.0
 */
@Component
@Slf4j
public class ApplicationLifecycleListener implements ApplicationListener<SpringApplicationEvent> {

    @Value("${honeymcp.lifecycle.logging:false}")
    private Boolean lifecycleLogging;

    private final OpenTelemetry openTelemetry;

    public ApplicationLifecycleListener(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    /**
     * Handles the application event when it is published, logging the event details.
     *
     * @param event the Spring application event received
     */
    @Override
    public void onApplicationEvent(SpringApplicationEvent event) {

        if (lifecycleLogging) {
            LoggerProvider loggerProvider = openTelemetry.getLogsBridge();
            var logger = loggerProvider.get("honeymcp-events");

            logger.logRecordBuilder()
                    .setBody("Lifecycle Event Received: " + event.toString())
                    .setSeverity(Severity.INFO)
                    .emit();
        }

    }

}
