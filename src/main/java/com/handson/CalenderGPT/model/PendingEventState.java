package com.handson.CalenderGPT.model;

public class PendingEventState {

    private String intent;
    private String summary = "";
    private String start = "";
    private String end = "";
    private String location = "";
    private String description = "";
    private boolean clarificationAsked = false;


    public boolean isComplete() {
        return !summary.isEmpty() && !start.isEmpty() && !end.isEmpty();
    }

    // Getters and setters
    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isClarificationAsked() {
        return clarificationAsked;
    }

    public void setClarificationAsked(boolean clarificationAsked) {
        this.clarificationAsked = clarificationAsked;
    }

    // Optional: merge helper
    public void mergeFrom(PendingEventState update) {
        if (this.summary.isEmpty()) this.summary = update.summary;
        if (this.start.isEmpty()) this.start = update.start;
        if (this.end.isEmpty()) this.end = update.end;
        if (this.location.isEmpty()) this.location = update.location;
        if (this.description.isEmpty()) this.description = update.description;
    }
}
