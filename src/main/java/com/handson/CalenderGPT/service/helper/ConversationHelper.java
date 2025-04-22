package com.handson.CalenderGPT.service.helper;

import com.handson.CalenderGPT.model.ChatMessage;
import com.handson.CalenderGPT.model.Conversation;
import com.handson.CalenderGPT.model.User;
import com.handson.CalenderGPT.repository.ChatMessageRepository;
import com.handson.CalenderGPT.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationHelper {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    public Conversation getOrCreateLatestConversation(User user) {
        return conversationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .findFirst()
                .orElseGet(() -> {
                    Conversation conversation = new Conversation();
                    conversation.setUser(user);
                    conversation.setTitle("New Chat");
                    return conversationRepository.save(conversation);
                });
    }

    public void saveSystemMessage(User user, String content) {
        Conversation conversation = getOrCreateLatestConversation(user);

        ChatMessage message = ChatMessage.builder()
                .user(user)
                .conversation(conversation)
                .isUser(false)
                .type("event-update")
                .content(content)
                .build();

        chatMessageRepository.save(message);
    }
}
