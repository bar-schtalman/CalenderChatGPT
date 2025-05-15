package com.handson.CalenderGPT.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Event {

    private String id;
    private String summary;       // e.g., "Team Meeting"
    private String description;   // optional: "Discuss project status"
    private String location;      // optional: "Zoom, Office, etc."
    private LocalDateTime start;  // or a String if you prefer
    private LocalDateTime end;    // or a String if you prefer
    private List<String> guests;
    private String timeZone;      // ✅ Added time zone

    // Getters and setters

}
