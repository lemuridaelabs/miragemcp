package com.lemuridaelabs.miragemcp.modules.notifications.dto;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Table("push_subscription")
public class PushSubscriptionEntity {

    @Id
    private Long id;

    private String endpoint;
    private String p256dh;
    private String auth;
    private Instant createdAt;

    public PushSubscriptionEntity() {
    }

    public PushSubscriptionEntity(
            Long id,
            String endpoint,
            String p256dh,
            String auth,
            Instant createdAt) {

        this.id = id;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.createdAt = createdAt;
    }

    // getters
}
