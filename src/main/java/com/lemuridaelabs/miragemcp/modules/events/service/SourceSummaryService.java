package com.lemuridaelabs.miragemcp.modules.events.service;

import com.lemuridaelabs.miragemcp.modules.alerts.dao.HoneyAlertRepository;
import com.lemuridaelabs.miragemcp.modules.events.dao.HoneyEventRepository;
import com.lemuridaelabs.miragemcp.modules.events.dto.SourceSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for aggregating source (IP address) activity summaries.
 * Provides metrics like total score, event count, and alert count per IP.
 */
@RequiredArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class SourceSummaryService {

    private final HoneyEventRepository honeyEventRepository;
    private final HoneyAlertRepository honeyAlertRepository;

    /**
     * Gets a list of source summaries for the top IP addresses by score
     * within the specified time window.
     *
     * @param hoursBack number of hours to look back (e.g., 24 for last 24 hours)
     * @param limit     maximum number of sources to return
     * @return list of SourceSummary objects ordered by total score descending
     */
    public List<SourceSummary> getTopSources(int hoursBack, int limit) {

        // Enforce limits
        var limitedHours = Math.min(hoursBack, 168); // Max 1 week
        var limitedCount = Math.min(limit, 100);

        var startDate = Date.from(Instant.now().minus(limitedHours, ChronoUnit.HOURS));

        // Get top IPs by score
        var topIps = honeyEventRepository.getTopIpAddressesByScore(startDate, limitedCount);

        // Build full summaries for each IP
        return topIps.stream()
                .map(ipScore -> buildSourceSummary(ipScore.getRemoteIp(), ipScore.getTotalScore(), startDate))
                .collect(Collectors.toList());
    }

    /**
     * Gets a source summary for a specific IP address.
     *
     * @param remoteIp  the IP address to get summary for
     * @param hoursBack number of hours to look back
     * @return SourceSummary for the IP, or null if no events found
     */
    public SourceSummary getSourceSummary(String remoteIp, int hoursBack) {

        var limitedHours = Math.min(hoursBack, 168); // Max 1 week
        var startDate = Date.from(Instant.now().minus(limitedHours, ChronoUnit.HOURS));

        var scores = honeyEventRepository.getIpAddressScore(remoteIp, startDate);
        if (scores.isEmpty()) {
            return null;
        }

        var totalScore = scores.get(0).getTotalScore();
        return buildSourceSummary(remoteIp, totalScore, startDate);
    }

    private SourceSummary buildSourceSummary(String remoteIp, int totalScore, Date startDate) {
        var eventCount = honeyEventRepository.countByRemoteIpAndTimestampGreaterThanEqual(remoteIp, startDate);
        var alertCount = honeyAlertRepository.countByRemoteIpAndTimestampGreaterThanEqual(remoteIp, startDate);
        var firstSeen = honeyEventRepository.getFirstSeenSince(remoteIp, startDate);
        var lastSeen = honeyEventRepository.getLastSeenSince(remoteIp, startDate);

        return SourceSummary.builder()
                .remoteIp(remoteIp)
                .totalScore(totalScore)
                .eventCount((int) eventCount)
                .alertCount((int) alertCount)
                .firstSeen(firstSeen)
                .lastSeen(lastSeen)
                .build();
    }

}
