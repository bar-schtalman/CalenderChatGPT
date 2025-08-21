package com.handson.CalenderGPT.service;
import java.time.*;

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
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.handson.CalenderGPT.util.DateUtils.*;

@Service
@RequiredArgsConstructor
public class EventService {

    private final GoogleCalendarProvider googleCalendarProvider;
    private final EventHistoryRepository eventHistoryRepository;

    // עדיף לקבל ObjectMapper כ-bean מנוהל (thread-safe בשימוש ברירת מחדל)
    private final ObjectMapper objectMapper;


    private static final String DEFAULT_TZ = "Asia/Jerusalem";

    private static String tzOrDefault(String tz) {
        return (tz == null || tz.isBlank()) ? DEFAULT_TZ : tz;
    }

    /**
     * המרה מדויקת של LocalDateTime לאובייקט Google DateTime לפי אזור זמן נתון.
     */
    private static DateTime toGoogleStrict(LocalDateTime ldt, String tz) {
        ZoneId zone = ZoneId.of(tzOrDefault(tz));
        ZonedDateTime zdt = ldt.atZone(zone);
        // DateTime: epochMillis + offsetMinutes
        return new DateTime(zdt.toInstant().toEpochMilli(), zdt.getOffset().getTotalSeconds() / 60);
    }

    /**
     * המרת Google DateTime בחזרה ל-LocalDateTime באזור זמן נתון להצגה (כרטיס אירוע).
     */
    private static LocalDateTime fromGoogleInZone(DateTime dt, String tz) {
        ZoneId zone = ZoneId.of(tzOrDefault(tz));
        return Instant.ofEpochMilli(dt.getValue()).atZone(zone).toLocalDateTime();
    }

    private static String effectiveCalendarId(String calendarId) {
        return (calendarId == null || calendarId.isBlank()) ? "primary" : calendarId;
    }

    private Calendar clientFor(User user) throws IOException, GeneralSecurityException {
        return googleCalendarProvider.getCalendarClient(user);
    }

    private void logHistory(User user, String calendarId, String eventId, String action, String oldData, String newData) {
        eventHistoryRepository.save(
                EventHistory.builder()
                        .user(user)
                        .calendarId(calendarId)
                        .eventId(eventId)
                        .action(action)
                        .oldData(oldData)
                        .newData(newData)
                        .build()
        );
    }

    @Transactional
    public Map<String, String> createEvent(String calendarId, Event eventRequest, User user)
            throws IOException, GeneralSecurityException {

        final String calId = effectiveCalendarId(calendarId);
        Calendar googleCalendarClient = clientFor(user);

        Map<String, String> newEvent = createEventInternal(calId, eventRequest, googleCalendarClient);

        logHistory(user, calId, newEvent.get("id"), "CREATE", null, objectMapper.writeValueAsString(newEvent));
        return newEvent;
    }

    @Transactional
    public String updateEvent(String calendarId, String eventId, Event eventRequest, User user)
            throws IOException, GeneralSecurityException {

        final String calId = effectiveCalendarId(calendarId);
        Calendar googleCalendarClient = clientFor(user);

        com.google.api.services.calendar.model.Event existingEvent =
                googleCalendarClient.events().get(calId, eventId).execute();

        String oldData = objectMapper.writeValueAsString(mapEvent(existingEvent));

        // שדות בסיס
        existingEvent.setSummary(eventRequest.getSummary());
        existingEvent.setLocation(eventRequest.getLocation());
        existingEvent.setDescription(eventRequest.getDescription());

        // זמן + אזור זמן (שומרים TZ גם ב-start וגם ב-end)
// זמן + אזור זמן (שומרים TZ גם ב-start וגם ב-end)
        String tz = tzOrDefault(eventRequest.getTimeZone());
        existingEvent.setStart(new EventDateTime()
                .setDateTime(toGoogleStrict(eventRequest.getStart(), tz))
                .setTimeZone(tz));
        existingEvent.setEnd(new EventDateTime()
                .setDateTime(toGoogleStrict(eventRequest.getEnd(), tz))
                .setTimeZone(tz));


        // אורחים: אם הרשימה לא ריקה נעדכן; אם null נשאיר קיימים; אם רשימה ריקה—ננקה
        if (eventRequest.getGuests() != null) {
            if (eventRequest.getGuests().isEmpty()) {
                existingEvent.setAttendees(new ArrayList<>());
            } else {
                List<EventAttendee> attendees = eventRequest.getGuests().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .map(email -> new EventAttendee().setEmail(email))
                        .collect(Collectors.toList());
                existingEvent.setAttendees(attendees);
            }
        }

        com.google.api.services.calendar.model.Event updated =
                googleCalendarClient.events()
                        .update(calId, eventId, existingEvent)
                        // .setSendUpdates("none") // אפשר לשקול "all" אם חשוב לשלוח מיילים לאורחים
                        .execute();

        String newData = objectMapper.writeValueAsString(mapEvent(updated));
        logHistory(user, calId, eventId, "UPDATE", oldData, newData);

        return updated.getHtmlLink();
    }

