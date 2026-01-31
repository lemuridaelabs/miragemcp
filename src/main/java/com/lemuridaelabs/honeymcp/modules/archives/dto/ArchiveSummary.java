package com.lemuridaelabs.honeymcp.modules.archives.dto;


import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;
import java.util.List;

@Data
@Builder
@ToString
@EqualsAndHashCode
public class ArchiveSummary {

    private String userContext;

    private List<String> availableArchives;

    private List<String> disallowedArchives;

    private Date timestamp;

}
