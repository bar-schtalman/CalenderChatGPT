package com.handson.CalenderGPT.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;


import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    private String firstName;
    private String lastName;
    private String sessionId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Constructors
    public User() {}

    public User(String email, String firstName, String lastName, String sessionId) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.sessionId = sessionId;
    }

    // Getters & Setters
    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