    @Transactional
    public void deleteEvent(String calendarId, String eventId, User user)
            throws IOException, GeneralSecurityException {

        final String calId = effectiveCalendarId(calendarId);
        Calendar googleCalendarClient = clientFor(user);

        com.google.api.services.calendar.model.Event existing =
                googleCalendarClient.events().get(calId, eventId).execute();

        String oldData = objectMapper.writeValueAsString(mapEvent(existing));

        googleCalendarClient.events().delete(calId, eventId)
                // .setSendUpdates("none")
                .execute();

        logHistory(user, calId, eventId, "DELETE", oldData, null);
    }

    private Map<String, String> createEventInternal(String calendarId,
                                                    Event eventRequest,
                                                    Calendar googleCalendarClient) throws IOException {

        final String calId = effectiveCalendarId(calendarId);
        String tz = tzOrDefault(eventRequest.getTimeZone());
        DateTime startDateTime = toGoogleStrict(eventRequest.getStart(), tz);
        DateTime endDateTime   = toGoogleStrict(eventRequest.getEnd(), tz);

        com.google.api.services.calendar.model.Event googleEvent =
                new com.google.api.services.calendar.model.Event()
                        .setSummary(eventRequest.getSummary())
                        .setLocation(eventRequest.getLocation())
                        .setDescription(eventRequest.getDescription());

        googleEvent.setStart(new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone(tz));
        googleEvent.setEnd(new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone(tz));


        if (eventRequest.getGuests() != null && !eventRequest.getGuests().isEmpty()) {
            List<EventAttendee> attendees = eventRequest.getGuests().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .map(email -> new EventAttendee().setEmail(email))
                    .collect(Collectors.toList());
            googleEvent.setAttendees(attendees);
        }

        com.google.api.services.calendar.model.Event created =
                googleCalendarClient.events()
                        .insert(calId, googleEvent)
                        // .setSendUpdates("none")
                        .execute();

        return mapEvent(created);
    }

    private Map<String, String> mapEvent(com.google.api.services.calendar.model.Event event) {
        Map<String, String> map = new HashMap<>();
        map.put("id", event.getId());
        map.put("summary", event.getSummary());
        map.put("location", Optional.ofNullable(event.getLocation()).orElse(""));
        map.put("description", Optional.ofNullable(event.getDescription()).orElse(""));

        EventDateTime startDt = event.getStart();
        EventDateTime endDt   = event.getEnd();

// קח את ה-TZ מהאירוע אם נשלח (Google יחזיר אותו אם הגדרנו אותו ביצירה/עדכון)
        String tz = DEFAULT_TZ;
        if (startDt != null && startDt.getTimeZone() != null && !startDt.getTimeZone().isBlank()) {
            tz = startDt.getTimeZone();
        }

        DateTime startRaw = (startDt != null && startDt.getDateTime() != null)
                ? startDt.getDateTime()
                : (startDt != null ? startDt.getDate() : null);
        DateTime endRaw = (endDt != null && endDt.getDateTime() != null)
                ? endDt.getDateTime()
                : (endDt != null ? endDt.getDate() : null);

        if (startRaw != null && endRaw != null) {
            LocalDateTime startLdt = fromGoogleInZone(startRaw, tz);
            LocalDateTime endLdt   = fromGoogleInZone(endRaw, tz);

            map.put("date", startLdt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            map.put("time",
                    startLdt.format(DateTimeFormatter.ofPattern("HH:mm")) +
                            " - " +
                            endLdt.format(DateTimeFormatter.ofPattern("HH:mm")));
        } else {
            map.put("date", "?");
            map.put("time", "N/A - ?");
        }

        map.put("start", format(startDt));
        map.put("end", format(endDt));

        if (event.getAttendees() != null) {
            String guests = event.getAttendees().stream()
                    .map(EventAttendee::getEmail)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.joining(","));
            map.put("guests", guests);
        }

        return map;
    }

