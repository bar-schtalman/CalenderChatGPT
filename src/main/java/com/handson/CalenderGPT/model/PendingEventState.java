package com.handson.CalenderGPT.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PendingEventState {

    private String initialPrompt;

    private String intent;
    private String summary = "";
    private String start = "";
    private String end = "";
    private String location = "";
    private String description = "";
    private boolean clarificationAsked = false;

    /**
     * An event state is “complete” once summary, start, and end are all non-empty.
     */
    public boolean isComplete() {
        return !summary.isEmpty() && !start.isEmpty() && !end.isEmpty();
    }

    /**
     * Merge values from another state, but only fill in fields that are still blank.
     */
    public void mergeFrom(PendingEventState update) {
        if (this.summary.isEmpty()) this.summary = update.summary;
        if (this.start.isEmpty()) this.start = update.start;
        if (this.end.isEmpty()) this.end = update.end;
        if (this.location.isEmpty()) this.location = update.location;
        if (this.description.isEmpty()) this.description = update.description;
    }
}
