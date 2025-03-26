package com.handson.CalenderGPT.controller;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.handson.CalenderGPT.config.GoogleCalendarConfig;
import com.handson.CalenderGPT.context.CalendarContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.handson.CalenderGPT.service.GoogleCalendarService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/google-calendar/calendars")
public class CalendarController {

    private final GoogleCalendarService googleCalendarService;
    private final CalendarContext calendarContext;

    @Autowired
    public CalendarController(GoogleCalendarService googleCalendarService, CalendarContext calendarContext) {
        this.googleCalendarService = googleCalendarService;
        this.calendarContext = calendarContext;
        /*
        // Set the default calendar ID at application start
        try {
            Map<String, String> defaultCalendar = googleCalendarService.getDefaultCalendarDetails();
            //this.calendarContext.setCalendarId(defaultCalendar.get("id"));
        } catch (IOException e) {
            e.printStackTrace();
            // Handle case where default calendar is not found
        }
        */
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getCalendarsWithDetails() {
        try {
            List<Map<String, Object>> calendarDetails = googleCalendarService.getCalendars();
            return ResponseEntity.ok(calendarDetails);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/default")
    public ResponseEntity<Map<String, String>> getDefaultCalendar() {
        try {
            Map<String, String> defaultCalendar = googleCalendarService.getDefaultCalendarDetails();
            return ResponseEntity.ok(defaultCalendar);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{calendarId}")
    public ResponseEntity<CalendarListEntry> getCalendarById(@PathVariable String calendarId) {
        try {
            CalendarListEntry calendar = googleCalendarService.getCalendarById(calendarId);
            return ResponseEntity.ok(calendar);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{calendarId}")
    public ResponseEntity<String> deleteCalendar(@PathVariable String calendarId) {
        try {
            googleCalendarService.deleteCalendar(calendarId);
            return ResponseEntity.ok("Calendar with ID " + calendarId + " deleted successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(404).body("Failed to delete calendar. Calendar ID not found.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Unexpected error occurred.");
        }
    }

    @PostMapping
    public ResponseEntity<String> createCalendar(@RequestParam String summary) {
        try {
            String calendarId = googleCalendarService.createCalendar(summary);
            return ResponseEntity.ok("Calendar created successfully with ID: " + calendarId);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to create calendar: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred while creating the calendar.");
        }
    }

}