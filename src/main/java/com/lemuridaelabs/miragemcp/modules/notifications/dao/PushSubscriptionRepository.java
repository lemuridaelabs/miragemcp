package com.lemuridaelabs.miragemcp.modules.notifications.dao;


import com.lemuridaelabs.miragemcp.modules.notifications.dto.PushSubscriptionEntity;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.repository.CrudRepository;

public interface PushSubscriptionRepository extends CrudRepository<PushSubscriptionEntity, Long> {

    boolean existsByEndpoint(String endpoint);

    @Modifying
    void deleteByEndpoint(String endpoint);

}
