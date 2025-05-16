package com.handson.CalenderGPT.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.handson.CalenderGPT.google.calendar.GoogleCalendarProvider;
import com.handson.CalenderGPT.model.Event;
import com.handson.CalenderGPT.model.EventHistory;
import com.handson.CalenderGPT.model.User;
import com.handson.CalenderGPT.repository.EventHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.handson.CalenderGPT.util.DateUtils.*;

@Service
@RequiredArgsConstructor
public class EventService {

    private final GoogleCalendarProvider googleCalendarProvider;
    private final EventHistoryRepository eventHistoryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Calendar clientFor(User user) throws IOException, GeneralSecurityException {
        return googleCalendarProvider.getCalendarClient(user);
    }

    private void logHistory(User user, String calendarId, String eventId, String action, String oldData, String newData) {

        eventHistoryRepository.save(EventHistory.builder().user(user).calendarId(calendarId).eventId(eventId).action(action).oldData(oldData).newData(newData).build());
    }


    public Map<String, String> createEvent(String calendarId, Event eventRequest, User user) throws IOException, GeneralSecurityException {
        Calendar googleCalendarClient = clientFor(user);

        Map<String, String> newEvent = createEventInternal(calendarId, eventRequest, googleCalendarClient);

        logHistory(user, calendarId, newEvent.get("id"), "CREATE", null, objectMapper.writeValueAsString(newEvent));

        return newEvent;
    }

    public String updateEvent(String calendarId, String eventId, Event eventRequest, User user) throws IOException, GeneralSecurityException {
        Calendar googleCalendarClient = clientFor(user);
        com.google.api.services.calendar.model.Event existingEvent = googleCalendarClient.events().get(calendarId, eventId).execute();

        String oldData = objectMapper.writeValueAsString(mapEvent(existingEvent));

        existingEvent.setSummary(eventRequest.getSummary());
        existingEvent.setLocation(eventRequest.getLocation());
        existingEvent.setDescription(eventRequest.getDescription());
        existingEvent.setStart(new EventDateTime().setDateTime(toGoogle(eventRequest.getStart(), eventRequest.getTimeZone())));
        existingEvent.setEnd(new EventDateTime().setDateTime(toGoogle(eventRequest.getEnd(), eventRequest.getTimeZone())));

        if (eventRequest.getGuests() != null && !eventRequest.getGuests().isEmpty()) {
            List<EventAttendee> attendees = eventRequest.getGuests().stream().map(email -> new EventAttendee().setEmail(email)).collect(Collectors.toList());
            existingEvent.setAttendees(attendees);
        }

        com.google.api.services.calendar.model.Event updated = googleCalendarClient.events().update(calendarId, eventId, existingEvent).execute();
        String newData = objectMapper.writeValueAsString(mapEvent(updated));
        logHistory(user, calendarId, eventId, "UPDATE", oldData, newData);

        return updated.getHtmlLink();
    }

    public void deleteEvent(String calendarId, String eventId, User user) throws IOException, GeneralSecurityException {
        Calendar googleCalendarClient = clientFor(user);
        com.google.api.services.calendar.model.Event existing = googleCalendarClient.events().get(calendarId, eventId).execute();
        String oldData = objectMapper.writeValueAsString(mapEvent(existing));

        googleCalendarClient.events().delete(calendarId, eventId).execute();
        logHistory(user, calendarId, eventId, "DELETE", oldData, null);


    }

