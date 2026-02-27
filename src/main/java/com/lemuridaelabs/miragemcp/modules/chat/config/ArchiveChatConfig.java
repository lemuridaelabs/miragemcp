package com.lemuridaelabs.miragemcp.modules.chat.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Configuration for the archive chat client.
 */
@Configuration
public class ArchiveChatConfig {

    /**
     * Creates and configures an archive chat client bean.
     *
     * @param builder      the builder used to construct the chat client
     * @param chatMemory   the chat memory instance for managing chat history
     * @param systemPrompt the resource containing the system prompt for the archive chat
     * @return a configured instance of ChatClient
     */
    @Bean
    public ChatClient archiveChatClient(ChatClient.Builder builder,
                                        ChatMemory chatMemory,
                                        @Value("classpath:/prompts/archive_chat_system.st") Resource systemPrompt) {
        return builder
                .defaultSystem(system -> system.text(systemPrompt))
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

}
