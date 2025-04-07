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
import java.util.stream.Collectors;

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

    public Map<String, String> createEvent(String calendarId, Event eventRequest) throws IOException {
        DateTime startDateTime = convertToGoogleDateTime(eventRequest.getStart(), eventRequest.getTimeZone());
        DateTime endDateTime = convertToGoogleDateTime(eventRequest.getEnd(), eventRequest.getTimeZone());

        com.google.api.services.calendar.model.Event googleEvent = new com.google.api.services.calendar.model.Event()
                .setSummary(eventRequest.getSummary())
                .setLocation(eventRequest.getLocation())
                .setDescription(eventRequest.getDescription());

        googleEvent.setStart(new EventDateTime().setDateTime(startDateTime).setTimeZone(eventRequest.getTimeZone()));
        googleEvent.setEnd(new EventDateTime().setDateTime(endDateTime).setTimeZone(eventRequest.getTimeZone()));

        List<String> guestEmails = eventRequest.getGuests();
        if (guestEmails != null && !guestEmails.isEmpty()) {
            List<EventAttendee> attendees = guestEmails.stream()
                    .map(email -> new EventAttendee().setEmail(email))
                    .collect(Collectors.toList());
            googleEvent.setAttendees(attendees);
        }

        com.google.api.services.calendar.model.Event created = googleCalendarClient.events()
                .insert(calendarId, googleEvent)
                .execute();

        return mapEvent(created);
    }

    public String updateEvent(String calendarId, String eventId, Event eventRequest) throws IOException {
        com.google.api.services.calendar.model.Event existingEvent =
                googleCalendarClient.events().get(calendarId, eventId).execute();

        existingEvent.setSummary(eventRequest.getSummary());
        existingEvent.setLocation(eventRequest.getLocation());
        existingEvent.setDescription(eventRequest.getDescription());
        existingEvent.setStart(new EventDateTime().setDateTime(convertToGoogleDateTime(eventRequest.getStart(), eventRequest.getTimeZone())).setTimeZone(eventRequest.getTimeZone()));
        existingEvent.setEnd(new EventDateTime().setDateTime(convertToGoogleDateTime(eventRequest.getEnd(), eventRequest.getTimeZone())).setTimeZone(eventRequest.getTimeZone()));

        List<String> guestEmails = eventRequest.getGuests();
        if (guestEmails != null && !guestEmails.isEmpty()) {
            List<EventAttendee> attendees = guestEmails.stream()
                    .map(email -> new EventAttendee().setEmail(email))
                    .collect(Collectors.toList());
            existingEvent.setAttendees(attendees);
        }

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
                .setFields("items(id,summary,start,end,location,description,attendees)")
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

    private DateTime convertToGoogleDateTime(LocalDateTime localDateTime, String timeZone) {
        ZoneId zone = (timeZone != null && !timeZone.isEmpty()) ? ZoneId.of(timeZone) : ZoneId.systemDefault();
        ZonedDateTime zonedDateTime = localDateTime.atZone(zone);
        return new DateTime(zonedDateTime.toInstant().toEpochMilli());
    }

    private LocalDateTime convertToLocalDateTime(DateTime googleDateTime) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(googleDateTime.getValue()), ZoneId.systemDefault());
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

        if (event.getAttendees() != null) {
            List<String> guestEmails = event.getAttendees().stream()
                    .map(com.google.api.services.calendar.model.EventAttendee::getEmail)
                    .filter(email -> email != null && !email.trim().isEmpty())
                    .collect(Collectors.toList());

            if (!guestEmails.isEmpty()) {
                map.put("guests", String.join(",", guestEmails));
            }
        }

        return map;
    }

    public String addGuests(String calendarId, String eventId, List<String> newGuests) throws IOException {
        com.google.api.services.calendar.model.Event event =
                googleCalendarClient.events().get(calendarId, eventId).execute();

        List<EventAttendee> attendees = event.getAttendees() != null ? new ArrayList<>(event.getAttendees()) : new ArrayList<>();

        for (String guest : newGuests) {
            if (attendees.stream().noneMatch(a -> a.getEmail().equalsIgnoreCase(guest))) {
                attendees.add(new EventAttendee().setEmail(guest));
            }
        }

        event.setAttendees(attendees);
        return googleCalendarClient.events().update(calendarId, eventId, event).execute().getHtmlLink();
    }
}
