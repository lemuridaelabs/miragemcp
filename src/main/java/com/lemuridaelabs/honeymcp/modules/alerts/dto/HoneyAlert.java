package com.lemuridaelabs.honeymcp.modules.alerts.dto;

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
@Table("honey_alerts")
public class HoneyAlert {

    @Id
    private String id;

    private String remoteIp;

    private String message;

    private Date timestamp;

}
