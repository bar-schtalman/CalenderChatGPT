package com.handson.CalenderGPT.model;

import java.util.List;

/**
 * A custom Calendar class, distinct from Google’s Calendar.
 * Useful if you want to store or manipulate your own copy of calendar data.
 */
public class Calendar {

    private String id;
    private String summary;
    private String timeZone;
    private List<Event> events;

    // Private constructor for builder
    private Calendar(Builder builder) {
        this.id = builder.id;
        this.summary = builder.summary;
        this.timeZone = builder.timeZone;
        this.events = builder.events;
    }

    // Default constructor (optional, if needed)
    public Calendar() {
    }

    public Calendar(String id, String summary, String timeZone, List<Event> events) {
        this.id = id;
        this.summary = summary;
        this.timeZone = timeZone;
        this.events = events;
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

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    // Builder class
    public static class Builder {
        private String id;
        private String summary;
        private String timeZone;
        private List<Event> events;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder timeZone(String timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        public Builder events(List<Event> events) {
            this.events = events;
            return this;
        }

        public Calendar build() {
            return new Calendar(this);
        }
    }
}
