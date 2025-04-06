package com.handson.CalenderGPT.service;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.handson.CalenderGPT.model.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EventService {

    private Calendar googleCalendarClient;

    @Autowired
    public EventService(Calendar googleCalendarClient) {
        this.googleCalendarClient = googleCalendarClient;
    }

    public void setCalendar(Calendar calendar) {
        this.googleCalendarClient = calendar;
    }

    public String createEvent(String calendarId, Event eventRequest) throws IOException {
        DateTime startDateTime = convertToGoogleDateTime(eventRequest.getStart());
        DateTime endDateTime = convertToGoogleDateTime(eventRequest.getEnd());

        com.google.api.services.calendar.model.Event googleEvent = new com.google.api.services.calendar.model.Event()
                .setSummary(eventRequest.getSummary())
                .setLocation(eventRequest.getLocation())
                .setDescription(eventRequest.getDescription());

        googleEvent.setStart(new EventDateTime().setDateTime(startDateTime).setTimeZone("UTC"));
        googleEvent.setEnd(new EventDateTime().setDateTime(endDateTime).setTimeZone("UTC"));

        return googleCalendarClient.events().insert(calendarId, googleEvent).execute().getHtmlLink();
    }

    public String updateEvent(String calendarId, String eventId, Event eventRequest) throws IOException {
        com.google.api.services.calendar.model.Event existingEvent =
                googleCalendarClient.events().get(calendarId, eventId).execute();

        existingEvent.setSummary(eventRequest.getSummary());
        existingEvent.setLocation(eventRequest.getLocation());
        existingEvent.setDescription(eventRequest.getDescription());

        existingEvent.setStart(new EventDateTime().setDateTime(convertToGoogleDateTime(eventRequest.getStart())).setTimeZone("UTC"));
        existingEvent.setEnd(new EventDateTime().setDateTime(convertToGoogleDateTime(eventRequest.getEnd())).setTimeZone("UTC"));

        return googleCalendarClient.events().update(calendarId, eventId, existingEvent).execute().getHtmlLink();
    }

    public List<Map<String, String>> getEventsInDateRange(String calendarId, String startDate, String endDate) throws IOException {
        DateTime timeMin = new DateTime(startDate);
        DateTime timeMax = new DateTime(endDate);

        Events events = googleCalendarClient.events().list(calendarId)
                .setTimeMin(timeMin)
                .setTimeMax(timeMax)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        List<Map<String, String>> simplifiedEvents = new ArrayList<>();
        for (com.google.api.services.calendar.model.Event event : events.getItems()) {
            simplifiedEvents.add(mapEvent(event));
        }

        return simplifiedEvents;
    }

    public Map<String, String> getEventById(String calendarId, String eventId) throws IOException {
        com.google.api.services.calendar.model.Event event =
                googleCalendarClient.events().get(calendarId, eventId).execute();

        return mapEvent(event);
    }

    public void deleteEvent(String calendarId, String eventId) throws IOException {
        googleCalendarClient.events().delete(calendarId, eventId).execute();
    }

    private DateTime convertToGoogleDateTime(LocalDateTime localDateTime) {
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of("UTC"));
        return new DateTime(zonedDateTime.toInstant().toEpochMilli());
    }

    private LocalDateTime convertToLocalDateTime(DateTime googleDateTime) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(googleDateTime.getValue()), ZoneId.of("UTC"));
    }

    private String formatDate(DateTime googleDateTime) {
        return (googleDateTime != null)
                ? convertToLocalDateTime(googleDateTime).format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
                : "";
    }

    private Map<String, String> mapEvent(com.google.api.services.calendar.model.Event event) {
        Map<String, String> map = new HashMap<>();
        map.put("id", event.getId());
        map.put("summary", event.getSummary());
        map.put("start", formatDate(event.getStart().getDateTime()));
        map.put("end", formatDate(event.getEnd().getDateTime()));
        map.put("location", Optional.ofNullable(event.getLocation()).orElse(""));
        map.put("description", Optional.ofNullable(event.getDescription()).orElse(""));
        return map;
    }
}
