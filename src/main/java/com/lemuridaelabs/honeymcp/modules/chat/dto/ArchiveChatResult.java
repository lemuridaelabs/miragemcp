package com.lemuridaelabs.honeymcp.modules.chat.dto;

import com.lemuridaelabs.honeymcp.modules.archives.dto.ArchiveFileRecord;

import java.util.List;

public record ArchiveChatResult(String conversationId, String archiveName, List<ArchiveFileRecord> records, long durationMs) {
}
