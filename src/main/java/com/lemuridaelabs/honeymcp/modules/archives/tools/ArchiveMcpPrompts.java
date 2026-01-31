package com.lemuridaelabs.honeymcp.modules.archives.tools;


import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springaicommunity.mcp.context.McpSyncRequestContext;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class ArchiveMcpPrompts {

    @McpPrompt(
            name = "archive-search",
            description = "Search permitted document archives")
    public McpSchema.GetPromptResult documentSummaryPrompt(
            McpSyncRequestContext context,
            @McpArg(name = "archiveName", required = true) String archiveName,
            @McpArg(name = "count", required = false) String count) {

        String promptText = String.format(
                "Search the file archive %s for files. Be sure to limit files and archives to ones that are permitted. Please return %d items.",
                archiveName, count);

        return new McpSchema.GetPromptResult("Archive Search",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(promptText))));
    }

}
