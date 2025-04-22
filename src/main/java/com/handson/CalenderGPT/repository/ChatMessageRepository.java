package com.handson.CalenderGPT.repository;

import com.handson.CalenderGPT.model.ChatMessage;
import com.handson.CalenderGPT.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByConversationOrderByTimestampAsc(Conversation conversation);
}
