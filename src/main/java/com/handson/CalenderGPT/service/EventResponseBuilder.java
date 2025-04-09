package com.handson.CalenderGPT.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.context.CalendarContext;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EventResponseBuilder {

    private final CalendarContext calendarContext;

    public EventResponseBuilder(CalendarContext calendarContext) {
        this.calendarContext = calendarContext;
    }

    public String buildEventCardResponse(com.handson.CalenderGPT.model.Event event, Map<String, String> created) {
        try {
            return new ObjectMapper().writeValueAsString(List.of(Map.of(
                    "role", "event",
                    "summary", event.getSummary(),
                    "date", event.getStart().toLocalDate().toString(),
                    "time", event.getStart().toLocalTime().toString() + " - " + event.getEnd().toLocalTime().toString(),
                    "calendarId", calendarContext.getCalendarId(),
                    "id", created.getOrDefault("id", ""),
                    "guests", created.getOrDefault("guests", ""),
                    "location", event.getLocation() != null ? event.getLocation() : ""
            )));
        } catch (Exception e) {
            return buildFallbackMessage(event.getSummary());
        }
    }

    public String buildNoEventsFound(String start, String end) {
        try {
            return new ObjectMapper().writeValueAsString(List.of(
                    Map.of("role", "ai", "content", "📭 No events found between " + start + " and " + end + ".")
            ));
        } catch (Exception e) {
            return buildFallbackMessage("No events found.");
        }
    }

    public String buildEventList(List<Map<String, String>> events, String calendarId) {
        try {
            List<Map<String, String>> responseList = new ArrayList<>();
            for (Map<String, String> event : events) {
                responseList.add(Map.of(
                        "role", "event",
                        "summary", event.get("summary"),
                        "date", event.get("start").split(" ")[0],
                        "time", event.get("start").split(" ")[1] + " - " + event.get("end").split(" ")[1],
                        "location", event.getOrDefault("location", "No location"),
                        "calendarId", calendarId,
                        "id", event.get("id"),
                        "guests", event.getOrDefault("guests", "")
                ));
            }
            return new ObjectMapper().writeValueAsString(responseList);
        } catch (Exception e) {
            return buildFallbackMessage("Could not build event list.");
        }
    }

    private String buildFallbackMessage(String msg) {
        return "[{\"role\":\"ai\",\"content\":\"" + msg + "\"}]";
    }

    public String formatDisplayDate(String isoDateTime) {
        try {
            return LocalDate.parse(isoDateTime.substring(0, 10))
                    .format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        } catch (Exception e) {
            return isoDateTime;
        }
    }
}
