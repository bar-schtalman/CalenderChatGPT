package com.handson.CalenderGPT.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = true)
    @JoinColumn(name = "conversation_id") // formerly chat_session_id
    private Conversation conversation;

    private boolean isUser; // true = user message, false = assistant/system

    @Column(columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    private LocalDateTime timestamp;

    private String type; // e.g. "user", "assistant", "event-update"

    private String relatedEventId; // Optional: links to a specific event

    public ChatMessage(User user, Conversation conversation, boolean isUser, String content) {
        this.user = user;
        this.conversation = conversation;
        this.isUser = isUser;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(User user, Conversation conversation, boolean isUser, String content, String type, String relatedEventId) {
        this.user = user;
        this.conversation = conversation;
        this.isUser = isUser;
        this.content = content;
        this.type = type;
        this.relatedEventId = relatedEventId;
        this.timestamp = LocalDateTime.now();
    }
}
