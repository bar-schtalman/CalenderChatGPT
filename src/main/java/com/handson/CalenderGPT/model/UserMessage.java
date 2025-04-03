package com.handson.CalenderGPT.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class UserMessage {
    public UserMessage(){};

    public UserMessage(User user,boolean isUser, String content) {
        this.user = user;
        this.isUser = isUser;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    @ManyToOne(optional = false)
    private User user;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }



    public boolean isUser() {
        return isUser;
    }

    public void setUser(boolean user) {
        isUser = user;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Id
    @GeneratedValue
    private UUID id;



    private boolean isUser; // true = user message, false = assistant response

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime timestamp = LocalDateTime.now();
}
