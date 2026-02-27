package com.lemuridaelabs.miragemcp.modules.archives.service;

import com.lemuridaelabs.miragemcp.modules.archives.dto.ArchiveFileSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Service for generating synthetic archive files using AI.
 *
 * <p>This service leverages Spring AI's ChatClient to generate realistic-looking
 * synthetic file records based on configurable prompts. The generated files serve
 * as deceptive content for the honeypot, appearing as legitimate internal documents.</p>
 *
 * @see ArchiveFileSummary
 * @see ArchiveCacheService
 * @since 1.0
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class ArchiveGenerationService {

    private final ChatModel chatModel;

    @Value("classpath:/prompts/archive_generation.st")
    private Resource archiveGenerationPrompt;

    @Value("${miragemcp.server.base-url}")
    private String baseUrl;

    @Value("${miragemcp.server.file-base}")
    private String fileBasePath;

    public ArchiveFileSummary generateArchiveFiles(String archiveName, Integer count) {

        log.info("Generating archive files for archiveName={}, count={}", archiveName, count);

        var dataLinkUrl = baseUrl + fileBasePath;

        var archiveSummary = ChatClient.create(chatModel).prompt()
                .system(u -> u.text(archiveGenerationPrompt)
                        .param("uriPrefix", "archive-file://")
                        .param("dataLinkUrl", dataLinkUrl)
                        .param("archiveType", archiveName)
                        .param("count", count))
                .call()
                .entity(ArchiveFileSummary.class);

        if (archiveSummary != null) {
            archiveSummary.setArchiveName(archiveName);
            archiveSummary.setCount(count);
        }

        return archiveSummary;
    }

}
