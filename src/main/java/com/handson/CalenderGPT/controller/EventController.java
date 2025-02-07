package com.handson.CalenderGPT.controller;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.handson.CalenderGPT.config.GoogleCalendarConfig;
import com.handson.CalenderGPT.model.Event;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/google-calendar/calendars/{calendarId}/events")
public class EventController {

    private final GoogleCalendarConfig googleCalendarConfig;
    private Calendar googleCalendarClient;

    @Autowired
    public EventController(GoogleCalendarConfig googleCalendarConfig) {
        this.googleCalendarConfig = googleCalendarConfig;
    }

    /**
     * Lazily initializes the Google Calendar client when needed.
     */
    private Calendar getGoogleCalendarClient() throws Exception {
        if (googleCalendarClient == null) {
            googleCalendarClient = googleCalendarConfig.googleCalendarClient();
        }
        return googleCalendarClient;
    }

    /**
     * Creates an event in the specified Google Calendar.
     *
     * @param calendarId The ID of the calendar.
     * @param eventRequest The event details from your custom Event model.
     * @return Confirmation message with event link.
     */
    @PostMapping
    public ResponseEntity<String> createEvent(
            @PathVariable String calendarId,
            @RequestBody Event eventRequest) {
        try {
            DateTime startDateTime = convertToGoogleDateTime(eventRequest.getStart(), "UTC");
            DateTime endDateTime = convertToGoogleDateTime(eventRequest.getEnd(), "UTC");

            com.google.api.services.calendar.model.Event googleEvent = new com.google.api.services.calendar.model.Event()
                    .setSummary(eventRequest.getSummary())
                    .setLocation(eventRequest.getLocation())
                    .setDescription(eventRequest.getDescription());

            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone("UTC");
            googleEvent.setStart(start);

            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime)
                    .setTimeZone("UTC");
            googleEvent.setEnd(end);

            com.google.api.services.calendar.model.Event createdEvent = getGoogleCalendarClient()
                    .events()
                    .insert(calendarId, googleEvent)
                    .execute();

            return ResponseEntity.ok("Event created successfully: " + createdEvent.getHtmlLink());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Failed to create event: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error occurred while creating the event.");
        }
    }

    /**
     * Updates an existing event in the specified Google Calendar.
     *
     * @param calendarId The ID of the calendar.
     * @param eventId The ID of the event to update.
     * @param eventRequest The updated event details from your custom Event model.
     * @return Confirmation message with updated event link.
     */
    @PutMapping("/{eventId}")
    public ResponseEntity<String> updateEvent(
            @PathVariable String calendarId,
            @PathVariable String eventId,
            @RequestBody Event eventRequest) {
        try {
            com.google.api.services.calendar.model.Event existingEvent = getGoogleCalendarClient()
                    .events()
                    .get(calendarId, eventId)
                    .execute();

            existingEvent.setSummary(eventRequest.getSummary());
            existingEvent.setLocation(eventRequest.getLocation());
            existingEvent.setDescription(eventRequest.getDescription());

            DateTime startDateTime = convertToGoogleDateTime(eventRequest.getStart(), "UTC");
            DateTime endDateTime = convertToGoogleDateTime(eventRequest.getEnd(), "UTC");

            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone("UTC");
            existingEvent.setStart(start);

            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime)
                    .setTimeZone("UTC");
            existingEvent.setEnd(end);

            com.google.api.services.calendar.model.Event updatedEvent = getGoogleCalendarClient()
                    .events()
                    .update(calendarId, eventId, existingEvent)
                    .execute();

            return ResponseEntity.ok("Event updated successfully: " + updatedEvent.getHtmlLink());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Failed to update event: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error occurred while updating the event.");
        }
    }

    /**
     * Fetches events from the specified calendar within a date range.
     *
     * @param calendarId The ID of the calendar.
     * @param startDate  The start date-time (RFC3339 format).
     *                   Example: "2025-02-01T00:00:00Z"
     * @param endDate    The end date-time (RFC3339 format).
     *                   Example: "2025-02-28T23:59:59Z"
     * @return List of events with ID, summary, time, location, and description.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, String>>> getEventsInDateRange(
            @PathVariable String calendarId,
            @RequestParam @ApiParam(value = "Start date-time in RFC3339 format", example = "2025-02-01T00:00:00Z", defaultValue = "2025-02-01T00:00:00Z") String startDate,
            @RequestParam @ApiParam(value = "End date-time in RFC3339 format", example = "2025-02-28T23:59:59Z", defaultValue = "2025-02-28T23:59:59Z") String endDate) {
        try {
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

            List<com.google.api.services.calendar.model.Event> items = events.getItems();
            List<Map<String, String>> simplifiedEvents = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

            for (com.google.api.services.calendar.model.Event event : items) {
                Map<String, String> eventDetails = new HashMap<>();
                eventDetails.put("id", event.getId());
                eventDetails.put("summary", event.getSummary());
                eventDetails.put("start", event.getStart() != null && event.getStart().getDateTime() != null
                        ? convertToLocalDateTime(event.getStart().getDateTime()).format(formatter) : "");
                eventDetails.put("end", event.getEnd() != null && event.getEnd().getDateTime() != null
                        ? convertToLocalDateTime(event.getEnd().getDateTime()).format(formatter) : "");
                eventDetails.put("location", event.getLocation() != null ? event.getLocation() : "");
                eventDetails.put("description", event.getDescription() != null ? event.getDescription() : "");
                simplifiedEvents.add(eventDetails);
            }

            return ResponseEntity.ok(simplifiedEvents.isEmpty() ? Collections.emptyList() : simplifiedEvents);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Deletes a specific event from the specified Google Calendar.
     *
     * @param calendarId The ID of the calendar.
     * @param eventId The ID of the event to delete.
     * @return Confirmation message on successful deletion.
     */
    @DeleteMapping("/{eventId}")
    public ResponseEntity<String> deleteEvent(
            @PathVariable String calendarId,
            @PathVariable String eventId) {
        try {
            getGoogleCalendarClient()
                    .events()
                    .delete(calendarId, eventId)
                    .execute();

            return ResponseEntity.ok("Event with ID " + eventId + " deleted successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Failed to delete event: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error occurred while deleting the event.");
        }
    }

    /**
     * Utility method to convert LocalDateTime to Google's DateTime object.
     */
    private DateTime convertToGoogleDateTime(LocalDateTime localDateTime, String timeZoneId) {
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of(timeZoneId));
        return new DateTime(zonedDateTime.toInstant().toEpochMilli());
    }

    /**
     * Utility method to convert Google's DateTime to LocalDateTime.
     */
    private LocalDateTime convertToLocalDateTime(DateTime googleDateTime) {
        if (googleDateTime == null) {
            return LocalDateTime.now(ZoneId.of("UTC"));
        }
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(googleDateTime.getValue()),
                ZoneId.of("UTC")
        );
    }
}
