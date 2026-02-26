package com.lemuridaelabs.honeymcp.modules.chat.service;

import com.lemuridaelabs.honeymcp.modules.chat.tools.ArchiveChatTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Service that manages chat interactions and conversation memory.
 */
@RequiredArgsConstructor
@Service
public class ArchiveChatService {

    private final ChatClient archiveChatClient;

    private final ArchiveChatTools archiveChatTools;

    private final ArchiveChatSessionService archiveChatSessionService;


    /**
     * Streams chat responses for a given conversation and user message.
     *
     * @param conversationId the unique identifier of the conversation
     * @param message the user's input message to the chat service
     * @return a reactive stream of chat responses as a Flux of strings
     */
    public Flux<String> stream(String conversationId, String message) {
        return archiveChatClient.prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(archiveChatTools)
                .user(message)
                .stream()
                .content()
                .doOnSubscribe(subscription -> archiveChatSessionService.setCurrentConversationId(conversationId))
                .doFinally(signal -> archiveChatSessionService.clearCurrentConversationId());
    }

}
