package com.lemuridaelabs.miragemcp.modules.archives.tools;

import com.lemuridaelabs.miragemcp.modules.events.service.EventLoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpComplete;
import org.springaicommunity.mcp.context.McpSyncRequestContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static com.lemuridaelabs.miragemcp.modules.events.dto.HoneyEventType.MCP;

/**
 * MCP completion provider for archive name auto-completion.
 *
 * <p>Provides auto-completion suggestions for archive names based on user input prefix.
 * This helps AI assistants discover available archives through the MCP protocol.</p>
 *
 * @see ArchiveMcpTools
 * @since 1.0
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class ArchiveMcpCompletions {

    private final ArchiveMcpTools archiveMcpTools;

    private final EventLoggingService eventLoggingService;


    @McpComplete(prompt = "archives")
    public List<String> completeArchiveNames(McpSyncRequestContext context,
                                             String prefix) {

        log.info("Getting Archive Names, context={}.", context);

        var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        var remoteIp = (attributes.getRequest() != null) ? attributes.getRequest().getRemoteAddr() : null;

        eventLoggingService.lowEvent(
                remoteIp,
                null,
                MCP,
                true,
                String.format("Returning Archive Names for prefix=%s.", prefix),
                null);

        return archiveMcpTools.getAllArchives().stream()
                .filter(archiveName -> archiveName.toLowerCase().startsWith(prefix.toLowerCase()))
                .limit(10)
                .toList();
    }

}
