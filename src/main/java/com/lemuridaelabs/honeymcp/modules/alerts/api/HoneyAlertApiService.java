package com.lemuridaelabs.honeymcp.modules.alerts.api;

import com.lemuridaelabs.honeymcp.modules.alerts.dto.HoneyAlert;
import com.lemuridaelabs.honeymcp.modules.alerts.service.AlertSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@Slf4j
public class HoneyAlertApiService {

    private final AlertSearchService alertSearchService;

    /**
     * Retrieves a list of HoneyAlert entities, limited to 100 records, sorted by timestamp in descending order.
     * This endpoint is protected by DashboardAccessInterceptor and requires a valid token parameter.
     *
     * @param page the page number to retrieve (0-based index, defaults to 0)
     * @param count the number of alerts to retrieve per page (defaults to 25, max 100)
     * @return a list of HoneyAlert objects representing the latest alerts
     */
    @GetMapping("/dashboard/alerts")
    public List<HoneyAlert> getHoneyAlerts(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                           @RequestParam(name = "count", required = false, defaultValue = "25") int count) {
        // Enforce maximum page size of 100
        int limitedCount = Math.min(count, 100);
        return alertSearchService.getAlerts(page, limitedCount).getContent();
    }

    /**
     * Retrieves a list of HoneyAlert entities filtered by IP address.
     * This endpoint is protected by DashboardAccessInterceptor and requires a valid token parameter.
     *
     * @param ipAddress the IP address to filter alerts by
     * @param page the page number to retrieve (0-based index, defaults to 0)
     * @param count the number of alerts to retrieve per page (defaults to 25, max 100)
     * @return a list of HoneyAlert objects for the specified IP address
     */
    @GetMapping("/dashboard/alerts/by-ip")
    public List<HoneyAlert> getHoneyAlertsByIp(@RequestParam(name = "ipAddress") String ipAddress,
                                                @RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                                @RequestParam(name = "count", required = false, defaultValue = "25") int count) {
        // Enforce maximum page size of 100
        int limitedCount = Math.min(count, 100);
        return alertSearchService.searchAlertsByIpAddress(page, limitedCount, ipAddress).getContent();
    }

}
