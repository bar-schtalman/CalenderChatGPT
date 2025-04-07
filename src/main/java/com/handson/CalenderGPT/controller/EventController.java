package com.handson.CalenderGPT.controller;

import com.handson.CalenderGPT.context.CalendarContext;
import com.handson.CalenderGPT.model.Event;
import com.handson.CalenderGPT.service.EventService;
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
        return eventService.createEvent(calendarId, eventRequest);
    }


    @PutMapping("/update/{eventId}")
    public String updateEvent(@PathVariable String eventId, @RequestBody Event eventRequest) throws Exception {
        String calendarId = getCalendarId();
        return eventService.updateEvent(calendarId, eventId, eventRequest);
    }

    @GetMapping("/view")
    public List<Map<String, String>> getEvents(@RequestParam String start, @RequestParam String end) throws Exception {
        String calendarId = getCalendarId();
        return eventService.getEventsInDateRange(calendarId, start, end);
    }

    @GetMapping("/{eventId}")
    public Map<String, String> getEventById(@PathVariable String eventId) throws Exception {
        String calendarId = getCalendarId();
        return eventService.getEventById(calendarId, eventId);
    }

    @DeleteMapping("/delete/{eventId}")
    public void deleteEvent(@PathVariable String eventId) throws Exception {
        String calendarId = getCalendarId();
        eventService.deleteEvent(calendarId, eventId);
    }

    private String getCalendarId() {
        String calendarId = calendarContext.getCalendarId();
        if (calendarId == null) {
            throw new IllegalStateException("Calendar ID not found");
        }
        return calendarId;
    }

    @PutMapping("/{eventId}/guests")
    public Map<String, String> addGuestsToEvent(@PathVariable String eventId, @RequestBody List<String> guests) throws Exception {
        String calendarId = getCalendarId();
        return eventService.addGuests(calendarId, eventId, guests);
    }

}
