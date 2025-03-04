package com.handson.CalenderGPT.controller;

import com.handson.CalenderGPT.model.Event;
import com.handson.CalenderGPT.service.EventService;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/google-calendar/calendars/{calendarId}/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @PostMapping
    public ResponseEntity<String> createEvent(@PathVariable String calendarId, @RequestBody Event eventRequest) {
        try {
            String eventLink = eventService.createEvent(calendarId, eventRequest);
            return ResponseEntity.ok("Event created successfully: " + eventLink);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred while creating the event.");
        }
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<String> updateEvent(@PathVariable String calendarId, @PathVariable String eventId, @RequestBody Event eventRequest) {
        try {
            String eventLink = eventService.updateEvent(calendarId, eventId, eventRequest);
            return ResponseEntity.ok("Event updated successfully: " + eventLink);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred while updating the event.");
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, String>>> getEventsInDateRange(@PathVariable String calendarId, @RequestParam @ApiParam(value = "Start date-time in RFC3339 format", example = "2025-02-01T00:00:00Z") String startDate, @RequestParam @ApiParam(value = "End date-time in RFC3339 format", example = "2025-02-28T23:59:59Z") String endDate) {
        try {
            List<Map<String, String>> events = eventService.getEventsInDateRange(calendarId, startDate, endDate);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<String> deleteEvent(@PathVariable String calendarId, @PathVariable String eventId) {
        try {
            eventService.deleteEvent(calendarId, eventId);
            return ResponseEntity.ok("Event with ID " + eventId + " deleted successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred while deleting the event.");
        }
    }
}
