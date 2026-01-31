package com.lemuridaelabs.honeymcp.modules.events.api;

import com.lemuridaelabs.honeymcp.modules.events.dto.HoneyEvent;
import com.lemuridaelabs.honeymcp.modules.events.service.EventSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@Slf4j
public class HoneyEventApiService {

    private final EventSearchService eventSearchService;

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
        int limitedCount = Math.min(count, 100);
        return eventSearchService.searchEvents(page, limitedCount, ipAddress, eventType, minimumScore).getContent();
    }

}
