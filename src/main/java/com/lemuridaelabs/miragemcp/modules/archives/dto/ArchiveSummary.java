package com.lemuridaelabs.miragemcp.modules.archives.dto;


import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class ArchiveSummary {

    private String userContext;

    private List<String> availableArchives;

    private List<String> disallowedArchives;

    private Date timestamp;

}
