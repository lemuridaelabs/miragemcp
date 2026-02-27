package com.lemuridaelabs.miragemcp.modules.chat.service;

import com.lemuridaelabs.miragemcp.modules.archives.dto.ArchiveFileRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-conversation archive records discovered during a chat request.
 */
@Service
public class ArchiveChatSessionService {

    public static final String CONVERSATION_ID_ATTR = "archiveChatConversationId";

    private final Map<String, List<ArchiveFileRecord>> recordsByConversation = new ConcurrentHashMap<>();
    private final Map<String, String> remoteIpByConversation = new ConcurrentHashMap<>();
    private final ThreadLocal<String> currentConversationId = new ThreadLocal<>();
    private volatile String activeConversationId;

    /**
     * Initializes a new chat session for the given conversation ID by creating an empty record list.
     *
     * @param conversationId the unique identifier of the conversation; if null, the method will not perform any actions
     */
    public void startRequest(String conversationId) {
        if (conversationId == null) {
            return;
        }
        recordsByConversation.put(conversationId, new ArrayList<>());
        activeConversationId = conversationId;
    }

    /**
     * Adds a collection of archive file records to the specified conversation's record list.
     * If the conversation does not exist in the records map, it is created. Records are deduplicated
     * before being added to the conversation's list.
     *
     * @param conversationId the unique identifier of the conversation to which the records should be added
     * @param records the list of {@code ArchiveFileRecord} elements to add to the conversation's existing records
     */
    public void addRecords(String conversationId, List<ArchiveFileRecord> records) {
        if (conversationId == null || records == null || records.isEmpty()) {
            return;
        }
        var existing = recordsByConversation.computeIfAbsent(conversationId, id -> new ArrayList<>());
        var deduped = new LinkedHashMap<String, ArchiveFileRecord>();
        for (var record : existing) {
            addRecordToMap(deduped, record);
        }
        for (var record : records) {
            addRecordToMap(deduped, record);
        }
        existing.clear();
        existing.addAll(deduped.values());
    }

    /**
     * Retrieves the list of archive file records associated with a specific conversation.
     * If the conversationId is null or no records are found for the given conversation, an empty list is returned.
     *
     * @param conversationId the unique identifier of the conversation for which to fetch records
     * @return a list of {@code ArchiveFileRecord} objects associated with the given conversation, or an empty list if no records are found
     */
    public List<ArchiveFileRecord> getRecords(String conversationId) {
        if (conversationId == null) {
            return List.of();
        }
        return recordsByConversation.getOrDefault(conversationId, List.of());
    }

    public void setCurrentConversationId(String conversationId) {
        if (conversationId == null) {
            currentConversationId.remove();
            return;
        }
        currentConversationId.set(conversationId);
    }

    public void clearCurrentConversationId() {
        currentConversationId.remove();
    }

    public String getCurrentConversationId() {
        var threadLocal = currentConversationId.get();
        return threadLocal != null ? threadLocal : activeConversationId;
    }

    public void setRemoteIp(String conversationId, String remoteIp) {
        if (conversationId != null && remoteIp != null) {
            remoteIpByConversation.put(conversationId, remoteIp);
        }
    }

    public String getRemoteIp(String conversationId) {
        if (conversationId == null) {
            return null;
        }
        return remoteIpByConversation.get(conversationId);
    }

    /**
     * Adds an {@link ArchiveFileRecord} to the provided map using a unique key.
     * If the record's ID is present and valid, it will be used as the key.
     * Otherwise, a fallback key is generated using the record's name, creation time, and modification time.
     *
     * @param deduped the map where the record will be added, using the calculated key
     * @param record the record to be added to the map; can be null, in which case no action is taken
     */
    private void addRecordToMap(Map<String, ArchiveFileRecord> deduped, ArchiveFileRecord record) {
        if (record == null) {
            return;
        }
        var id = record.getId();
        if (id != null && !id.isBlank()) {
            deduped.put(id, record);
            return;
        }
        var fallbackKey = (record.getName() != null ? record.getName() : "unknown")
                + "::" + (record.getCreated() != null ? record.getCreated().getTime() : "")
                + "::" + (record.getModified() != null ? record.getModified().getTime() : "");
        deduped.put(fallbackKey, record);
    }

}
