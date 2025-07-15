package com.handson.CalenderGPT.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "conversations")
public class Conversation {
    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    private User user;


}


