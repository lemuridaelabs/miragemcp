package com.lemuridaelabs.honeymcp.modules.events.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApplicationLifecycleListener implements ApplicationListener<SpringApplicationEvent> {

    /**
     * Handles the application event when it is published, logging the event details.
     *
     * @param event the Spring application event received
     */
    @Override
    public void onApplicationEvent(SpringApplicationEvent event) {
      log.info("Received event: {}", event);
    }

}
