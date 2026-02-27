package com.lemuridaelabs.miragemcp.modules.notifications.dao;

import com.lemuridaelabs.miragemcp.modules.notifications.dto.VapidKey;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VapidKeyRepository extends CrudRepository<VapidKey, Long> {
}
