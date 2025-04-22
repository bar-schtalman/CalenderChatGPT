package com.handson.CalenderGPT.repository;

import com.handson.CalenderGPT.model.Conversation;
import com.handson.CalenderGPT.model.PendingEventStateEntity;
import com.handson.CalenderGPT.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PendingEventStateRepository extends JpaRepository<PendingEventStateEntity, UUID> {
    Optional<PendingEventStateEntity> findByUserAndConversation(User user, Conversation conversation);
    void deleteByUserAndConversation(User user, Conversation conversation);
}
