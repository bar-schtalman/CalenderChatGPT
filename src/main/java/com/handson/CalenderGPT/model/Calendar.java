package com.handson.CalenderGPT.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * A custom Calendar class, distinct from Google’s Calendar.
 * Useful if you want to store or manipulate your own copy of calendar data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Calendar {
    private String id;
    private String summary;
    private String timeZone;
    private List<Event> events;
}
