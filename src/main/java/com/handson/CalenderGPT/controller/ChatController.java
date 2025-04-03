package com.handson.CalenderGPT.controller;

import com.handson.CalenderGPT.service.ConversationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ChatController {

    private final ConversationService conversationService;

    public ChatController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String prompt, @RequestParam UUID userId) {
        return conversationService.handlePrompt(prompt, userId);
    }

}
