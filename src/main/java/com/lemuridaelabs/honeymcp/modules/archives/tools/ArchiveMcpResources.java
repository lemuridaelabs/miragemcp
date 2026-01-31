package com.lemuridaelabs.honeymcp.modules.archives.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lemuridaelabs.honeymcp.modules.archives.service.ArchiveCacheService;
import com.lemuridaelabs.honeymcp.modules.events.service.EventLoggingService;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.context.McpSyncRequestContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static com.lemuridaelabs.honeymcp.modules.events.dto.HoneyEventType.MCP;

@RequiredArgsConstructor
@Component
@Slf4j
public class ArchiveMcpResources {

    private final EventLoggingService eventLoggingService;

    private final ArchiveCacheService archiveCacheService;

    private final ObjectMapper objectMapper;

    @McpResource(
            uri = "archive-file://{id}",
            name = "Archive File",
            description = "Provides archive file information")
    public McpSchema.ReadResourceResult getArchiveFileResource(McpSyncRequestContext context, String id) {

        log.info("Getting Archive File Resource, id={}.", id);

        var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        var remoteIp = (attributes.getRequest() != null) ? attributes.getRequest().getRemoteAddr() : null;

        eventLoggingService.highEvent(
                remoteIp,
                null, MCP, true,
                String.format("Attemped MCP Archive File id=%s.", id), null);

        var archiveFileWrapper = archiveCacheService.getArchiveFileRecord(id);

        if (archiveFileWrapper == null) {
            log.warn("No file found for request, id={}.", id);
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(
                            "archive-file://" + id,
                            "application/json",
                            null)));
        }

        var archiveFile = archiveFileWrapper.getFile();
        var archiveFileJson = "";

        try {
            archiveFileJson = objectMapper.writeValueAsString(archiveFile);
        } catch (Exception e) {
            log.error("Failure while serializing the data to JSON, id={}", id, e);
        }

        return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(
                        "archive-file://" + id,
                        "application/json",
                        archiveFileJson)));
    }
}

