package com.handson.CalenderGPT.controller;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.handson.CalenderGPT.config.GoogleCalendarConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/google-calendar/calendars")
public class CalendarController {

    private final GoogleCalendarConfig googleCalendarConfig;
    private Calendar googleCalendarClient;

    @Autowired
    public CalendarController(GoogleCalendarConfig googleCalendarConfig) {
        this.googleCalendarConfig = googleCalendarConfig;
    }

    private Calendar getGoogleCalendarClient() throws Exception {
        if (googleCalendarClient == null) {
            googleCalendarClient = googleCalendarConfig.googleCalendarClient();
        }
        return googleCalendarClient;
    }

    @GetMapping
    public ResponseEntity<List<String>> getCalendarsWithIds() {
        try {
            CalendarList calendarList = getGoogleCalendarClient().calendarList().list().execute();
            List<String> calendarPairs = new ArrayList<>();
            for (CalendarListEntry entry : calendarList.getItems()) {
                calendarPairs.add(entry.getSummary() + "," + entry.getId());
            }
            return ResponseEntity.ok(calendarPairs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/{calendarId}")
    public ResponseEntity<CalendarListEntry> getCalendarById(@PathVariable String calendarId) {
        try {
            CalendarListEntry calendar = getGoogleCalendarClient().calendarList().get(calendarId).execute();
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
            getGoogleCalendarClient().calendarList().delete(calendarId).execute();
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
            com.google.api.services.calendar.model.Calendar calendar = new com.google.api.services.calendar.model.Calendar()
                    .setSummary(summary)
                    .setTimeZone("UTC"); // You can modify the timezone if needed

            com.google.api.services.calendar.model.Calendar createdCalendar = getGoogleCalendarClient()
                    .calendars()
                    .insert(calendar)
                    .execute();

            return ResponseEntity.ok("Calendar created successfully with ID: " + createdCalendar.getId());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to create calendar: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error occurred while creating the calendar.");
        }
    }

}
