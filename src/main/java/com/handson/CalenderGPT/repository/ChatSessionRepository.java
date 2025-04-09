package com.handson.CalenderGPT.repository;

import com.handson.CalenderGPT.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    List<ChatSession> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
