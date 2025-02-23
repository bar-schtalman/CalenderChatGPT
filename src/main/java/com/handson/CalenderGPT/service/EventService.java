package com.handson.CalenderGPT.service;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.handson.CalenderGPT.config.GoogleCalendarConfig;
import com.handson.CalenderGPT.model.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EventService {

    private final GoogleCalendarConfig googleCalendarConfig;
    private Calendar googleCalendarClient;

    @Autowired
    public EventService(GoogleCalendarConfig googleCalendarConfig) {
        this.googleCalendarConfig = googleCalendarConfig;
    }

    private Calendar getGoogleCalendarClient() throws Exception {
        if (googleCalendarClient == null) {
            googleCalendarClient = googleCalendarConfig.googleCalendarClient();
        }
        return googleCalendarClient;
    }

    public String createEvent(String calendarId, Event eventRequest) throws Exception {
        DateTime startDateTime = convertToGoogleDateTime(eventRequest.getStart(), "UTC");
        DateTime endDateTime = convertToGoogleDateTime(eventRequest.getEnd(), "UTC");

        com.google.api.services.calendar.model.Event googleEvent = new com.google.api.services.calendar.model.Event()
                .setSummary(eventRequest.getSummary())
                .setLocation(eventRequest.getLocation())
                .setDescription(eventRequest.getDescription());

        googleEvent.setStart(new EventDateTime().setDateTime(startDateTime).setTimeZone("UTC"));
        googleEvent.setEnd(new EventDateTime().setDateTime(endDateTime).setTimeZone("UTC"));

        com.google.api.services.calendar.model.Event createdEvent = getGoogleCalendarClient()
                .events()
                .insert(calendarId, googleEvent)
                .execute();

        return createdEvent.getHtmlLink();
    }

    public String updateEvent(String calendarId, String eventId, Event eventRequest) throws Exception {
        com.google.api.services.calendar.model.Event existingEvent = getGoogleCalendarClient()
                .events()
                .get(calendarId, eventId)
                .execute();

        existingEvent.setSummary(eventRequest.getSummary());
        existingEvent.setLocation(eventRequest.getLocation());
        existingEvent.setDescription(eventRequest.getDescription());

        existingEvent.setStart(new EventDateTime().setDateTime(convertToGoogleDateTime(eventRequest.getStart(), "UTC")).setTimeZone("UTC"));
        existingEvent.setEnd(new EventDateTime().setDateTime(convertToGoogleDateTime(eventRequest.getEnd(), "UTC")).setTimeZone("UTC"));

        com.google.api.services.calendar.model.Event updatedEvent = getGoogleCalendarClient()
                .events()
                .update(calendarId, eventId, existingEvent)
                .execute();

        return updatedEvent.getHtmlLink();
    }

    public List<Map<String, String>> getEventsInDateRange(String calendarId, String startDate, String endDate) throws Exception {
        DateTime timeMin = new DateTime(startDate);
        DateTime timeMax = new DateTime(endDate);

        Events events = getGoogleCalendarClient()
                .events()
                .list(calendarId)
                .setTimeMin(timeMin)
                .setTimeMax(timeMax)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        List<Map<String, String>> simplifiedEvents = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

        for (com.google.api.services.calendar.model.Event event : events.getItems()) {
            Map<String, String> eventDetails = new HashMap<>();
            eventDetails.put("id", event.getId());
            eventDetails.put("summary", event.getSummary());
            eventDetails.put("start", event.getStart() != null && event.getStart().getDateTime() != null
                    ? convertToLocalDateTime(event.getStart().getDateTime()).format(formatter) : "");
            eventDetails.put("end", event.getEnd() != null && event.getEnd().getDateTime() != null
                    ? convertToLocalDateTime(event.getEnd().getDateTime()).format(formatter) : "");
            eventDetails.put("location", Optional.ofNullable(event.getLocation()).orElse(""));
            eventDetails.put("description", Optional.ofNullable(event.getDescription()).orElse(""));
            simplifiedEvents.add(eventDetails);
        }

        return simplifiedEvents;
    }

    public void deleteEvent(String calendarId, String eventId) throws Exception {
        getGoogleCalendarClient().events().delete(calendarId, eventId).execute();
    }

    private DateTime convertToGoogleDateTime(LocalDateTime localDateTime, String timeZoneId) {
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of(timeZoneId));
        return new DateTime(zonedDateTime.toInstant().toEpochMilli());
    }

    private LocalDateTime convertToLocalDateTime(DateTime googleDateTime) {
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(googleDateTime.getValue()), ZoneId.of("UTC"));
    }


}
