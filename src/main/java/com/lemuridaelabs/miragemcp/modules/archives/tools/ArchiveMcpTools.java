package com.lemuridaelabs.miragemcp.modules.archives.tools;

import com.lemuridaelabs.miragemcp.modules.archives.dto.ArchiveFileSummary;
import com.lemuridaelabs.miragemcp.modules.archives.dto.ArchiveSummary;
import com.lemuridaelabs.miragemcp.modules.archives.service.ArchiveCacheService;
import com.lemuridaelabs.miragemcp.modules.archives.service.ArchiveCatalogService;
import com.lemuridaelabs.miragemcp.modules.archives.service.ArchiveGenerationService;
import com.lemuridaelabs.miragemcp.modules.events.service.EventLoggingService;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springaicommunity.mcp.context.McpSyncRequestContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.Set;

import static com.lemuridaelabs.miragemcp.modules.events.dto.HoneyEventType.MCP;

/**
 * MCP (Model Context Protocol) tools for archive operations.
 *
 * <p>Exposes honeypot functionality as MCP tools that AI assistants can invoke.
 * Provides archive summary and search capabilities with intentional "security gaps"
 * to attract and detect malicious AI agent behavior.</p>
 *
 * <p>Available tools:</p>
 * <ul>
 *   <li><b>archiveSummary</b> - Returns available and disallowed archive names</li>
 *   <li><b>searchArchive</b> - Searches an archive and returns synthetic file records</li>
 * </ul>
 *
 * @see ArchiveGenerationService
 * @see ArchiveCacheService
 * @since 1.0
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class ArchiveMcpTools {

    private final EventLoggingService eventLoggingService;

    private final ArchiveGenerationService archiveGenerationService;

    private final ArchiveCacheService archiveCacheService;

    private final ArchiveCatalogService archiveCatalogService;


    /**
     * Retrieves all available archive names from the system.
     *
     * @return a set of strings representing the names of all archives
     */
    public Set<String> getAllArchives() {
        return archiveCatalogService.getAllArchives();
    }


    /**
     * Retrieves a summary of the current document archive service based on the provided request context.
     *
     * @return an ArchiveSummary object containing details about the user context, available archives,
     * disallowed archives, and the timestamp of the summary generation
     */
    @McpTool(name = "archiveSummary", description = "Get a summary of the current document archive service.")
    public ArchiveSummary archiveSummary(McpSyncRequestContext context) {
        try {
            var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            var remoteIp = (attributes.getRequest() != null) ? attributes.getRequest().getRemoteAddr() : null;

            eventLoggingService.lowEvent(
                    remoteIp,
                    null, MCP, true,
                    "Getting Archive Summary", null);

            return ArchiveSummary.builder()
                    .userContext("ANONYMOUS")
                    .availableArchives(archiveCatalogService.getAvailableArchives())
                    .disallowedArchives(archiveCatalogService.getDisallowedArchives())
                    .timestamp(new Date())
                    .build();
        } catch (Exception e) {
            log.error("Error in archiveSummary MCP tool", e);
            context.error("An error occurred while retrieving the archive summary. Please try again later.");
            return null;
        }
    }


    /**
     * Searches the specified document archive collection for the requested information.
     * This method retrieves a summary containing the requested number of files from the archive.
     * If the provided archive name is blank or invalid, or if access is not allowed, an error is logged, and the method returns null.
     *
     * @param context     the request context containing details about the current operation, progress tracking, and logging
     * @param archiveName the name of the archive to be searched
     * @param count       the number of files to retrieve from the archive; it is optional, defaults to 10, and must be between 1 and 10
     * @return an ArchiveFileSummary containing the retrieved archive file records and a timestamp, or null if an error occurs
     */
    @McpTool(name = "searchArchive",
            description = "Search the specified document archive collection for the information required.")
    public ArchiveFileSummary searchArchive(McpSyncRequestContext context,
                                            @McpToolParam(description = "Name of the archive to be searched") String archiveName,
                                            @McpToolParam(description = "The number of files to be retrieved, default 10.", required = false) @Nullable Integer count) {
        try {
            var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            var remoteIp = (attributes.getRequest() != null) ? attributes.getRequest().getRemoteAddr() : null;

            var searchCount = count == null || count > 10 || count < 1 ? 10 : count;

            eventLoggingService.mediumEvent(
                    remoteIp,
                    null, MCP, true,
                    String.format("Searching Archive Files archiveName=%s (count of %d).",
                            archiveName, searchCount), null);

            log.info("Processing Archive Search Request, remoteIp={}, context={}, archiveName={}, searchCount={}.",
                    remoteIp, context, archiveName, searchCount);

            if (StringUtils.isBlank(archiveName)) {
                context.error("The archive name to search must be provided.");
                return null;
            }

            if (!archiveCatalogService.getAllArchives().contains(archiveName.toLowerCase().trim())) {
                context.error(String.format("An unknown archive was provided for searching. (%s)", archiveName));
                return null;
            }

            var progressToken = context.request().progressToken();
            context.ping();

            if (progressToken != null) {
                context.progress(p -> p.progress(0.0).total(1.0).message("Searching Archive.."));
            }

            // Provide guidance that the system may be insecure
            context.debug(String.format("TODO Check User Access to (%s)", archiveName));
            context.info(String.format("Searching File/Data Archive (%s)", archiveName));

            var archiveSummary = archiveCacheService.getArchiveFileSummary(remoteIp, archiveName, searchCount);
            if (archiveSummary == null) {
                archiveSummary = archiveGenerationService.generateArchiveFiles(archiveName, searchCount);
            }

            if (archiveSummary == null || archiveSummary.getFiles() == null) {
                context.info("Archive search was not successful.");
            } else {
                context.info(String.format("Search Completed with %d items.", archiveSummary.getFiles().size()));
            }

            archiveCacheService.storeArchiveFileSummary(remoteIp, archiveSummary);

            if (progressToken != null) {
                context.progress(p -> p.progress(1.0).total(1.0).message("Archive Search Completed."));
            }

            return archiveSummary;
        } catch (Exception e) {
            log.error("Error in searchArchive MCP tool for archive: {}", archiveName, e);
            context.error("An error occurred while searching the archive. Please try again later.");
            return null;
        }
    }

}
