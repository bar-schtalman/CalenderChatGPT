package com.handson.CalenderGPT.controller;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.handson.CalenderGPT.context.CalendarContext;
import com.handson.CalenderGPT.model.Event;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final Calendar googleCalendarClient;
    private final CalendarContext calendarContext;

    public EventController(Calendar googleCalendarClient, CalendarContext calendarContext) {
        this.googleCalendarClient = googleCalendarClient;
        this.calendarContext = calendarContext;
    }

    @PostMapping("/create")
    public String createEvent(@RequestBody Event eventRequest) throws Exception {
        String calendarId = calendarContext.getCalendarId();
        if (calendarId == null) throw new IllegalStateException("Calendar ID not found");

        com.google.api.services.calendar.model.Event googleEvent = new com.google.api.services.calendar.model.Event()
                .setSummary(eventRequest.getSummary())
                .setLocation(eventRequest.getLocation())
                .setDescription(eventRequest.getDescription());

        googleEvent.setStart(new EventDateTime().setDateTime(convertToGoogleDateTime(eventRequest.getStart())).setTimeZone("UTC"));
        googleEvent.setEnd(new EventDateTime().setDateTime(convertToGoogleDateTime(eventRequest.getEnd())).setTimeZone("UTC"));

        return googleCalendarClient.events().insert(calendarId, googleEvent).execute().getHtmlLink();
    }

    @PutMapping("/update/{eventId}")
    public String updateEvent(@PathVariable String eventId, @RequestBody Event eventRequest) throws Exception {
        String calendarId = calendarContext.getCalendarId();
        com.google.api.services.calendar.model.Event existingEvent = googleCalendarClient.events().get(calendarId, eventId).execute();

        existingEvent.setSummary(eventRequest.getSummary());
        existingEvent.setLocation(eventRequest.getLocation());
        existingEvent.setDescription(eventRequest.getDescription());
        existingEvent.setStart(new EventDateTime().setDateTime(convertToGoogleDateTime(eventRequest.getStart())).setTimeZone("UTC"));
        existingEvent.setEnd(new EventDateTime().setDateTime(convertToGoogleDateTime(eventRequest.getEnd())).setTimeZone("UTC"));

        return googleCalendarClient.events().update(calendarId, eventId, existingEvent).execute().getHtmlLink();
    }

    @GetMapping("/view")
    public List<Map<String, String>> getEvents(@RequestParam String start, @RequestParam String end) throws Exception {
        String calendarId = calendarContext.getCalendarId();
        List<Map<String, String>> simplifiedEvents = new ArrayList<>();
        Events events = googleCalendarClient.events().list(calendarId)
                .setTimeMin(new DateTime(start))
                .setTimeMax(new DateTime(end))
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        for (com.google.api.services.calendar.model.Event e : events.getItems()) {
            Map<String, String> map = new HashMap<>();
            map.put("id", e.getId());
            map.put("summary", e.getSummary());
            map.put("start", formatDate(e.getStart().getDateTime()));
            map.put("end", formatDate(e.getEnd().getDateTime()));
            map.put("location", Optional.ofNullable(e.getLocation()).orElse(""));
            map.put("description", Optional.ofNullable(e.getDescription()).orElse(""));
            simplifiedEvents.add(map);
        }
        return simplifiedEvents;
    }

    @DeleteMapping("/delete/{eventId}")
    public void deleteEvent(@PathVariable String eventId) throws Exception {
        String calendarId = calendarContext.getCalendarId();
        googleCalendarClient.events().delete(calendarId, eventId).execute();
    }

    private DateTime convertToGoogleDateTime(LocalDateTime ldt) {
        return new DateTime(ldt.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli());
    }

    private String formatDate(DateTime dt) {
        return dt != null ? LocalDateTime.ofInstant(Instant.ofEpochMilli(dt.getValue()), ZoneId.of("UTC"))
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) : "";
    }
}
