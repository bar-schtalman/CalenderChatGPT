package com.handson.CalenderGPT.service;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoogleCalendarService {

    @Autowired
    private Calendar googleCalendarClient; // Inject your Google Calendar client.

    /**
     * Fetches the list of all calendars for the authenticated user with details.
     *
     * @return List of calendar details (name, id, primary status).
     * @throws IOException If the API call fails.
     */
    public List<Map<String, Object>> getCalendars() throws IOException {
        List<Map<String, Object>> calendarDetails = new ArrayList<>();

        // Fetch the list of calendars
        CalendarList calendarList = googleCalendarClient.calendarList().list().execute();

        // Iterate through the calendars and collect their details
        for (CalendarListEntry entry : calendarList.getItems()) {
            Map<String, Object> calendarInfo = new HashMap<>();
            calendarInfo.put("name", entry.getSummary());
            calendarInfo.put("id", entry.getId());
            calendarInfo.put("primary", Boolean.TRUE.equals(entry.getPrimary()));

            calendarDetails.add(calendarInfo);
        }

        return calendarDetails;
    }

    /**
     * Fetch a calendar by its ID.
     */
    public CalendarListEntry getCalendarById(String calendarId) throws IOException {
        return googleCalendarClient.calendarList().get(calendarId).execute();
    }

    /**
     * Delete a calendar by its ID.
     */
    public void deleteCalendar(String calendarId) throws IOException {
        googleCalendarClient.calendarList().delete(calendarId).execute();
    }

    /**
     * Create a new calendar.
     */
    public String createCalendar(String summary) throws IOException {
        com.google.api.services.calendar.model.Calendar calendar = new com.google.api.services.calendar.model.Calendar().setSummary(summary).setTimeZone("UTC");

        com.google.api.services.calendar.model.Calendar createdCalendar = googleCalendarClient.calendars().insert(calendar).execute();

        return createdCalendar.getId();
    }


    public Map<String, String> getDefaultCalendarDetails() throws IOException {
        CalendarList calendarList = googleCalendarClient.calendarList().list().execute();
        for (CalendarListEntry entry : calendarList.getItems()) {
            if (Boolean.TRUE.equals(entry.getPrimary())) {
                Map<String, String> defaultCalendarDetails = new HashMap<>();
                defaultCalendarDetails.put("name", entry.getSummary());
                defaultCalendarDetails.put("id", entry.getId());
                return defaultCalendarDetails;
            }
        }
        throw new IOException("Default calendar not found.");
    }


}
