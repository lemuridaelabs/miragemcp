package com.lemuridaelabs.honeymcp.modules.events.api;

import com.lemuridaelabs.honeymcp.modules.events.dto.HoneyEvent;
import com.lemuridaelabs.honeymcp.modules.events.dto.SourceSummary;
import com.lemuridaelabs.honeymcp.modules.events.service.EventSearchService;
import com.lemuridaelabs.honeymcp.modules.events.service.SourceSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API controller for querying honeypot events.
 *
 * <p>Provides endpoints for retrieving and filtering {@link HoneyEvent} entities.
 * This controller is protected by the {@code DashboardAccessInterceptor} and requires
 * a valid access token. Results are paginated with a maximum page size of 100.</p>
 *
 * @see HoneyEvent
 * @see EventSearchService
 * @since 1.0
 */
@RequiredArgsConstructor
@RestController
@Slf4j
public class HoneyEventApiService {

    private final EventSearchService eventSearchService;
    private final SourceSummaryService sourceSummaryService;

    /**
     * Retrieves a list of HoneyEvent entities with optional filters for IP address, event type, and minimum score.
     * Results are paginated and sorted by timestamp in descending order.
     *
     * @param ipAddress optional IP address filter (partial match)
     * @param eventType optional event type filter (HTTP, MCP, API, DASHBOARD, LIFECYCLE, ALERT)
     * @param minimumScore minimum threat score threshold (default: 0)
     * @param page page number (default: 0)
     * @param count events per page (default: 25, max: 100)
     * @return a list of HoneyEvent objects matching the filter criteria
     */
    @GetMapping("/dashboard/events")
    public List<HoneyEvent> getHoneyEvents(@RequestParam(name = "ipAddress", required = false) String ipAddress,
                                           @RequestParam(name = "eventType", required = false) String eventType,
                                           @RequestParam(name = "minimumScore", required = false, defaultValue = "0") int minimumScore,
                                           @RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                           @RequestParam(name = "count", required = false, defaultValue = "25") int count) {
        // Enforce maximum page size
        var limitedCount = Math.min(count, 100);
        return eventSearchService.searchEvents(page, limitedCount, ipAddress, eventType, minimumScore).getContent();
    }

    /**
     * Retrieves a list of source (IP address) summaries showing aggregated activity.
     * Returns the top sources by total threat score within the specified time window.
     *
     * @param hoursBack number of hours to look back (default: 24, max: 168/1 week)
     * @param limit maximum number of sources to return (default: 50, max: 100)
     * @return a list of SourceSummary objects with aggregated metrics per IP
     */
    @GetMapping("/dashboard/sources")
    public List<SourceSummary> getSources(
            @RequestParam(name = "hoursBack", required = false, defaultValue = "24") int hoursBack,
            @RequestParam(name = "limit", required = false, defaultValue = "50") int limit) {
        // Enforce limits
        var limitedHours = Math.min(hoursBack, 168); // Max 1 week
        var limitedCount = Math.min(limit, 100);
        return sourceSummaryService.getTopSources(limitedHours, limitedCount);
    }

}
