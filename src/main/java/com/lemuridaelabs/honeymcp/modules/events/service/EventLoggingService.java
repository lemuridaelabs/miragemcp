package com.lemuridaelabs.honeymcp.modules.events.service;

import com.lemuridaelabs.honeymcp.modules.alerts.service.HoneyAlertProcessingService;
import com.lemuridaelabs.honeymcp.modules.events.config.EventsConfig;
import com.lemuridaelabs.honeymcp.modules.events.dao.HoneyEventRepository;
import com.lemuridaelabs.honeymcp.modules.events.dto.HoneyEvent;
import com.lemuridaelabs.honeymcp.modules.events.dto.HoneyEventType;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.logs.Severity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Service responsible for logging honeypot events with configurable severity levels.
 *
 * <p>This service provides methods to log events at minor, low, medium, and high priority levels,
 * each with different threat scores. Events are persisted to the database and subsequently
 * evaluated by the alert processing service for potential threat detection.</p>
 *
 * @see HoneyEvent
 * @see HoneyAlertProcessingService
 * @since 1.0
 */
@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
public class EventLoggingService {

    private final EventsConfig eventsConfig;

    private final HoneyEventRepository honeyEventRepository;

    private final HoneyAlertProcessingService honeyAlertProcessingService;

    private final OpenTelemetry openTelemetry;


    /**
     * Logs a minor event with the specified details. Used for routine occurrences
     * like missing resources (404s) that are unlikely to indicate malicious activity.
     *
     * @param remoteIp       The IP address of the remote client.
     * @param uri            The URI associated with the event being logged.
     * @param eventType      The type of the event, such as HTTP, MCP, or ALERT.
     * @param isMcp          A boolean indicating if the event is from an MCP (multi-channel provider).
     * @param message        A message describing the event.
     * @param data           Additional data or payload related to the event.
     */
    public void minorEvent(String remoteIp, String uri, HoneyEventType eventType,
                           Boolean isMcp, String message, String data) {
        logEvent(remoteIp, uri, eventType, isMcp, eventsConfig.weights().minor(), message, data);
    }


    /**
     * Logs a low-level event with the specified details.
     *
     * @param remoteIp       The IP address of the remote client.
     * @param uri            The URI associated with the event being logged.
     * @param eventType      The type of the event, such as HTTP, MCP, or ALERT.
     * @param isMcp          A boolean indicating if the event is from an MCP (multi-channel provider).
     * @param message        A message describing the event.
     * @param data           Additional data or payload related to the event.
     */
    public void lowEvent(String remoteIp, String uri, HoneyEventType eventType,
                         Boolean isMcp, String message, String data) {
        logEvent(remoteIp, uri, eventType, isMcp, eventsConfig.weights().low(), message, data);
    }


    /**
     * Logs a medium-priority event with the provided details.
     *
     * @param remoteIp       The IP address of the remote client.
     * @param uri            The URI associated with the event being logged.
     * @param eventType      The type of the event, such as HTTP, MCP, or ALERT.
     * @param isMcp          A boolean indicating if the event is from an MCP (multi-channel provider).
     * @param message        A message describing the event.
     * @param data           Additional data or payload related to the event.
     */
    public void mediumEvent(String remoteIp, String uri, HoneyEventType eventType,
                            Boolean isMcp, String message, String data) {
        logEvent(remoteIp, uri, eventType, isMcp, eventsConfig.weights().medium(), message, data);
    }


    /**
     * Logs a high-impact event with the provided details.
     *
     * @param remoteIp       The IP address of the remote client.
     * @param uri            The URI associated with the event being logged.
     * @param eventType      The type of the event, such as HTTP, MCP, DASHBOARD, API, or ALERT.
     * @param isMcp          A boolean indicating if the event is related to an MCP (multi-channel provider).
     * @param message        A message providing additional information about the event.
     * @param data           Additional data or context associated with the event.
     */
    public void highEvent(String remoteIp, String uri, HoneyEventType eventType,
                          Boolean isMcp, String message, String data) {
        logEvent(remoteIp, uri, eventType, isMcp, eventsConfig.weights().high(), message, data);
    }


    /**
     * Logs an event with the provided details into the HoneyEvent repository.
     *
     * @param remoteIp       The IP address of the remote client.
     * @param uri            The URI associated with the event being logged.
     * @param eventType      The type of the event, such as HTTP, MCP, or ALERT.
     * @param isMcp          A boolean indicating if the event is from an MCP (multi-channel provider).
     * @param score          A negativity score for the event
     * @param message        A message describing the event.
     * @param data           Additional data or payload related to the event.
     */
    public void logEvent(String remoteIp, String uri, HoneyEventType eventType,
                         Boolean isMcp, Integer score, String message, String data) {

        if (score>0) {
            LoggerProvider loggerProvider = openTelemetry.getLogsBridge();
            log.info("OpenTelemetry Logger Provider: {}", loggerProvider);
            var logger = loggerProvider.get("honeymcp-events");
            log.info("OpenTelemetry Logger: {}", logger);
            logger.logRecordBuilder()
                    .setBody(message)
                    .setSeverity(Severity.INFO)
                    .setAttribute("uri", uri)
                    .setAttribute("remoteIp", remoteIp)
                    .emit();
        }

        var honeyEvent = HoneyEvent.builder()
                .remoteIp(remoteIp)
                .uri(uri).eventType(eventType).isMcp(isMcp)
                .score(score).message(message).data(data)
                .timestamp(new Date())
                .build();

        log.info("Logging Event, honeyEvent={}", honeyEvent);

        var finalEvent = honeyEventRepository.save(honeyEvent);

        honeyAlertProcessingService.evaluateEvent(finalEvent);
    }

}
