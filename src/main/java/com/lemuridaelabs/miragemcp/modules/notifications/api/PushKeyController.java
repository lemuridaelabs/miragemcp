package com.lemuridaelabs.miragemcp.modules.notifications.api;

import com.lemuridaelabs.miragemcp.modules.notifications.dao.VapidKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST API controller for retrieving VAPID public keys for push notification subscriptions.
 *
 * <p>This controller provides the public VAPID key that clients need to subscribe
 * to push notifications. The endpoint is protected by the {@code DashboardAccessInterceptor}
 * and requires a valid access token.</p>
 *
 * @see VapidKey
 * @see VapidKeyRepository
 * @since 1.0
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/dashboard/api/push")
@Slf4j
public class PushKeyController {

    private final VapidKeyRepository repository;

    /**
     * Retrieves the public key associated with the VapidKey entity.
     * The public key is fetched from the database using the repository
     * and corresponds to the entity with the ID of 1. If the entity is
     * not found, an exception is thrown.
     *
     * @return the public key for the VapidKey entity with ID 1
     */
    @GetMapping("/public-key")
    public String getPublicKey() {
        log.info("Fetching VAPID public key");
        try {
            // Check how many keys exist
            var count = repository.count();
            log.info("VAPID key count in database: {}", count);

            // List all keys if there are any
            if (count > 0) {
                repository.findAll().forEach(key ->
                        log.info("Found VAPID key with ID: {}, publicKey length: {}",
                                key.getId(),
                                key.getPublicKey() != null ? key.getPublicKey().length() : 0)
                );
            }

            var vapidKey = repository.findById(1L)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "VAPID keys not initialized. Please restart the application."));

            log.info("VAPID public key retrieved successfully, length: {}", vapidKey.getPublicKey().length());
            return vapidKey.getPublicKey();
        } catch (Exception e) {
            log.error("Error retrieving VAPID public key", e);
            throw e;
        }
    }

}
