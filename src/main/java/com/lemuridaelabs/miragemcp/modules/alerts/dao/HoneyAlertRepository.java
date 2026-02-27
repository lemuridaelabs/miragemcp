package com.lemuridaelabs.miragemcp.modules.alerts.dao;

import com.lemuridaelabs.miragemcp.modules.alerts.dto.HoneyAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface HoneyAlertRepository extends CrudRepository<HoneyAlert, String>, PagingAndSortingRepository<HoneyAlert, String> {

    Page<HoneyAlert> findAllByRemoteIp(String remoteIp, Pageable pageable);

    long countAllByRemoteIpAndTimestampAfter(String remoteIp, Date startTimestamp);

    @Query("""
            SELECT COUNT(e) FROM "honey_alerts" e WHERE e.remote_ip = :remoteIp
            """)
    List<HoneyAlert> countAllByRemoteIp(String remoteIp);

    /**
     * Counts alerts for a specific IP address since a given date.
     */
    long countByRemoteIpAndTimestampGreaterThanEqual(String remoteIp, Date startDate);

}
