package com.lemuridaelabs.miragemcp.modules.chat.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web controller for the archive chat page.
 */
@Controller
public class ChatPageController {

    /**
     * Handles HTTP GET requests for the chat page. Just a simple standard
     * landing page for the chat interaction.
     *
     * @return The name of the view representing the chat page.
     */
    @GetMapping("/chat")
    public String chatIndex() {
        return "chat/index";
    }

}
