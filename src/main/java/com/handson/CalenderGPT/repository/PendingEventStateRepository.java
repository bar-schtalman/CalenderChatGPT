package com.handson.CalenderGPT.repository;

import com.handson.CalenderGPT.model.PendingEventStateEntity;
import com.handson.CalenderGPT.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PendingEventStateRepository extends JpaRepository<PendingEventStateEntity, UUID> {
    Optional<PendingEventStateEntity> findByUser(User user);
    void deleteByUser(User user);
}
