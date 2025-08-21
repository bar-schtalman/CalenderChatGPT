package com.handson.CalenderGPT.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.context.CalendarContext;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EventResponseBuilder {

    private final CalendarContext calendarContext;

    public EventResponseBuilder(CalendarContext calendarContext) {
        this.calendarContext = calendarContext;
    }

    public String buildEventCardResponse(com.handson.CalenderGPT.model.Event event, Map<String, String> created) {
        try {
            String summary = (event.getSummary() != null && !event.getSummary().isBlank())
                    ? event.getSummary()
                    :created.getOrDefault("summary","");
            String date = created.getOrDefault("date","");
            String time = created.getOrDefault("time","");
            String id = created.getOrDefault("id","");
            String guests = created.getOrDefault("guests","");
            String location = created.getOrDefault("location","");
            return new ObjectMapper().writeValueAsString(
                    List.of(Map.of("role","event","summary",summary,"date",date,"time",time,"id",id,"guests",guests,"location",location)));
        } catch (Exception e) {
            return buildFallbackMessage("Could not build event card.");
        }
    }

    public String buildNoEventsFound(String start, String end) {
        try {

            String simpleStart = start.length() >= 10 ? start.substring(0, 10) : start;
            String simpleEnd = end.length() >= 10 ? end.substring(0, 10) : end;

            return new ObjectMapper().writeValueAsString(List.of(Map.of("role", "ai", "content", "📭 No events found between " + simpleStart + " and " + simpleEnd + ".")));
        } catch (Exception e) {
            return buildFallbackMessage("No events found.");
        }
    }


    public String buildEventList(List<Map<String, String>> events, String calendarId) {
        try {
            List<Map<String, String>> responseList = new ArrayList<>();
            for (Map<String, String> ev : events) {
                String summary = ev.getOrDefault("summary","");
                String date = ev.getOrDefault("date","");
                String time = ev.getOrDefault("time","");
                String id = ev.getOrDefault("id","");
                String guests = ev.getOrDefault("guests","");
                String location = ev.getOrDefault("location","");
                responseList.add(Map.of("role","event","summary",summary,"date",date,"time",time,"id",id,"guests",guests,"location",location));
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
            return LocalDate.parse(isoDateTime.substring(0, 10)).format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        } catch (Exception e) {
            return isoDateTime;
        }
    }
}