    @Transactional
    public Map<String, String> addGuests(String calendarId, String eventId, List<String> guests, User user)
            throws IOException, GeneralSecurityException {

        final String calId = effectiveCalendarId(calendarId);
        Calendar calendar = clientFor(user);

        com.google.api.services.calendar.model.Event event =
                calendar.events().get(calId, eventId).execute();

        String oldData = objectMapper.writeValueAsString(mapEvent(event));

        List<EventAttendee> existingAttendees =
                new ArrayList<>(Optional.ofNullable(event.getAttendees()).orElse(new ArrayList<>()));

        // set למניעת כפילויות (case-insensitive)
        Set<String> existingEmails = existingAttendees.stream()
                .map(EventAttendee::getEmail)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        for (String email : guests) {
            if (email == null) continue;
            String normalized = email.trim();
            if (normalized.isEmpty()) continue;
            if (!existingEmails.contains(normalized.toLowerCase())) {
                existingAttendees.add(new EventAttendee().setEmail(normalized));
                existingEmails.add(normalized.toLowerCase());
            }
        }

        event.setAttendees(existingAttendees);

        com.google.api.services.calendar.model.Event updatedEvent =
                calendar.events()
                        .update(calId, eventId, event)
                        // .setSendUpdates("all") // אם תרצה לשלוח מיילי עדכון
                        .execute();

        String newData = objectMapper.writeValueAsString(mapEvent(updatedEvent));
        logHistory(user, calId, eventId, "ADD_GUESTS", oldData, newData);

        return mapEvent(updatedEvent);
    }

    @Transactional
    public Map<String, String> removeGuests(String calendarId, String eventId, List<String> emailsToRemove, User user)
            throws IOException, GeneralSecurityException {

        final String calId = effectiveCalendarId(calendarId);
        Calendar calendar = clientFor(user);

        com.google.api.services.calendar.model.Event event =
                calendar.events().get(calId, eventId).execute();

        String oldData = objectMapper.writeValueAsString(mapEvent(event));

        Set<String> toRemove = Optional.ofNullable(emailsToRemove).orElse(List.of()).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<EventAttendee> updatedAttendees = Optional.ofNullable(event.getAttendees())
                .orElse(new ArrayList<>())
                .stream()
                .filter(a -> {
                    String e = (a.getEmail() == null) ? "" : a.getEmail().trim().toLowerCase();
                    return !toRemove.contains(e);
                })
                .collect(Collectors.toList());

        event.setAttendees(updatedAttendees);

        com.google.api.services.calendar.model.Event updatedEvent =
                calendar.events()
                        .update(calId, eventId, event)
                        // .setSendUpdates("all")
                        .execute();

        String newData = objectMapper.writeValueAsString(mapEvent(updatedEvent));
        logHistory(user, calId, eventId, "REMOVE_GUESTS", oldData, newData);

        return mapEvent(updatedEvent);
    }

    public List<Map<String, String>> getEventsInDateRange(String calendarId, String startDate, String endDate, User user)
            throws IOException, GeneralSecurityException {

        final String calId = effectiveCalendarId(calendarId);
        Calendar googleCalendarClient = clientFor(user);

// בחר TZ להצגה/חישוב (אפשר גם לשמור אצל המשתמש אם יש לך שדה עתידי)
        ZoneId zone = ZoneId.of(DEFAULT_TZ);

// הפרשנות: המחרוזות שמגיעות הן ימי-קצה (למשל "2025-08-21T00:00:00.000Z")
// אבל אנחנו רוצים "היום" באזור הזמן המקומי (כדי לא לאבד אירועי 00:15/23:30)
        Instant startInstant = Instant.parse(startDate); // לא קריטי, רק כדי להוציא LocalDate
        LocalDate day = startInstant.atZone(zone).toLocalDate();

        Instant min = day.atStartOfDay(zone).toInstant();
        Instant max = day.plusDays(1).atStartOfDay(zone).minusNanos(1).toInstant();

        Events events = googleCalendarClient.events()
                .list(calId)
                .setTimeMin(new DateTime(min.toEpochMilli()))
                .setTimeMax(new DateTime(max.toEpochMilli()))
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .setTimeZone(DEFAULT_TZ) // חשוב כדי שסטארט/אנד יגיעו לפי ה-TZ הזה
                .setFields("items(id,summary,start,end,location,description,attendees)")
                .execute();

        return events.getItems().stream().map(this::mapEvent).collect(Collectors.toList());

    }

    public Map<String, String> getEventById(String calendarId, String eventId, User user)
            throws IOException, GeneralSecurityException {

        final String calId = effectiveCalendarId(calendarId);
        Calendar googleCalendarClient = clientFor(user);

        com.google.api.services.calendar.model.Event event =
                googleCalendarClient.events().get(calId, eventId).execute();

        return mapEvent(event);
    }
}
