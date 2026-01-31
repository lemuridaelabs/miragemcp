package com.lemuridaelabs.honeymcp.modules.archives.service;

import com.lemuridaelabs.honeymcp.modules.archives.dto.ArchiveFileSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class ArchiveGenerationService {

    private final ChatModel chatModel;

    @Value("classpath:/prompts/archive_generation.st")
    private Resource archiveGenerationPrompt;

    @Value("${honeymcp.server.base-url}")
    private String baseUrl;

    @Value("${honeymcp.server.file-base}")
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

        if (archiveSummary!=null) {
            archiveSummary.setArchiveName(archiveName);
            archiveSummary.setCount(count);
        }

        return archiveSummary;
    }

//    String filePromptTemplate = """
//            You need to generate synthetic file records that look realistic for the specified type.
//            Please generate {count} files.
//            The generated File URI should have a prefix of {uriPrefix} and have the ID as the end.
//            The generated Data URL should have a base path of {dataLinkUrl} and have the ID as the end.
//            These files should have been created over the last 5 years with random creation and modification dates.
//            The names and tags should be semantically reasonable, and should reflect internal documents and artifacts.
//            The summary should represent a plausible short to moderate summary of document contents, written as a user would with occasional typos or errors.
//            Some summaries should be very brief as if a user quickly entered it, and some may be missing as if they skipped it.
//            If you generate date or time references in the file name the dates in the file should also match these.
//            The file Id field should be a random UUID.
//            The file types should be normal document or file types for the type, for example office files for accounting or source code for development.
//            These files are supposed to look like user created data and should be realistic. Users will occasionally have typos or entries like backup or (1) in filenames as they work on records.
//            The intent is to have a set of realistic records that look consistent and reasonable.
//            Type: {archiveType}
//            """;

}
