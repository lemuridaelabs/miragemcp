package com.lemuridaelabs.honeymcp.modules.alerts.service;

import com.lemuridaelabs.honeymcp.modules.alerts.dao.HoneyAlertRepository;
import com.lemuridaelabs.honeymcp.modules.alerts.dto.HoneyAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class AlertSearchService {

    private final HoneyAlertRepository honeyAlertRepository;

    /**
     * Retrieves a page of all HoneyAlert entities.
     * The results are paginated and sorted in descending order by the timestamp.
     *
     * @param page the page number to retrieve (0-based index)
     * @param count the number of alerts to retrieve per page
     * @return a Page object containing the HoneyAlert entities
     */
    public Page<HoneyAlert> getAlerts(int page, int count) {
        return honeyAlertRepository.findAll(
                PageRequest.of(page, count, Sort.by("timestamp").descending()));
    }

    /**
     * Searches and retrieves a page of HoneyAlert records filtered by the given IP address.
     *
     * @param page the page number to retrieve, with 0-based indexing
     * @param count the number of records per page
     * @param ipAddress the IP address to filter alerts by
     * @return a Page containing HoneyAlert objects that match the given IP address
     */
    public Page<HoneyAlert> searchAlertsByIpAddress(int page, int count, String ipAddress) {
        return honeyAlertRepository.findAllByRemoteIp(ipAddress,
                PageRequest.of(page, count, Sort.by("timestamp").descending()));
    }

}
