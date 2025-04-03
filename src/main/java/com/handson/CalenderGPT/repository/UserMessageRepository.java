package com.handson.CalenderGPT.repository;

import com.handson.CalenderGPT.model.UserMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserMessageRepository extends JpaRepository<UserMessage, UUID> {
    List<UserMessage> findByUserIdOrderByTimestampAsc(UUID userId);
}
