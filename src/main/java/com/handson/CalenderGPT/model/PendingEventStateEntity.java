package com.handson.CalenderGPT.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingEventStateEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = true)
    private Conversation conversation;

    private String intent;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "start_time")
    private String startTime;

    @Column(name = "end_time")
    private String endTime;

    private String location;
    private String description;

    private boolean isComplete;
}
