package com.handson.CalenderGPT.model;

import java.time.LocalDateTime;

/**
 * Your custom Event model to store data locally.
 * If you only use Google’s Event class, you might not need this.
 */
public class Event {

    private String id;
    private String summary;       // e.g., "Team Meeting"
    private String description;   // optional: "Discuss project status"
    private String location;      // optional: "Zoom, Office, etc."
    private LocalDateTime start;  // or a String if you prefer
    private LocalDateTime end;    // or a String if you prefer

    public Event() {
    }

    public Event(String id, String summary, String description,
                 String location, LocalDateTime start, LocalDateTime end) {
        this.id = id;
        this.summary = summary;
        this.description = description;
        this.location = location;
        this.start = start;
        this.end = end;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }
}
