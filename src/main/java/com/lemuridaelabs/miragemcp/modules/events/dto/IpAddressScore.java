package com.lemuridaelabs.miragemcp.modules.events.dto;


import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.Id;

@Data
@Builder
@ToString
@EqualsAndHashCode
public class IpAddressScore {

    @Id
    private String remoteIp;

    private int totalScore;

}
