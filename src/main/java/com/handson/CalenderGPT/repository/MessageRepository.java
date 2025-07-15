package com.handson.CalenderGPT.repository;

import com.handson.CalenderGPT.model.Conversation;
import com.handson.CalenderGPT.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByConversationOrderByTimestampAsc(Conversation conversation);
    long countByConversation(Conversation conversation);
    Optional<Message> findFirstByConversationOrderByTimestampAsc(Conversation conversation);
}
