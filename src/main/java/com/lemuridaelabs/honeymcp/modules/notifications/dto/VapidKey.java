package com.lemuridaelabs.honeymcp.modules.notifications.dto;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("vapid_keys")
public class VapidKey implements Persistable<Long> {

    @Id
    private Long id;

    private String publicKey;

    private String privateKey;

    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    // Required by Spring Data JDBC
    public VapidKey() {
    }

    public VapidKey(Long id, String publicKey, String privateKey, Instant createdAt) {
        this.id = id;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.createdAt = createdAt;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void markNotNew() {
        this.isNew = false;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
