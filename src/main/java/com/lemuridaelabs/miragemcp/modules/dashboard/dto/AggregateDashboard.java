package com.lemuridaelabs.miragemcp.modules.dashboard.dto;

import com.lemuridaelabs.miragemcp.modules.events.dto.HoneyEvent;
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
