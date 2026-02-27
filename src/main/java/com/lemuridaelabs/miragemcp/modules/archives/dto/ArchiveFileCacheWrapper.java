package com.lemuridaelabs.miragemcp.modules.archives.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;

@Data
@Builder
@ToString
@EqualsAndHashCode
public class ArchiveFileCacheWrapper {

    private String remoteIp;

    private ArchiveFileRecord file;

    private Date timestamp;

}
