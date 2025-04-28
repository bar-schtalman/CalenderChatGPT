package com.handson.CalenderGPT.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.handson.CalenderGPT.model.Event;
import com.handson.CalenderGPT.model.EventHistory;
import com.handson.CalenderGPT.model.User;
import com.handson.CalenderGPT.provider.GoogleCalendarProvider;
import com.handson.CalenderGPT.repository.EventHistoryRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final GoogleCalendarProvider googleCalendarProvider;
    private final EventHistoryRepository eventHistoryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, String> createEvent(String calendarId, Event eventRequest, User user) throws IOException, GeneralSecurityException {
        Calendar googleCalendarClient = googleCalendarProvider.getCalendarClient(user);
        Map<String, String> newEvent = createEventInternal(calendarId, eventRequest, googleCalendarClient);

        eventHistoryRepository.save(EventHistory.builder()
                .user(user)
                .calendarId(calendarId)
                .eventId(newEvent.get("id"))
                .action("CREATE")
                .newData(objectMapper.writeValueAsString(newEvent))
                .build());


        return newEvent;
    }

    public String updateEvent(String calendarId, String eventId, Event eventRequest, User user) throws IOException, GeneralSecurityException {
        Calendar googleCalendarClient = googleCalendarProvider.getCalendarClient(user);
        com.google.api.services.calendar.model.Event existingEvent =
                googleCalendarClient.events().get(calendarId, eventId).execute();

        String oldData = objectMapper.writeValueAsString(mapEvent(existingEvent));

        existingEvent.setSummary(eventRequest.getSummary());
        existingEvent.setLocation(eventRequest.getLocation());
        existingEvent.setDescription(eventRequest.getDescription());
        existingEvent.setStart(new EventDateTime().setDateTime(convertToGoogleDateTime(eventRequest.getStart(), eventRequest.getTimeZone())));
        existingEvent.setEnd(new EventDateTime().setDateTime(convertToGoogleDateTime(eventRequest.getEnd(), eventRequest.getTimeZone())));

        if (eventRequest.getGuests() != null && !eventRequest.getGuests().isEmpty()) {
            List<EventAttendee> attendees = eventRequest.getGuests().stream()
                    .map(email -> new EventAttendee().setEmail(email))
                    .collect(Collectors.toList());
            existingEvent.setAttendees(attendees);
        }

        com.google.api.services.calendar.model.Event updated = googleCalendarClient.events().update(calendarId, eventId, existingEvent).execute();
        String newData = objectMapper.writeValueAsString(mapEvent(updated));

        eventHistoryRepository.save(EventHistory.builder()
                .user(user)
                .calendarId(calendarId)
                .eventId(eventId)
                .action("UPDATE")
                .oldData(oldData)
                .newData(newData)
                .build());


        return updated.getHtmlLink();
    }

    public void deleteEvent(String calendarId, String eventId, User user) throws IOException, GeneralSecurityException {
        Calendar googleCalendarClient = googleCalendarProvider.getCalendarClient(user);
        com.google.api.services.calendar.model.Event existing = googleCalendarClient.events().get(calendarId, eventId).execute();
        String oldData = objectMapper.writeValueAsString(mapEvent(existing));

        googleCalendarClient.events().delete(calendarId, eventId).execute();

        eventHistoryRepository.save(EventHistory.builder()
                .user(user)
                .calendarId(calendarId)
                .eventId(eventId)
                .action("DELETE")
                .oldData(oldData)
                .build());


    }

    private Map<String, String> createEventInternal(String calendarId, Event eventRequest, Calendar googleCalendarClient) throws IOException {
        DateTime startDateTime = convertToGoogleDateTime(eventRequest.getStart(), eventRequest.getTimeZone());
        DateTime endDateTime = convertToGoogleDateTime(eventRequest.getEnd(), eventRequest.getTimeZone());

        com.google.api.services.calendar.model.Event googleEvent = new com.google.api.services.calendar.model.Event()
                .setSummary(eventRequest.getSummary())
                .setLocation(eventRequest.getLocation())
                .setDescription(eventRequest.getDescription());

        googleEvent.setStart(new EventDateTime().setDateTime(startDateTime).setTimeZone(eventRequest.getTimeZone()));
        googleEvent.setEnd(new EventDateTime().setDateTime(endDateTime).setTimeZone(eventRequest.getTimeZone()));

        if (eventRequest.getGuests() != null && !eventRequest.getGuests().isEmpty()) {
            List<EventAttendee> attendees = eventRequest.getGuests().stream()
                    .map(email -> new EventAttendee().setEmail(email))
                    .collect(Collectors.toList());
            googleEvent.setAttendees(attendees);
        }

        com.google.api.services.calendar.model.Event created = googleCalendarClient.events()
                .insert(calendarId, googleEvent)
                .execute();

        return mapEvent(created);
    }

    private DateTime convertToGoogleDateTime(LocalDateTime localDateTime, String timeZone) {
        ZoneId zone = (timeZone != null && !timeZone.isEmpty()) ? ZoneId.of(timeZone) : ZoneId.systemDefault();
        ZonedDateTime zonedDateTime = localDateTime.atZone(zone);
        return new DateTime(zonedDateTime.toInstant().toEpochMilli());
    }

    private LocalDateTime convertToLocalDateTime(DateTime googleDateTime) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(googleDateTime.getValue()), ZoneId.systemDefault());
    }

    private String formatDate(EventDateTime eventDateTime) {
        if (eventDateTime == null) return "N/A";
        DateTime dateTime = eventDateTime.getDateTime() != null
                ? eventDateTime.getDateTime()
                : eventDateTime.getDate();
        return (dateTime != null)
                ? convertToLocalDateTime(dateTime).format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
                : "N/A";
    }

    private Map<String, String> mapEvent(com.google.api.services.calendar.model.Event event) {
        Map<String, String> map = new HashMap<>();
        map.put("id", event.getId());
        map.put("summary", event.getSummary());
        map.put("location", Optional.ofNullable(event.getLocation()).orElse(""));
        map.put("description", Optional.ofNullable(event.getDescription()).orElse(""));

        EventDateTime startDt = event.getStart();
        EventDateTime endDt = event.getEnd();

        DateTime startRaw = (startDt != null && startDt.getDateTime() != null) ? startDt.getDateTime() : startDt.getDate();
        DateTime endRaw = (endDt != null && endDt.getDateTime() != null) ? endDt.getDateTime() : endDt.getDate();

        if (startRaw != null && endRaw != null) {
            LocalDateTime startLdt = convertToLocalDateTime(startRaw);
            LocalDateTime endLdt = convertToLocalDateTime(endRaw);

            map.put("date", startLdt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            map.put("time", startLdt.format(DateTimeFormatter.ofPattern("HH:mm")) +
                    " - " + endLdt.format(DateTimeFormatter.ofPattern("HH:mm")));
        } else {
            map.put("date", "?");
            map.put("time", "N/A - ?");
        }

        map.put("start", formatDate(startDt));
        map.put("end", formatDate(endDt));

        if (event.getAttendees() != null) {
            String guests = event.getAttendees().stream()
                    .map(EventAttendee::getEmail)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(","));
            map.put("guests", guests);
        }

        return map;
    }

    public Map<String, String> addGuests(String calendarId, String eventId, List<String> guests, User user) throws IOException, GeneralSecurityException {
        Calendar calendar = googleCalendarProvider.getCalendarClient(user);
        com.google.api.services.calendar.model.Event event = calendar.events().get(calendarId, eventId).execute();

        String oldData = objectMapper.writeValueAsString(mapEvent(event));

        List<EventAttendee> existingAttendees = Optional.ofNullable(event.getAttendees()).orElse(new ArrayList<>());
        Set<String> existingEmails = existingAttendees.stream()
                .map(EventAttendee::getEmail)
                .collect(Collectors.toSet());

        for (String email : guests) {
            if (!existingEmails.contains(email)) {
                existingAttendees.add(new EventAttendee().setEmail(email));
            }
        }

        event.setAttendees(existingAttendees);
        com.google.api.services.calendar.model.Event updatedEvent = calendar.events().update(calendarId, eventId, event).execute();

        String newData = objectMapper.writeValueAsString(mapEvent(updatedEvent));
        eventHistoryRepository.save(EventHistory.builder()
                .user(user)
                .calendarId(calendarId)
                .eventId(eventId)
                .action("ADD_GUESTS")
                .oldData(oldData)
                .newData(newData)
                .build());


        return mapEvent(updatedEvent);
    }

    public Map<String, String> removeGuests(String calendarId, String eventId, List<String> emailsToRemove, User user) throws IOException, GeneralSecurityException {
        Calendar calendar = googleCalendarProvider.getCalendarClient(user);
        com.google.api.services.calendar.model.Event event = calendar.events().get(calendarId, eventId).execute();

        String oldData = objectMapper.writeValueAsString(mapEvent(event));

        List<EventAttendee> updatedAttendees = Optional.ofNullable(event.getAttendees()).orElse(new ArrayList<>())
                .stream()
                .filter(attendee -> !emailsToRemove.contains(attendee.getEmail()))
                .collect(Collectors.toList());

        event.setAttendees(updatedAttendees);
        com.google.api.services.calendar.model.Event updatedEvent = calendar.events().update(calendarId, eventId, event).execute();

        String newData = objectMapper.writeValueAsString(mapEvent(updatedEvent));
        eventHistoryRepository.save(EventHistory.builder()
                .user(user)
                .calendarId(calendarId)
                .eventId(eventId)
                .action("REMOVE_GUESTS")
                .oldData(oldData)
                .newData(newData)
                .build());


        return mapEvent(updatedEvent);
    }

    public List<Map<String, String>> getEventsInDateRange(String calendarId, String startDate, String endDate, User user) throws IOException, GeneralSecurityException {
        Calendar googleCalendarClient = googleCalendarProvider.getCalendarClient(user);
        Events events = googleCalendarClient.events().list(calendarId)
                .setTimeMin(new DateTime(startDate))
                .setTimeMax(new DateTime(endDate))
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .setFields("items(id,summary,start,end,location,description,attendees)")
                .execute();

        return events.getItems().stream().map(this::mapEvent).collect(Collectors.toList());
    }

    public Map<String, String> getEventById(String calendarId, String eventId, User user) throws IOException, GeneralSecurityException {
        Calendar googleCalendarClient = googleCalendarProvider.getCalendarClient(user);
        com.google.api.services.calendar.model.Event event =
                googleCalendarClient.events().get(calendarId, eventId).execute();

        return mapEvent(event);
    }


}
