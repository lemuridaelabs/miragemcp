package com.lemuridaelabs.miragemcp.modules.events.service;

import com.lemuridaelabs.miragemcp.modules.events.dao.HoneyEventRepository;
import com.lemuridaelabs.miragemcp.modules.events.dto.HoneyEvent;
import com.lemuridaelabs.miragemcp.modules.events.dto.HoneyEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for searching and filtering honeypot events.
 *
 * <p>Provides paginated search capabilities for {@link HoneyEvent} entities with support
 * for filtering by IP address, event type, URI, and minimum threat score. All results
 * are sorted by timestamp in descending order.</p>
 *
 * @see HoneyEvent
 * @see HoneyEventRepository
 * @since 1.0
 */
@RequiredArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class EventSearchService {

    private final HoneyEventRepository honeyEventRepository;

    /**
     * Retrieves a page of HoneyEvent entities with a score greater than the specified minimum score.
     * The results are paginated and sorted in descending order by the timestamp.
     *
     * @param page         the page number to retrieve (0-based index)
     * @param count        the number of events to retrieve per page
     * @param minimumScore the minimum score threshold for filtering events
     * @return a Page object containing the HoneyEvent entities that meet the minimum score criteria
     */
    public Page<HoneyEvent> getEvents(int page, int count, int minimumScore) {
        return honeyEventRepository.findAllByScoreGreaterThan(minimumScore,
                PageRequest.of(page, count, Sort.by("timestamp").descending()));
    }


    /**
     * Searches and retrieves a page of HoneyEvent records filtered by the given IP address.
     *
     * @param page      the page number to retrieve, with 0-based indexing
     * @param count     the number of records per page
     * @param ipAddress the IP address to filter events by
     * @return a Page containing HoneyEvent objects that match the given IP address
     */
    public Page<HoneyEvent> searchEventsByIpAddress(int page, int count, String ipAddress) {
        return honeyEventRepository.findAllByRemoteIp(ipAddress,
                PageRequest.of(page, count, Sort.by("timestamp").descending()));
    }


    /**
     * Searches and retrieves a page of HoneyEvent entities filtered by the specified URI.
     *
     * @param page  the page index to retrieve, zero-based
     * @param count the number of records per page
     * @param uri   the URI by which the HoneyEvents should be filtered
     * @return a Page object containing the HoneyEvents that match the specified URI
     */
    public Page<HoneyEvent> searchEventsByUri(int page, int count, String uri) {
        return honeyEventRepository.findAllByUri(uri,
                PageRequest.of(page, count, Sort.by("timestamp").descending()));
    }


    /**
     * Searches and retrieves a page of HoneyEvent entities that have a score greater than the specified minimum score.
     * The results are paginated and sorted in descending order by the timestamp.
     *
     * @param page         the page number to retrieve (0-based index)
     * @param count        the number of events to retrieve per page
     * @param minimumScore the minimum score threshold for filtering events
     * @return a Page object containing HoneyEvent entities that meet the minimum score criteria
     */
    public Page<HoneyEvent> searchEventsByMinimumScore(int page, int count, int minimumScore) {
        return honeyEventRepository.findAllByScoreGreaterThan(minimumScore,
                PageRequest.of(page, count, Sort.by("timestamp").descending()));
    }

    /**
     * Searches events with multiple optional filters: IP address, event type, and minimum score.
     * All filter parameters are optional - pass null or empty string to skip that filter.
     *
     * @param page         the page number to retrieve (0-based index)
     * @param count        the number of events to retrieve per page
     * @param ipAddress    optional IP address to filter by (partial match)
     * @param eventType    optional event type to filter by
     * @param minimumScore minimum score threshold for filtering events
     * @return a Page object containing filtered HoneyEvent entities
     */
    public Page<HoneyEvent> searchEvents(int page, int count, String ipAddress, String eventType, int minimumScore) {

        var limitedCount = Math.min(count, 100);

        var pageRequest = PageRequest.of(page, limitedCount, Sort.by("timestamp").descending());


        var hasIpFilter = ipAddress != null && !ipAddress.trim().isEmpty();
        var hasEventTypeFilter = eventType != null && !eventType.trim().isEmpty();
        var hasScoreFilter = minimumScore > 0;

        HoneyEventType eventTypeEnum = null;
        if (hasEventTypeFilter) {
            try {
                eventTypeEnum = HoneyEventType.valueOf(eventType.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid event type: {}", eventType);
                hasEventTypeFilter = false;
            }
        }

        // All three filters
        if (hasIpFilter && hasEventTypeFilter && hasScoreFilter) {
            return honeyEventRepository.findAllByRemoteIpContainingAndEventTypeAndScoreGreaterThan(
                    ipAddress, eventTypeEnum, minimumScore, pageRequest);
        }
        // IP and Event Type
        else if (hasIpFilter && hasEventTypeFilter) {
            return honeyEventRepository.findAllByRemoteIpContainingAndEventType(
                    ipAddress, eventTypeEnum, pageRequest);
        }
        // IP and Score
        else if (hasIpFilter && hasScoreFilter) {
            return honeyEventRepository.findAllByRemoteIpContainingAndScoreGreaterThan(
                    ipAddress, minimumScore, pageRequest);
        }
        // Event Type and Score
        else if (hasEventTypeFilter && hasScoreFilter) {
            return honeyEventRepository.findAllByEventTypeAndScoreGreaterThan(
                    eventTypeEnum, minimumScore, pageRequest);
        }
        // Only IP
        else if (hasIpFilter) {
            return honeyEventRepository.findAllByRemoteIpContaining(ipAddress, pageRequest);
        }
        // Only Event Type
        else if (hasEventTypeFilter) {
            return honeyEventRepository.findAllByEventType(eventTypeEnum, pageRequest);
        }
        // Only Score
        else if (hasScoreFilter) {
            return honeyEventRepository.findAllByScoreGreaterThan(minimumScore, pageRequest);
        }
        // No filters
        else {
            return honeyEventRepository.findAll(pageRequest);
        }
    }

}
