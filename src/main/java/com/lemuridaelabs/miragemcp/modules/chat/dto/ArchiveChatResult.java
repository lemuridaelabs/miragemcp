package com.lemuridaelabs.miragemcp.modules.chat.dto;

import com.lemuridaelabs.miragemcp.modules.archives.dto.ArchiveFileRecord;

import java.util.List;

public record ArchiveChatResult(String conversationId, String archiveName, List<ArchiveFileRecord> records, long durationMs) {
}
