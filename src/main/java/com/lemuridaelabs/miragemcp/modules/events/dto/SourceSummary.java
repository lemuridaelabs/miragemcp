package com.lemuridaelabs.miragemcp.modules.events.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * Summary of activity from a single IP address source.
 * Used for the dashboard "Sources" view to show aggregated metrics per IP.
 */
@Data
@Builder
public class SourceSummary {

    private String remoteIp;
    private int totalScore;
    private int eventCount;
    private int alertCount;
    private Date firstSeen;
    private Date lastSeen;

}
