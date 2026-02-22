package com.lemuridaelabs.honeymcp.modules.notifications.service;

import com.lemuridaelabs.honeymcp.modules.notifications.dao.PushSubscriptionRepository;
import com.lemuridaelabs.honeymcp.modules.notifications.dao.VapidKeyRepository;
import com.lemuridaelabs.honeymcp.modules.notifications.dto.VapidKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutionException;

/**
 * Service for sending web push notifications to subscribed clients.
 *
 * <p>This service manages the delivery of push notifications using the Web Push protocol
 * with VAPID (Voluntary Application Server Identification) authentication. Notifications
 * are sent asynchronously to avoid blocking the caller.</p>
 *
 * <p>Features include:</p>
 * <ul>
 *   <li>Async notification delivery to all registered subscriptions</li>
 *   <li>Automatic cleanup of invalid/expired subscriptions</li>
 *   <li>JSON payload construction with title, body, and custom data</li>
 * </ul>
 *
 * @see VapidKey
 * @see PushSubscriptionRepository
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final PushSubscriptionRepository subscriptionRepository;
    private final VapidKeyRepository vapidKeyRepository;

    @Value("${honeymcp.notifications.enabled:true}")
    private boolean notificationsEnabled;

    /**
     * Sends a push notification to all subscribed clients.
     * This method runs asynchronously to avoid blocking the caller.
     *
     * @param title   the notification title
     * @param message the notification message
     * @param data    optional JSON data to include with the notification
     */
    @Async
    public void sendNotificationToAll(String title, String message, String data) {
        try {
            if (!notificationsEnabled) {
                log.debug("Push notifications are disabled; skipping send.");
                return;
            }

            log.info("Sending Global Push Notification, title={}, message={}, data={}.", title, message, data);

            // Retrieve VAPID keys
            var vapidKey = vapidKeyRepository.findById(1L)
                    .orElseThrow(() -> new IllegalStateException("VAPID keys not initialized"));

            // Initialize push service with VAPID keys
            var pushService = createPushService(vapidKey);

            // Build notification payload
            var payload = buildNotificationPayload(title, message, data);

            // Send to all subscriptions
            subscriptionRepository.findAll().forEach(sub -> {
                log.info("Processing Subscription, sub={}.", sub);
                try {
                    sendToSubscription(pushService, sub.getEndpoint(), sub.getP256dh(), sub.getAuth(), payload);
                } catch (Exception e) {
                    log.error("Failed to send push notification to endpoint: {}", sub.getEndpoint(), e);
                    // Consider removing invalid subscriptions
                    if (isSubscriptionGone(e)) {
                        log.info("Removing invalid subscription: {}", sub.getEndpoint());
                        subscriptionRepository.deleteByEndpoint(sub.getEndpoint());
                    }
                }
            });

            log.info("Push notifications sent successfully. Title: {}, Message: {}", title, message);

        } catch (Exception e) {
            log.error("Error sending push notifications", e);
        }
    }

    /**
     * Creates a PushService instance configured with VAPID keys.
     *
     * @param vapidKey the VAPID key entity containing public and private keys
     * @return configured PushService instance
     * @throws GeneralSecurityException if key conversion fails
     */
    private PushService createPushService(VapidKey vapidKey) throws GeneralSecurityException {
        // PushService expects base64url-encoded string keys
        return new PushService(vapidKey.getPublicKey(), vapidKey.getPrivateKey());
    }

    /**
     * Sends a notification to a specific subscription.
     *
     * @param pushService the configured PushService
     * @param endpoint    the subscription endpoint
     * @param p256dh      the subscription's p256dh key
     * @param auth        the subscription's auth secret
     * @param payload     the notification payload JSON
     * @throws GeneralSecurityException if cryptographic operations fail
     * @throws IOException              if network communication fails
     * @throws JoseException            if JOSE operations fail
     * @throws ExecutionException       if async execution fails
     * @throws InterruptedException     if execution is interrupted
     */
    private void sendToSubscription(PushService pushService, String endpoint, String p256dh, String auth, String payload)
            throws GeneralSecurityException, IOException, JoseException, ExecutionException, InterruptedException {

        // Create subscription object
        var subscription = new Subscription(
                endpoint,
                new Subscription.Keys(p256dh, auth)
        );

        // Create notification
        var notification = new Notification(subscription, payload);

        // Send notification
        pushService.send(notification);

        log.debug("Push notification sent to endpoint: {}", endpoint);
    }

    /**
     * Builds a JSON payload for the push notification.
     *
     * @param title   notification title
     * @param message notification message
     * @param data    optional additional data
     * @return JSON string payload
     */
    private String buildNotificationPayload(String title, String message, String data) {
        var json = new StringBuilder();
        json.append("{");
        json.append("\"title\":\"").append(escapeJson(title)).append("\",");
        json.append("\"body\":\"").append(escapeJson(message)).append("\"");

        if (data != null && !data.isEmpty()) {
            json.append(",\"data\":").append(data);
        }

        json.append("}");
        return json.toString();
    }

    /**
     * Escapes special characters for JSON strings.
     *
     * @param value the string to escape
     * @return escaped string
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Determines if an exception indicates a subscription is no longer valid (410 Gone).
     *
     * @param e the exception to check
     * @return true if subscription should be removed
     */
    private boolean isSubscriptionGone(Exception e) {
        var message = e.getMessage();
        return message != null && (message.contains("410") || message.contains("Gone"));
    }
}
