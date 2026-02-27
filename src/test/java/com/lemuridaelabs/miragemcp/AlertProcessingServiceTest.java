package com.lemuridaelabs.miragemcp;

import com.lemuridaelabs.miragemcp.modules.alerts.dao.HoneyAlertRepository;
import com.lemuridaelabs.miragemcp.modules.alerts.dto.HoneyAlert;
import com.lemuridaelabs.miragemcp.modules.events.dao.HoneyEventRepository;
import com.lemuridaelabs.miragemcp.modules.events.dto.HoneyEventType;
import com.lemuridaelabs.miragemcp.modules.events.service.EventLoggingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "miragemcp.alert.delayMinutes=60",
        "miragemcp.alert.checkPeriodMinutes=60",
        "miragemcp.alert.thresholds.flagged=1",
        "miragemcp.alert.thresholds.malicious=2",
        "miragemcp.notifications.enabled=false"
})
@ActiveProfiles("test")
class AlertProcessingServiceTest {

    @Autowired
    private EventLoggingService eventLoggingService;

    @Autowired
    private HoneyAlertRepository honeyAlertRepository;

    @Autowired
    private HoneyEventRepository honeyEventRepository;

    @BeforeEach
    void resetDatabase() {
        // Ensure deterministic counts for each test.
        honeyAlertRepository.deleteAll();
        honeyEventRepository.deleteAll();
    }

    @Test
    void generatesAlertAndHonorsCooldown() {
        // A flagged score should generate an alert, and a second event should be suppressed by cooldown.
        var startingAlerts = honeyAlertRepository.count();

        eventLoggingService.logEvent(
                "1.2.3.4",
                "/test",
                HoneyEventType.HTTP,
                false,
                1,
                "test event",
                null
        );

        var afterFirst = honeyAlertRepository.count();
        assertThat(afterFirst).isEqualTo(startingAlerts + 1);
        var firstAlert = getAlertForIp("1.2.3.4");
        assertThat(firstAlert).isNotNull();
        assertThat(firstAlert.getMessage()).contains("FLAGGED");

        eventLoggingService.logEvent(
                "1.2.3.4",
                "/test-2",
                HoneyEventType.HTTP,
                false,
                2,
                "test event 2",
                null
        );

        var afterSecond = honeyAlertRepository.count();
        assertThat(afterSecond).isEqualTo(afterFirst);
        assertThat(honeyEventRepository.count()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void generatesMaliciousAlertForHighScore() {
        // A higher score should generate a MALICIOUS alert.
        eventLoggingService.logEvent(
                "5.6.7.8",
                "/test-malicious",
                HoneyEventType.HTTP,
                false,
                2,
                "test event malicious",
                null
        );

        var alert = getAlertForIp("5.6.7.8");
        assertThat(alert).isNotNull();
        assertThat(alert.getMessage()).contains("MALICIOUS");
    }

    private HoneyAlert getAlertForIp(String remoteIp) {
        List<HoneyAlert> alerts = new ArrayList<>();
        honeyAlertRepository.findAll().forEach(alerts::add);
        return alerts.stream()
                .filter(alert -> remoteIp.equals(alert.getRemoteIp()))
                .reduce((first, second) -> second)
                .orElse(null);
    }
}
