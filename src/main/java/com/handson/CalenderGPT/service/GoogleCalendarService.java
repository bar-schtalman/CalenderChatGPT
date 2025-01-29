package com.handson.CalenderGPT.service;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleCalendarService {

    @Autowired
    private Calendar googleCalendarClient; // Inject your Google Calendar client.

    /**
     * Fetches the list of all calendars for the authenticated user.
     *
     * @return List of calendar summaries.
     * @throws IOException If the API call fails.
     */
    public List<String> getCalendars() throws IOException {
        List<String> calendarSummaries = new ArrayList<>();

        // Fetch the list of calendars
        CalendarList calendarList = googleCalendarClient.calendarList().list().execute();

        // Iterate through the calendars and collect their summaries
        for (CalendarListEntry entry : calendarList.getItems()) {
            calendarSummaries.add(entry.getSummary());
        }

        return calendarSummaries;
    }
}
