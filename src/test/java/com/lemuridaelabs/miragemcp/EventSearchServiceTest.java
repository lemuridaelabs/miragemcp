package com.lemuridaelabs.miragemcp;

import com.lemuridaelabs.miragemcp.modules.events.dao.HoneyEventRepository;
import com.lemuridaelabs.miragemcp.modules.events.dto.HoneyEvent;
import com.lemuridaelabs.miragemcp.modules.events.dto.HoneyEventType;
import com.lemuridaelabs.miragemcp.modules.events.service.EventSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class EventSearchServiceTest {

    @Autowired
    private EventSearchService eventSearchService;

    @Autowired
    private HoneyEventRepository honeyEventRepository;

    @BeforeEach
    void resetDatabase() {
        // Ensure each test starts with a clean event table.
        honeyEventRepository.deleteAll();
    }

    @Test
    void filtersByIpTypeAndScore() {
        // Insert events with different attributes to verify filter combinations.
        saveEvent("10.0.0.1", HoneyEventType.HTTP, 10, "/alpha");
        saveEvent("10.0.0.1", HoneyEventType.MCP, 5, "/beta");
        saveEvent("10.0.0.2", HoneyEventType.MCP, 50, "/gamma");

        var page = eventSearchService.searchEvents(0, 10, "10.0.0.1", "mcp", 1);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getRemoteIp()).isEqualTo("10.0.0.1");
        assertThat(page.getContent().get(0).getEventType()).isEqualTo(HoneyEventType.MCP);
    }

    @Test
    void filtersByScoreOnly() {
        // Score filter should return only events above the requested threshold.
        saveEvent("10.0.0.3", HoneyEventType.HTTP, 5, "/low");
        saveEvent("10.0.0.3", HoneyEventType.HTTP, 60, "/high");

        var page = eventSearchService.searchEvents(0, 10, null, null, 10);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getUri()).isEqualTo("/high");
    }

    @Test
    void ignoresInvalidEventTypeFilter() {
        // Invalid event types should not filter out valid results.
        saveEvent("10.0.0.4", HoneyEventType.HTTP, 10, "/one");
        saveEvent("10.0.0.5", HoneyEventType.MCP, 20, "/two");

        var page = eventSearchService.searchEvents(0, 10, null, "not-a-type", 0);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    private void saveEvent(String remoteIp, HoneyEventType type, int score, String uri) {
        honeyEventRepository.save(HoneyEvent.builder()
                .remoteIp(remoteIp)
                .eventType(type)
                .isMcp(type == HoneyEventType.MCP)
                .score(score)
                .uri(uri)
                .message("test")
                .timestamp(new Date())
                .build());
    }
}
