package com.handson.CalenderGPT.repository;

import com.handson.CalenderGPT.model.User;
import com.handson.CalenderGPT.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findBySessionId(String sessionId);
    Optional<UserSession> findByUser(User user);
}