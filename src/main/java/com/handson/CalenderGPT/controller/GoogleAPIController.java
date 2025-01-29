package com.handson.CalenderGPT.controller;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.handson.CalenderGPT.config.GoogleCalendarConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/google-calendar")
public class GoogleAPIController {

    private final GoogleCalendarConfig googleCalendarConfig;
    private Calendar googleCalendarClient;

    @Autowired
    public GoogleAPIController(GoogleCalendarConfig googleCalendarConfig) {
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
     * Fetches the list of calendars along with their IDs for the authenticated user.
     *
     * @return List of strings in "calendarName,calendarId" format.
     */
    @GetMapping("/calendars")
    public ResponseEntity<List<String>> getCalendarsWithIds() {
        try {
            List<String> calendarPairs = new ArrayList<>();
            CalendarList calendarList = getGoogleCalendarClient().calendarList().list().execute();

            for (CalendarListEntry entry : calendarList.getItems()) {
                calendarPairs.add(entry.getSummary() + "," + entry.getId());
            }

            return ResponseEntity.ok(calendarPairs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Fetches a specific calendar by its ID.
     *
     * @param calendarId The ID of the calendar to retrieve.
     * @return CalendarListEntry object containing details of the calendar.
     */
    @GetMapping("/calendars/{calendarId}")
    public ResponseEntity<CalendarListEntry> getCalendarById(@PathVariable String calendarId) {
        try {
            CalendarListEntry calendarListEntry = getGoogleCalendarClient()
                    .calendarList()
                    .get(calendarId)
                    .execute();
            return ResponseEntity.ok(calendarListEntry);
        } catch (IOException e) {
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Deletes a calendar from the user's Google Calendar list.
     *
     * @param calendarId The ID of the calendar to delete.
     * @return ResponseEntity with success or failure message.
     */
    @DeleteMapping("/calendars/{calendarId}")
    public ResponseEntity<String> deleteCalendar(@PathVariable String calendarId) {
        try {
            getGoogleCalendarClient().calendarList().delete(calendarId).execute();
            return ResponseEntity.ok("Calendar with ID " + calendarId + " deleted successfully.");
        } catch (IOException e) {
            return ResponseEntity.status(404).body("Failed to delete calendar. Calendar ID not found.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Unexpected error occurred.");
        }
    }


}
