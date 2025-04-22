package com.handson.CalenderGPT.controller;

import com.handson.CalenderGPT.model.ChatMessage;
import com.handson.CalenderGPT.model.Conversation;
import com.handson.CalenderGPT.model.User;
import com.handson.CalenderGPT.repository.ChatMessageRepository;
import com.handson.CalenderGPT.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    // 🧠 Get all conversations for the authenticated user
    @GetMapping
    public List<Conversation> getUserConversations(@AuthenticationPrincipal User user) {
        return conversationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // ➕ Start a new conversation manually
    @PostMapping
    public Conversation startNewConversation(@AuthenticationPrincipal User user) {
        Conversation convo = new Conversation();
        convo.setUser(user);
        convo.setTitle("New Chat");
        return conversationRepository.save(convo);
    }

    // 💬 Get all messages for a specific conversation
    @GetMapping("/{conversationId}/messages")
    public List<ChatMessage> getConversationMessages(
            @AuthenticationPrincipal User user,
            @PathVariable UUID conversationId
    ) {
        Conversation convo = conversationRepository.findById(conversationId)
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        return chatMessageRepository.findByConversationOrderByTimestampAsc(convo);
    }
}