    private Map<String, String> createEventInternal(String calendarId, Event eventRequest, Calendar googleCalendarClient) throws IOException {
        DateTime startDateTime = toGoogle(eventRequest.getStart(), eventRequest.getTimeZone());
        DateTime endDateTime = toGoogle(eventRequest.getEnd(), eventRequest.getTimeZone());

        com.google.api.services.calendar.model.Event googleEvent = new com.google.api.services.calendar.model.Event().setSummary(eventRequest.getSummary()).setLocation(eventRequest.getLocation()).setDescription(eventRequest.getDescription());

        googleEvent.setStart(new EventDateTime().setDateTime(startDateTime).setTimeZone(eventRequest.getTimeZone()));
        googleEvent.setEnd(new EventDateTime().setDateTime(endDateTime).setTimeZone(eventRequest.getTimeZone()));

        if (eventRequest.getGuests() != null && !eventRequest.getGuests().isEmpty()) {
            List<EventAttendee> attendees = eventRequest.getGuests().stream().map(email -> new EventAttendee().setEmail(email)).collect(Collectors.toList());
            googleEvent.setAttendees(attendees);
        }

        com.google.api.services.calendar.model.Event created = googleCalendarClient.events().insert(calendarId, googleEvent).execute();

        return mapEvent(created);
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
            LocalDateTime startLdt = fromGoogle(startRaw);
            LocalDateTime endLdt = fromGoogle(endRaw);

            map.put("date", startLdt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            map.put("time", startLdt.format(DateTimeFormatter.ofPattern("HH:mm")) + " - " + endLdt.format(DateTimeFormatter.ofPattern("HH:mm")));
        } else {
            map.put("date", "?");
            map.put("time", "N/A - ?");
        }

        map.put("start", format(startDt));
        map.put("end", format(endDt));

        if (event.getAttendees() != null) {
            String guests = event.getAttendees().stream().map(EventAttendee::getEmail).filter(Objects::nonNull).collect(Collectors.joining(","));
            map.put("guests", guests);
        }

        return map;
    }

    public Map<String, String> addGuests(String calendarId, String eventId, List<String> guests, User user) throws IOException, GeneralSecurityException {
        Calendar calendar = clientFor(user);

        com.google.api.services.calendar.model.Event event = calendar.events().get(calendarId, eventId).execute();

        String oldData = objectMapper.writeValueAsString(mapEvent(event));

        List<EventAttendee> existingAttendees = Optional.ofNullable(event.getAttendees()).orElse(new ArrayList<>());
        Set<String> existingEmails = existingAttendees.stream().map(EventAttendee::getEmail).collect(Collectors.toSet());

        for (String email : guests) {
            if (!existingEmails.contains(email)) {
                existingAttendees.add(new EventAttendee().setEmail(email));
            }
        }

        event.setAttendees(existingAttendees);
        com.google.api.services.calendar.model.Event updatedEvent = calendar.events().update(calendarId, eventId, event).execute();

        String newData = objectMapper.writeValueAsString(mapEvent(updatedEvent));
        logHistory(user, calendarId, eventId, "ADD_GUESTS", oldData, newData);


        return mapEvent(updatedEvent);
    }

    public Map<String, String> removeGuests(String calendarId, String eventId, List<String> emailsToRemove, User user) throws IOException, GeneralSecurityException {
        Calendar calendar = clientFor(user);
        com.google.api.services.calendar.model.Event event = calendar.events().get(calendarId, eventId).execute();

        String oldData = objectMapper.writeValueAsString(mapEvent(event));

        List<EventAttendee> updatedAttendees = Optional.ofNullable(event.getAttendees()).orElse(new ArrayList<>()).stream().filter(attendee -> !emailsToRemove.contains(attendee.getEmail())).collect(Collectors.toList());

        event.setAttendees(updatedAttendees);
        com.google.api.services.calendar.model.Event updatedEvent = calendar.events().update(calendarId, eventId, event).execute();

        String newData = objectMapper.writeValueAsString(mapEvent(updatedEvent));
        logHistory(user, calendarId, eventId, "REMOVE_GUESTS", oldData, newData);

        return mapEvent(updatedEvent);
    }

    public List<Map<String, String>> getEventsInDateRange(String calendarId, String startDate, String endDate, User user) throws IOException, GeneralSecurityException {
        Calendar googleCalendarClient = clientFor(user);
        Events events = googleCalendarClient.events().list(calendarId).setTimeMin(new DateTime(startDate)).setTimeMax(new DateTime(endDate)).setOrderBy("startTime").setSingleEvents(true).setFields("items(id,summary,start,end,location,description,attendees)").execute();

        return events.getItems().stream().map(this::mapEvent).collect(Collectors.toList());
    }

    public Map<String, String> getEventById(String calendarId, String eventId, User user) throws IOException, GeneralSecurityException {
        Calendar googleCalendarClient = clientFor(user);
        com.google.api.services.calendar.model.Event event = googleCalendarClient.events().get(calendarId, eventId).execute();

        return mapEvent(event);
    }


}
