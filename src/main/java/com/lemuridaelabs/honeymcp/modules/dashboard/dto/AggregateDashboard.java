package com.lemuridaelabs.honeymcp.modules.dashboard.dto;

import com.lemuridaelabs.honeymcp.modules.events.dto.HoneyEvent;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@ToString
@EqualsAndHashCode
public class AggregateDashboard {

    private List<HoneyEvent> recentEvents;

    private List<String> notableIps;

}
