package com.lemuridaelabs.honeymcp;

import com.lemuridaelabs.honeymcp.modules.alerts.dao.HoneyAlertRepository;
import com.lemuridaelabs.honeymcp.modules.alerts.dto.HoneyAlert;
import com.lemuridaelabs.honeymcp.modules.events.dao.HoneyEventRepository;
import com.lemuridaelabs.honeymcp.modules.events.dto.HoneyEvent;
import com.lemuridaelabs.honeymcp.modules.events.dto.HoneyEventType;
import com.lemuridaelabs.honeymcp.modules.events.service.SourceSummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SourceSummaryServiceTest {

    @Autowired
    private SourceSummaryService sourceSummaryService;

    @Autowired
    private HoneyEventRepository honeyEventRepository;

    @Autowired
    private HoneyAlertRepository honeyAlertRepository;

    @BeforeEach
    void resetDatabase() {
        // Ensure each test starts with clean tables for deterministic aggregates.
        honeyAlertRepository.deleteAll();
        honeyEventRepository.deleteAll();
    }

    @Test
    void aggregatesCountsAndScoresForIp() {
        // Events and alerts for the same IP should be aggregated correctly.
        var ip = "9.9.9.9";
        var firstSeen = Date.from(Instant.now().minus(2, ChronoUnit.HOURS));
        var lastSeen = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));

        honeyEventRepository.save(HoneyEvent.builder()
                .remoteIp(ip)
                .eventType(HoneyEventType.HTTP)
                .score(10)
                .uri("/one")
                .message("one")
                .timestamp(firstSeen)
                .build());

        honeyEventRepository.save(HoneyEvent.builder()
                .remoteIp(ip)
                .eventType(HoneyEventType.HTTP)
                .score(20)
                .uri("/two")
                .message("two")
                .timestamp(lastSeen)
                .build());

        honeyAlertRepository.save(HoneyAlert.builder()
                .remoteIp(ip)
                .message("alert")
                .timestamp(lastSeen)
                .build());

        var summary = sourceSummaryService.getSourceSummary(ip, 24);
        assertThat(summary).isNotNull();
        assertThat(summary.getEventCount()).isEqualTo(2);
        assertThat(summary.getAlertCount()).isEqualTo(1);
        assertThat(summary.getTotalScore()).isEqualTo(30);
        assertThat(summary.getFirstSeen().toInstant()).isEqualTo(firstSeen.toInstant());
        assertThat(summary.getLastSeen().toInstant()).isEqualTo(lastSeen.toInstant());
    }

    @Test
    void ordersTopSourcesByScore() {
        // The top sources list should be ordered by total score descending.
        var ipLow = "8.8.8.8";
        var ipHigh = "7.7.7.7";
        var timestamp = Date.from(Instant.now().minus(30, ChronoUnit.MINUTES));

        honeyEventRepository.save(HoneyEvent.builder()
                .remoteIp(ipLow)
                .eventType(HoneyEventType.HTTP)
                .score(5)
                .uri("/low")
                .message("low")
                .timestamp(timestamp)
                .build());

        honeyEventRepository.save(HoneyEvent.builder()
                .remoteIp(ipHigh)
                .eventType(HoneyEventType.HTTP)
                .score(50)
                .uri("/high")
                .message("high")
                .timestamp(timestamp)
                .build());

        var sources = sourceSummaryService.getTopSources(24, 2);
        assertThat(sources).hasSize(2);
        assertThat(sources.get(0).getRemoteIp()).isEqualTo(ipHigh);
        assertThat(sources.get(1).getRemoteIp()).isEqualTo(ipLow);
    }
}
