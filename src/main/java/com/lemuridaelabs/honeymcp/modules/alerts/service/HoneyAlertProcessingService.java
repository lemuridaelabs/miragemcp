package com.lemuridaelabs.honeymcp.modules.alerts.service;

import com.lemuridaelabs.honeymcp.modules.alerts.config.AlertsConfig;
import com.lemuridaelabs.honeymcp.modules.alerts.dao.HoneyAlertRepository;
import com.lemuridaelabs.honeymcp.modules.alerts.dto.HoneyAlert;
import com.lemuridaelabs.honeymcp.modules.events.dao.HoneyEventRepository;
import com.lemuridaelabs.honeymcp.modules.events.dto.HoneyEvent;
import com.lemuridaelabs.honeymcp.modules.events.dto.HoneyEventType;
import com.lemuridaelabs.honeymcp.modules.notifications.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Service responsible for evaluating events and generating security alerts.
 *
 * <p>This service implements a time-window based threshold system with per-IP cooldowns
 * to detect malicious activity. When event scores exceed configured thresholds within
 * the check period, alerts are generated and push notifications are sent to subscribed
 * clients.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Aggregates event scores per IP within a configurable time window</li>
 *   <li>Supports flagged and malicious severity levels based on thresholds</li>
 *   <li>Enforces cooldown periods to prevent alert flooding</li>
 *   <li>Triggers async push notifications on alert generation</li>
 * </ul>
 *
 * @see HoneyAlert
 * @see HoneyEvent
 * @see AlertsConfig
 * @since 1.0
 */
@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
public class HoneyAlertProcessingService {

    private final AlertsConfig alertsConfig;

    private final HoneyEventRepository honeyEventRepository;

    private final HoneyAlertRepository honeyAlertRepository;

    private final PushNotificationService pushNotificationService;

    /**
     * Evaluates a newly logged event to determine if an alert should be triggered.
     * <p>
     * This method implements a time-window based threshold system with per-IP cooldowns:
     * 1. Calculates the time window for score aggregation based on checkPeriodMinutes
     * 2. Aggregates all event scores for the IP within the time window
     * 3. Checks if the IP is currently in cooldown (recently alerted)
     * 4. If not in cooldown and threshold exceeded, generates an alert
     * 5. Prevents alert flooding by enforcing delayMinutes cooldown period
     *
     * @param event The newly logged HoneyEvent to evaluate
     */
    public void evaluateEvent(HoneyEvent event) {
        log.info("Evaluating event: {}", event);

        var remoteIp = event.getRemoteIp();

        // Calculate the time window for score aggregation
        var checkPeriodStart = Date.from(
                Instant.now().minus(alertsConfig.checkPeriodMinutes(), ChronoUnit.MINUTES)
        );

        // Get aggregated score for this IP within the check period
        var ipScores = honeyEventRepository.getIpAddressScore(remoteIp, checkPeriodStart);

        if (ipScores.isEmpty()) {
            log.debug("No score data found for IP: {}", remoteIp);
            return;
        }

        var totalScore = ipScores.get(0).getTotalScore();
        log.info("IP {} has total score {} over last {} minutes",
                remoteIp, totalScore, alertsConfig.checkPeriodMinutes());

        // Check if we're in cooldown period for this IP
        var cooldownStart = Date.from(
                Instant.now().minus(alertsConfig.delayMinutes(), ChronoUnit.MINUTES)
        );

        var recentAlerts = honeyAlertRepository.countAllByRemoteIpAndTimestampAfter(
                remoteIp, cooldownStart
        );

        if (recentAlerts > 0) {
            log.info("IP {} is in cooldown period ({} recent alerts in last {} minutes), skipping alert generation",
                    remoteIp, recentAlerts, alertsConfig.delayMinutes());
            return;
        }

        // Determine alert severity based on threshold
        String alertMessage = null;
        if (totalScore >= alertsConfig.thresholds().malicious()) {
            alertMessage = String.format(
                    "MALICIOUS activity detected from IP %s: Score %d exceeds malicious threshold of %d",
                    remoteIp, totalScore, alertsConfig.thresholds().malicious()
            );
        } else if (totalScore >= alertsConfig.thresholds().flagged()) {
            alertMessage = String.format(
                    "FLAGGED activity detected from IP %s: Score %d exceeds flagged threshold of %d",
                    remoteIp, totalScore, alertsConfig.thresholds().flagged()
            );
        }

        // Generate alert if threshold exceeded
        if (alertMessage != null) {
            var alert = HoneyAlert.builder()
                    .remoteIp(remoteIp)
                    .message(alertMessage)
                    .timestamp(new Date())
                    .build();

            var savedAlert = honeyAlertRepository.save(alert);
            log.warn("ALERT GENERATED: {}", alertMessage);

            // Log the alert as a special event for tracking
            logAlertEvent(savedAlert);

            // Send push notification to all subscribed clients
            sendPushNotification(savedAlert, totalScore);
        } else {
            log.debug("Score {} below flagged threshold {} for IP {}",
                    totalScore, alertsConfig.thresholds().flagged(), remoteIp);
        }
    }

    /**
     * Logs the alert generation as a special event for audit trail.
     * This creates a HoneyEvent record with type ALERT to track when alerts are generated.
     *
     * @param alert The HoneyAlert that was generated
     */
    private void logAlertEvent(HoneyAlert alert) {
        var alertEvent = HoneyEvent.builder()
                .remoteIp(alert.getRemoteIp())
                .uri("/internal/alert")
                .eventType(HoneyEventType.ALERT)
                .isMcp(false)
                .score(0) // Alert events don't contribute to score
                .message("Alert generated: " + alert.getMessage())
                .data(alert.getId())
                .timestamp(new Date())
                .build();

        honeyEventRepository.save(alertEvent);
    }

    /**
     * Sends push notifications to all subscribed clients when an alert is generated.
     *
     * @param alert      The generated alert
     * @param totalScore The total threat score that triggered the alert
     */
    private void sendPushNotification(HoneyAlert alert, int totalScore) {
        try {
            log.info("Sending push notification for alert={}", alert);

            // Determine severity for notification title
            var severity = alert.getMessage().contains("MALICIOUS") ? "MALICIOUS" : "FLAGGED";
            var title = String.format("HoneyMCP Security Alert - %s", severity);

            // Build notification message
            var message = String.format("IP %s detected with threat score %d",
                    alert.getRemoteIp(), totalScore);

            // Build additional data payload
            var data = String.format(
                    "{\"alertId\":\"%s\",\"remoteIp\":\"%s\",\"score\":%d,\"severity\":\"%s\",\"timestamp\":\"%s\"}",
                    alert.getId(),
                    alert.getRemoteIp(),
                    totalScore,
                    severity,
                    alert.getTimestamp().toInstant().toString()
            );

            // Send notification asynchronously
            pushNotificationService.sendNotificationToAll(title, message, data);

            log.info("Push notification triggered for alert: {}", alert.getId());
        } catch (Exception e) {
            // Log error but don't fail alert generation
            log.error("Failed to send push notification for alert: {}", alert.getId(), e);
        }
    }

}
