package com.lemuridaelabs.honeymcp.modules.events.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Date;

@Data
@Builder
@ToString
@EqualsAndHashCode
@Table("honey_events")
public class HoneyEvent {

    @Id
    private String id;

    private String remoteIp;

    private String uri;

    private HoneyEventType eventType;

    private Boolean isMcp;

    private String message;

    private String data;

    private Integer score;

    private Date timestamp;

}
