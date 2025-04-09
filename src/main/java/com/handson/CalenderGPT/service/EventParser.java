package com.handson.CalenderGPT.service;

import com.handson.CalenderGPT.model.Event;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class EventParser {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public Event parseFromJson(com.fasterxml.jackson.databind.JsonNode jsonNode) {
        try {
            Event event = new Event();
            event.setSummary(jsonNode.path("summary").asText("No Title"));
            event.setDescription(jsonNode.path("description").asText(""));
            event.setLocation(jsonNode.path("location").asText(""));

            String rawStart = jsonNode.path("start").asText("").replace("Z", "");
            String rawEnd = jsonNode.path("end").asText("").replace("Z", "");

            event.setStart(LocalDateTime.parse(rawStart, ISO_FORMATTER));
            event.setEnd(LocalDateTime.parse(rawEnd, ISO_FORMATTER));
            event.setTimeZone(ZoneId.systemDefault().toString());

            return event;
        } catch (Exception e) {
            // fallback in case of parsing failure
            Event fallback = new Event();
            fallback.setSummary("Default Event");
            fallback.setDescription("No Description");
            fallback.setLocation("No Location");
            fallback.setStart(LocalDateTime.now());
            fallback.setEnd(LocalDateTime.now().plusHours(1));
            fallback.setTimeZone(ZoneId.systemDefault().toString());
            return fallback;
        }
    }
}
