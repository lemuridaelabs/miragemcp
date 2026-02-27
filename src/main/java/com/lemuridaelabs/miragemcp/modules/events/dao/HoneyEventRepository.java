package com.lemuridaelabs.miragemcp.modules.events.dao;

import com.lemuridaelabs.miragemcp.modules.events.dto.HoneyEvent;
import com.lemuridaelabs.miragemcp.modules.events.dto.HoneyEventType;
import com.lemuridaelabs.miragemcp.modules.events.dto.IpAddressScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface HoneyEventRepository extends CrudRepository<HoneyEvent, String>, PagingAndSortingRepository<HoneyEvent, String> {

    Page<HoneyEvent> findAllByRemoteIp(String remoteIp, Pageable pageable);

    Page<HoneyEvent> findAllByRemoteIpContaining(String remoteIp, Pageable pageable);

    Page<HoneyEvent> findAllByUri(String uri, Pageable pageable);

    Page<HoneyEvent> findAllByScoreGreaterThan(int minimumScore, Pageable pageable);

    Page<HoneyEvent> findAllByEventType(HoneyEventType eventType, Pageable pageable);

    Page<HoneyEvent> findAllByRemoteIpContainingAndEventType(String remoteIp, HoneyEventType eventType, Pageable pageable);

    Page<HoneyEvent> findAllByRemoteIpContainingAndScoreGreaterThan(String remoteIp, int minimumScore, Pageable pageable);

    Page<HoneyEvent> findAllByEventTypeAndScoreGreaterThan(HoneyEventType eventType, int minimumScore, Pageable pageable);

    Page<HoneyEvent> findAllByRemoteIpContainingAndEventTypeAndScoreGreaterThan(String remoteIp, HoneyEventType eventType, int minimumScore, Pageable pageable);

    @Query("""
            SELECT COUNT(e) FROM "honey_events" e WHERE e.remote_ip = :remoteIp
            """)
    List<HoneyEvent> countAllByRemoteIp(String remoteIp);


    @Query("""
            SELECT remote_ip, sum(score) as total_score
            FROM "honey_events" e WHERE
                 (e.remote_ip = :remoteIp) and
                 (e.timestamp >= :startDate)
            group by remote_ip
            """)
    List<IpAddressScore> getIpAddressScore(String remoteIp, Date startDate);

    /**
     * Gets distinct IP addresses with their aggregated scores since a given date.
     * Returns IPs ordered by total score descending.
     */
    @Query("""
            SELECT remote_ip, sum(score) as total_score
            FROM "honey_events" e
            WHERE e.timestamp >= :startDate
            GROUP BY remote_ip
            ORDER BY total_score DESC
            LIMIT :limit
            """)
    List<IpAddressScore> getTopIpAddressesByScore(Date startDate, int limit);

    /**
     * Counts events for a specific IP address since a given date.
     */
    long countByRemoteIpAndTimestampGreaterThanEqual(String remoteIp, Date startDate);

    /**
     * Gets the first event timestamp for a specific IP since a given date.
     */
    @Query("""
            SELECT MIN(e.timestamp)
            FROM "honey_events" e
            WHERE e.remote_ip = :remoteIp AND e.timestamp >= :startDate
            """)
    Date getFirstSeenSince(String remoteIp, Date startDate);

    /**
     * Gets the last event timestamp for a specific IP since a given date.
     */
    @Query("""
            SELECT MAX(e.timestamp)
            FROM "honey_events" e
            WHERE e.remote_ip = :remoteIp AND e.timestamp >= :startDate
            """)
    Date getLastSeenSince(String remoteIp, Date startDate);

}
