package com.handson.CalenderGPT.controller;

import com.handson.CalenderGPT.context.CalendarContext;
import com.handson.CalenderGPT.model.Event;
import com.handson.CalenderGPT.model.User;
import com.handson.CalenderGPT.service.EventService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final CalendarContext calendarContext;
    private final EventService eventService;

    public EventController(CalendarContext calendarContext, EventService eventService) {
        this.calendarContext = calendarContext;
        this.eventService = eventService;
    }

    @PostMapping("/create")
    public Map<String, String> createEvent(@RequestBody Event eventRequest) throws Exception {
        String calendarId = getCalendarId();
        User user = getAuthenticatedUser();
        return eventService.createEvent(calendarId, eventRequest, user);
    }

    @PutMapping("/update/{eventId}")
    public String updateEvent(@PathVariable String eventId, @RequestBody Event eventRequest) throws Exception {
        String calendarId = getCalendarId();
        User user = getAuthenticatedUser();
        return eventService.updateEvent(calendarId, eventId, eventRequest, user);
    }

    @GetMapping("/view")
    public List<Map<String, String>> getEvents(@RequestParam String start, @RequestParam String end) throws Exception {
        String calendarId = getCalendarId();
        User user = getAuthenticatedUser();
        return eventService.getEventsInDateRange(calendarId, start, end, user);
    }

    @GetMapping("/{eventId}")
    public Map<String, String> getEventById(@PathVariable String eventId) throws Exception {
        String calendarId = getCalendarId();
        User user = getAuthenticatedUser();
        return eventService.getEventById(calendarId, eventId, user);
    }

    @DeleteMapping("/delete/{eventId}")
    public void deleteEvent(@PathVariable String eventId) throws Exception {
        String calendarId = getCalendarId();
        User user = getAuthenticatedUser();
        eventService.deleteEvent(calendarId, eventId, user);
    }

    @PutMapping("/{eventId}/guests")
    public Map<String, String> addGuestsToEvent(@PathVariable String eventId, @RequestBody List<String> guests) throws Exception {
        String calendarId = getCalendarId();
        User user = getAuthenticatedUser();
        return eventService.addGuests(calendarId, eventId, guests, user);
    }

    @PutMapping("/{eventId}/guests/remove")
    public Map<String, String> removeGuestsFromEvent(@PathVariable String eventId, @RequestBody List<String> guests) throws Exception {
        String calendarId = getCalendarId();
        User user = getAuthenticatedUser();
        return eventService.removeGuests(calendarId, eventId, guests, user);
    }

    private String getCalendarId() {
        String calendarId = calendarContext.getCalendarId();
        if (calendarId == null) {
            throw new IllegalStateException("Calendar ID not found");
        }
        return calendarId;
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            throw new IllegalStateException("Authenticated user not found");
        }
        return (User) auth.getPrincipal();
    }
}
