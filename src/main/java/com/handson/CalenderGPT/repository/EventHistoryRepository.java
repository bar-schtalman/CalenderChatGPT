package com.handson.CalenderGPT.repository;

import com.handson.CalenderGPT.model.EventHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventHistoryRepository extends JpaRepository<EventHistory, UUID> {
    List<EventHistory> findByUserIdOrderByTimestampDesc(UUID userId);
}
