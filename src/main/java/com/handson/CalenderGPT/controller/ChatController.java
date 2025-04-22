package com.handson.CalenderGPT.controller;

import com.handson.CalenderGPT.context.CalendarContext;
import com.handson.CalenderGPT.model.User;
import com.handson.CalenderGPT.service.ConversationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ConversationService conversationService;
    private final CalendarContext calendarContext;

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @PostMapping("/message")
    public String handleMessage(@AuthenticationPrincipal User user,
                                @RequestBody String message,
                                HttpServletRequest request) {
        log.info("Received message from {}: {}", user.getEmail(), message);
        return conversationService.handlePrompt(message, user, calendarContext);

    }
}
