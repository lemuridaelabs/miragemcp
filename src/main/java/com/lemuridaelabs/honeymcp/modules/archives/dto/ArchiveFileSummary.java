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
public class ArchiveFileSummary {

    private String archiveName;

    private Integer count;

    private List<ArchiveFileRecord> files;

    private Date timestamp;

}
