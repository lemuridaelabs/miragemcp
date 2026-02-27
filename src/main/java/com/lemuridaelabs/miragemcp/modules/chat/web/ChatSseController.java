package com.lemuridaelabs.miragemcp.modules.chat.web;

import com.lemuridaelabs.miragemcp.modules.chat.dto.ArchiveChatRequest;
import com.lemuridaelabs.miragemcp.modules.chat.dto.ArchiveChatResult;
import com.lemuridaelabs.miragemcp.modules.chat.dto.ChatSessionResponse;
import com.lemuridaelabs.miragemcp.modules.chat.service.ArchiveChatService;
import com.lemuridaelabs.miragemcp.modules.chat.service.ArchiveChatSessionService;
import com.lemuridaelabs.miragemcp.modules.chat.service.ChatSseSessionRegistry;
import com.lemuridaelabs.miragemcp.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Controller for archive chat streaming and results.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/chat")
@Slf4j
public class ChatSseController {

    private final ArchiveChatService archiveChatService;
    private final ArchiveChatSessionService archiveChatSessionService;
    private final ChatSseSessionRegistry sseSessionRegistry;

    /**
     * Creates a new chat session identifier.
     */
    @PostMapping(path = "/session", produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatSessionResponse createSession() {
        return new ChatSessionResponse(UUID.randomUUID().toString());
    }

    /**
     * Opens a persistent SSE connection for the given conversation.
     * The client connects once per session and receives AI chat chunks as they are generated.
     *
     * @param conversationId the conversation identifier
     * @return a persistent SseEmitter
     */
    @GetMapping(path = "/stream/{conversationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String conversationId) {
        return sseSessionRegistry.register(conversationId);
    }

    /**
     * Accepts a user message, runs the AI chat, pushes text chunks to the persistent SSE emitter,
     * then returns the conversation's records as JSON.
     *
     * @param request the chat request containing the conversation id and message
     * @param servletRequest the underlying servlet request for setting context
     * @return a result payload with the archive records
     */
    @PostMapping(path = "/message", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ArchiveChatResult message(@RequestBody ArchiveChatRequest request,
                                     HttpServletRequest servletRequest) {

        var message = request != null ? request.message() : null;
        var conversationId = request != null ? request.conversationId() : null;
        var resolvedConversationId = StringUtils.hasText(conversationId)
                ? conversationId
                : UUID.randomUUID().toString();

        if (servletRequest != null) {
            servletRequest.setAttribute(ArchiveChatSessionService.CONVERSATION_ID_ATTR, resolvedConversationId);
        }

        archiveChatSessionService.startRequest(resolvedConversationId);

        if (servletRequest != null) {
            archiveChatSessionService.setRemoteIp(resolvedConversationId,
                    RequestUtils.getEffectiveRemoteIp(servletRequest));
        }

        if (!StringUtils.hasText(message)) {
            sseSessionRegistry.send(resolvedConversationId,
                    SseEmitter.event().name("error").data("Message is required."));
            return new ArchiveChatResult(resolvedConversationId, null, archiveChatSessionService.getRecords(resolvedConversationId), 0L);
        }

        var startTime = System.currentTimeMillis();
        var latch = new CountDownLatch(1);

        archiveChatService.stream(resolvedConversationId, message)
                .subscribe(
                        chunk -> sseSessionRegistry.send(resolvedConversationId,
                                SseEmitter.event().data(Map.of("text", chunk))),
                        error -> {
                            log.error("Chat stream error for conversation {}", resolvedConversationId, error);
                            sseSessionRegistry.send(resolvedConversationId,
                                    SseEmitter.event().name("error").data("Stream error."));
                            latch.countDown();
                        },
                        () -> {
                            sseSessionRegistry.send(resolvedConversationId,
                                    SseEmitter.event().name("done").data("[DONE]"));
                            latch.countDown();
                        }
                );

        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Message processing interrupted for conversation {}", resolvedConversationId);
        }

        var durationMs = System.currentTimeMillis() - startTime;
        var records = archiveChatSessionService.getRecords(resolvedConversationId);
        var archiveName = records.isEmpty() ? null : resolvedConversationId;

        return new ArchiveChatResult(resolvedConversationId, archiveName, records, durationMs);
    }
}
