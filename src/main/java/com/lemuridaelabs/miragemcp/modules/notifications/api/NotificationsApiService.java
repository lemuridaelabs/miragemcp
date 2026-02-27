package com.lemuridaelabs.miragemcp.modules.notifications.api;

import com.lemuridaelabs.miragemcp.modules.notifications.dao.PushSubscriptionRepository;
import com.lemuridaelabs.miragemcp.modules.notifications.dto.PushSubscription;
import com.lemuridaelabs.miragemcp.modules.notifications.dto.PushSubscriptionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/dashboard/api/push")
@Slf4j
public class NotificationsApiService {

    private final PushSubscriptionRepository repository;

    /**
     * Handles the process of unsubscribing a client by removing their push notification subscription.
     *
     * @param body a map containing the subscription details, specifically the "endpoint" key,
     *             which represents the unique endpoint identifier for the subscription to be removed.
     */
    @DeleteMapping("/unsubscribe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@RequestBody Map<String, String> body) {
        repository.deleteByEndpoint(body.get("endpoint"));
    }

    /**
     * Subscribes a user to push notifications. The subscription details are persisted
     * if the endpoint does not already exist in the repository, ensuring idempotency.
     *
     * @param dto the push subscription details, including the endpoint and encryption keys
     */
    @PostMapping("/subscribe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(@RequestBody PushSubscription dto) {

        // idempotency: avoid duplicates
        if (repository.existsByEndpoint(dto.getEndpoint())) {
            return;
        }

        repository.save(new PushSubscriptionEntity(
                null,
                dto.getEndpoint(),
                dto.getKeys().getP256dh(),
                dto.getKeys().getAuth(),
                Instant.now()
        ));
    }

}
