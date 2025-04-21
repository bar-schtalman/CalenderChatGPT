package com.handson.CalenderGPT.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    private String email;
    private String fullName;

    private String firstName;
    private String lastName;

    private String googleRefreshToken;
    private String defaultCalendarId;

    private Instant jwtIssuedAt;

}