// src/main/java/com/handson/CalenderGPT/controller/CalendarController.java
package com.handson.CalenderGPT.controller;

import com.google.api.services.calendar.model.CalendarListEntry;
import com.handson.CalenderGPT.google.calendar.GoogleCalendarService;
import com.handson.CalenderGPT.model.User;
import com.handson.CalenderGPT.repository.UserRepository;
import com.handson.CalenderGPT.security.AuthenticatedUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/google-calendar/calendars")
@RequiredArgsConstructor
public class CalendarController {

    private final GoogleCalendarService googleCalendarService;
    private final AuthenticatedUserResolver userResolver;
    private final UserRepository userRepository; // עבור עדכון default_calendar_id

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getCalendarsWithDetails() {
        try {
            User user = userResolver.getCurrentUserOrThrow();
            List<Map<String, Object>> calendarDetails = googleCalendarService.getCalendars(user);
            return ResponseEntity.ok(calendarDetails);
        } catch (Exception e) {
            e.printStackTrace();
            // 401 כשאין אימות/ריפרש טוקן תקין
            if (e instanceof IllegalStateException && e.getMessage().toLowerCase().contains("refresh")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/default")
    public ResponseEntity<Map<String, String>> getDefaultCalendar() {
        try {
            User user = userResolver.getCurrentUserOrThrow();

            // אם כבר שמור לו default_calendar_id — נחזיר אותו (שימושי ומהיר)
            if (user.getDefaultCalendarId() != null && !user.getDefaultCalendarId().isBlank()) {
                CalendarListEntry entry = googleCalendarService.getCalendarById(user.getDefaultCalendarId(), user);
                Map<String, String> res = new HashMap<>();
                res.put("name", entry.getSummary());
                res.put("id", entry.getId());
                return ResponseEntity.ok(res);
            }

            // אחרת—נאתר Primary מגוגל
            Map<String, String> defaultCalendar = googleCalendarService.getDefaultCalendarDetails(user);
            return ResponseEntity.ok(defaultCalendar);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{calendarId}")
    public ResponseEntity<CalendarListEntry> getCalendarById(@PathVariable String calendarId) {
        try {
            User user = userResolver.getCurrentUserOrThrow();
            CalendarListEntry calendar = googleCalendarService.getCalendarById(calendarId, user);
            return ResponseEntity.ok(calendar);
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException gjre) {
            gjre.printStackTrace();
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{calendarId}")
    public ResponseEntity<String> deleteCalendar1(@PathVariable String calendarId) {
        try {
            User user = userResolver.getCurrentUserOrThrow();
            googleCalendarService.deleteCalendar(calendarId, user);

            // אם מחקנו את היומן שהיה ברירת־המחדל — ננקה מה־DB
            if (calendarId.equals(user.getDefaultCalendarId())) {
                user.setDefaultCalendarId(null);
                userRepository.save(user);
            }
            return ResponseEntity.ok("Calendar with ID " + calendarId + " deleted successfully.");
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException gjre) {
            gjre.printStackTrace();
            return ResponseEntity.status(404).body("Failed to delete calendar. Calendar ID not found.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Unexpected error occurred.");
        }
    }

    @PostMapping
    public ResponseEntity<String> createCalendar(@RequestParam String summary) {
        try {
            User user = userResolver.getCurrentUserOrThrow();
            String calendarId = googleCalendarService.createCalendar(summary, user);
            return ResponseEntity.ok("Calendar created successfully with ID: " + calendarId);
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException gjre) {
            gjre.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to create calendar: " + gjre.getDetails());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred while creating the calendar.");
        }
    }

    @PostMapping("/select-calendar")
    public ResponseEntity<Void> selectCalendar(@RequestParam String calendarId) {
        try {
            User user = userResolver.getCurrentUserOrThrow();

            // וולידציה זריזה: היומן באמת קיים אצל המשתמש
            googleCalendarService.getCalendarById(calendarId, user);

            user.setDefaultCalendarId(calendarId);
            userRepository.save(user);

            System.out.println("✅ Selected calendar updated to: " + calendarId + " for user " + user.getEmail());
            return ResponseEntity.ok().build();
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException gjre) {
            gjre.printStackTrace();
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
