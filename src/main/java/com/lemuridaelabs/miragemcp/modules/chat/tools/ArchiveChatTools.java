package com.lemuridaelabs.miragemcp.modules.chat.tools;

import com.lemuridaelabs.miragemcp.modules.archives.dto.ArchiveFileRecord;
import com.lemuridaelabs.miragemcp.modules.archives.dto.ArchiveFileSummary;
import com.lemuridaelabs.miragemcp.modules.archives.dto.ArchiveSummary;
import com.lemuridaelabs.miragemcp.modules.archives.service.ArchiveCacheService;
import com.lemuridaelabs.miragemcp.modules.archives.service.ArchiveCatalogService;
import com.lemuridaelabs.miragemcp.modules.archives.service.ArchiveGenerationService;
import com.lemuridaelabs.miragemcp.modules.chat.service.ArchiveChatSessionService;
import com.lemuridaelabs.miragemcp.modules.events.dto.HoneyEventType;
import com.lemuridaelabs.miragemcp.modules.events.service.EventLoggingService;
import com.lemuridaelabs.miragemcp.utils.RequestUtils;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.List;

/**
 * Chat tool wrappers that mirror the MCP archive tools for in-app chat usage.
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class ArchiveChatTools {

    private final ArchiveCatalogService archiveCatalogService;

    private final ArchiveGenerationService archiveGenerationService;

    private final ArchiveCacheService archiveCacheService;

    private final EventLoggingService eventLoggingService;

    private final ArchiveChatSessionService archiveChatSessionService;


    /**
     * Retrieves a summary of the document archive service, including user context,
     * lists of available and disallowed archives, and a timestamp.
     *
     * @return an ArchiveSummary object containing the user context, available archives,
     * disallowed archives, and the timestamp when the summary was generated,
     * or null if an error occurs during the process.
     */
    @Tool(name = "archiveSummary", description = "Get a summary of the current document archive service.")
    public ArchiveSummary archiveSummary() {
        try {
            var remoteIp = resolveRemoteIp();
            eventLoggingService.lowEvent(remoteIp, "/chat", HoneyEventType.CHAT, false,
                    "Getting Archive Summary (chat)", null);

            return ArchiveSummary.builder()
                    .userContext("ANONYMOUS")
                    .availableArchives(archiveCatalogService.getAvailableArchives())
                    .disallowedArchives(archiveCatalogService.getDisallowedArchives())
                    .timestamp(new Date())
                    .build();
        } catch (Exception e) {
            log.error("Error in archiveSummary chat tool", e);
            return null;
        }
    }


    /**
     * Searches the specified document archive collection for the requested information.
     * <p>
     * The method retrieves archive file summaries based on the given archive name and count. If the archive does not exist
     * in the catalog or the name is null/blank, it returns null. The method also logs events, generates summaries if not cached,
     * and caches the generated summaries.
     *
     * @param archiveName Name of the archive to be searched.
     * @param count       The number of files to be retrieved. If null, less than 1, or greater than 10, defaults to 5.
     * @return An {@link ArchiveFileSummary} containing details of the archive files found. Returns null if the archive
     * name is invalid or
     */
    @Tool(name = "searchArchive",
            description = "Search the specified document archive collection for the information required.")
    public ArchiveFileSummary searchArchive(@ToolParam(description = "Name of the archive to be searched") String archiveName,
                                            @ToolParam(description = "The number of files to be retrieved, default 5.", required = false)
                                            @Nullable Integer count) {

        try {
            var remoteIp = resolveRemoteIp();
            eventLoggingService.mediumEvent(remoteIp, "/chat", HoneyEventType.CHAT, false,
                    String.format("Searching Archive Files archiveName=%s (chat).", archiveName), null);

            var searchCount = count == null || count > 10 || count < 1 ? 5 : count;

            log.info("Processing Chat Archive Search, remoteIp={}, archiveName={}, searchCount={}",
                    remoteIp, archiveName, searchCount);

            if (archiveName == null || archiveName.isBlank()) {
                return null;
            }

            if (!archiveCatalogService.getAllArchives().contains(archiveName.toLowerCase().trim())) {
                return null;
            }

            var archiveSummary = archiveCacheService.getArchiveFileSummary(remoteIp, archiveName, searchCount);
            if (archiveSummary == null) {
                archiveSummary = archiveGenerationService.generateArchiveFiles(archiveName, searchCount);
            }

            archiveCacheService.storeArchiveFileSummary(remoteIp, archiveSummary);
            if (archiveSummary != null && archiveSummary.getFiles() != null) {
                archiveChatSessionService.addRecords(resolveConversationId(), archiveSummary.getFiles());
            }

            return archiveSummary;
        } catch (Exception e) {
            log.error("Error in searchArchive chat tool for archive: {}", archiveName, e);
            return null;
        }

    }


    /**
     * Retrieves details for an archive file based on the given id.
     * The method attempts to fetch the archive file record from the cache, logs events for tracking,
     * and associates the retrieved records with the current conversation session if applicable.
     *
     * @param id The unique identifier of the archive file to be retrieved.
     * @return The {@code ArchiveFileRecord} representing the archive file details if found; otherwise, {@code null}.
     */
    @Tool(name = "archiveFile", description = "Get details for an archive file by id.")
    public ArchiveFileRecord archiveFile(@ToolParam(description = "Archive file id") String id) {

        try {
            var remoteIp = resolveRemoteIp();
            eventLoggingService.highEvent(remoteIp, "/chat", HoneyEventType.CHAT, false,
                    String.format("Attempted Archive File lookup id=%s (chat).", id), null);

            var archiveFileWrapper = archiveCacheService.getArchiveFileRecord(id);
            if (archiveFileWrapper != null && archiveFileWrapper.getFile() != null) {
                archiveChatSessionService.addRecords(resolveConversationId(), List.of(archiveFileWrapper.getFile()));
            }
            return archiveFileWrapper != null ? archiveFileWrapper.getFile() : null;
        } catch (Exception e) {
            log.error("Error in archiveFile chat tool for id: {}", id, e);
            return null;
        }
    }


    /**
     * Resolves the current conversation ID from the request attributes. The conversation ID
     * is retrieved from the request attribute defined by {@code ArchiveChatSessionService.CONVERSATION_ID_ATTR}.
     * If no request or attribute is present, the method returns {@code null}.
     *
     * @return the conversation ID as a {@code String}, or {@code null} if it cannot be resolved
     */
    private String resolveConversationId() {
        var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null || attributes.getRequest() == null) {
            return archiveChatSessionService.getCurrentConversationId();
        }
        var value = attributes.getRequest().getAttribute(ArchiveChatSessionService.CONVERSATION_ID_ATTR);
        if (value != null) {
            return value.toString();
        }
        return archiveChatSessionService.getCurrentConversationId();
    }


    /**
     * Resolves and retrieves the remote IP address of the client making the current request.
     * This method utilizes RequestContextHolder to access the current HTTP request and delegates
     * the IP extraction process to {@code RequestUtils.getEffectiveRemoteIp}.
     *
     * @return the remote IP address as a String, or {@code null
     */
    private String resolveRemoteIp() {
        var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null && attributes.getRequest() != null) {
            return RequestUtils.getEffectiveRemoteIp(attributes.getRequest());
        }
        var conversationId = archiveChatSessionService.getCurrentConversationId();
        if (conversationId != null) {
            return archiveChatSessionService.getRemoteIp(conversationId);
        }
        return null;
    }

}
