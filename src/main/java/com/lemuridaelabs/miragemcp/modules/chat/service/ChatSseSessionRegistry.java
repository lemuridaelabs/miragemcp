package com.lemuridaelabs.miragemcp.modules.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that holds persistent SSE emitters keyed by conversation ID.
 * Any server component can push events to a session's SSE stream via this registry.
 */
@Slf4j
@Service
public class ChatSseSessionRegistry {

    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Registers a new persistent SSE emitter for the given conversation.
     * If an emitter already exists for this conversation, it is completed and replaced.
     *
     * @param conversationId the conversation identifier
     * @return the newly created SseEmitter
     */
    public SseEmitter register(String conversationId) {
        var existing = emitters.remove(conversationId);
        if (existing != null) {
            try {
                existing.complete();
            } catch (Exception ignored) {
                // old emitter may already be completed
            }
        }

        var emitter = new SseEmitter(0L);

        emitter.onCompletion(() -> emitters.remove(conversationId, emitter));
        emitter.onTimeout(() -> emitters.remove(conversationId, emitter));
        emitter.onError(ex -> emitters.remove(conversationId, emitter));

        emitters.put(conversationId, emitter);
        return emitter;
    }

    /**
     * Sends an SSE event to the emitter registered for the given conversation.
     *
     * @param conversationId the conversation identifier
     * @param event the SSE event to send
     */
    public void send(String conversationId, SseEmitter.SseEventBuilder event) {
        var emitter = emitters.get(conversationId);
        if (emitter == null) {
            log.warn("No SSE emitter registered for conversation {}", conversationId);
            return;
        }
        try {
            emitter.send(event);
        } catch (IOException ex) {
            log.debug("Failed to send SSE event for conversation {}", conversationId, ex);
            emitters.remove(conversationId, emitter);
        }
    }

    /**
     * Removes and completes the emitter for the given conversation.
     *
     * @param conversationId the conversation identifier
     */
    public void remove(String conversationId) {
        var emitter = emitters.remove(conversationId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // emitter may already be completed
            }
        }
    }
}
