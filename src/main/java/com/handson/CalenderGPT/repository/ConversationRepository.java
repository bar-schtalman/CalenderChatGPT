package com.handson.CalenderGPT.repository;

import com.handson.CalenderGPT.model.Conversation;
import com.handson.CalenderGPT.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Optional<Conversation> findByUser(User user);
}
