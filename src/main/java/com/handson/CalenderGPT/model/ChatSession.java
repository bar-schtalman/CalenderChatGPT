package com.handson.CalenderGPT.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
public class ChatSession {

    @Id
    @GeneratedValue
    private UUID id;

    private String title;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(optional = false)
    private User user;

    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserMessage> messages;

    // Getters & Setters
    public UUID getId() { return id; }

    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getUser() { return user; }

    public void setUser(User user) { this.user = user; }

    public List<UserMessage> getMessages() { return messages; }

    public void setMessages(List<UserMessage> messages) { this.messages = messages; }
}
