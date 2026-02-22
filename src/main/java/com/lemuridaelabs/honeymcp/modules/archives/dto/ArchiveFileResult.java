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
public class ArchiveFileResult {

    private String id;

    private String name;

    private List<String> keywords;

    private Long size;

    private String summary;

    private byte[] fileData;

    private Date created;

    private Date modified;

}
