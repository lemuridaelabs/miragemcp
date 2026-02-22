package com.lemuridaelabs.honeymcp.modules.archives.dto;

import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class ArchiveFileSummary {

    private String archiveName;

    private Integer count;

    private List<ArchiveFileRecord> files;

    private Date timestamp;

}
