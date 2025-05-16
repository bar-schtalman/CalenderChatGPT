package com.handson.CalenderGPT.google.calendar;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.handson.CalenderGPT.context.CalendarContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoogleCalendarService {

    private final GoogleCalendarProvider calendarProvider;
    private final CalendarContext calendarContext;
    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);

    public GoogleCalendarService(GoogleCalendarProvider calendarProvider, CalendarContext calendarContext) {
        this.calendarProvider = calendarProvider;
        this.calendarContext = calendarContext;
    }


    private Calendar getCalendarClient() throws GeneralSecurityException, IOException {
        OAuth2AuthorizedClient client = calendarContext.getAuthorizedClient();
        if (client == null) {
            throw new IllegalStateException("User is not authenticated with Google.");
        }


        return calendarProvider.getCalendarClient(client);
    }

    public List<Map<String, Object>> getCalendars() throws IOException, GeneralSecurityException {
        List<Map<String, Object>> calendarDetails = new ArrayList<>();

        // Ensure that OAuth2AuthorizedClient is used correctly
        CalendarList calendarList = getCalendarClient().calendarList().list().execute();

        for (CalendarListEntry entry : calendarList.getItems()) {
            Map<String, Object> calendarInfo = new HashMap<>();
            calendarInfo.put("name", entry.getSummary());
            calendarInfo.put("id", entry.getId());
            calendarInfo.put("primary", Boolean.TRUE.equals(entry.getPrimary()));
            calendarDetails.add(calendarInfo);
        }

        return calendarDetails;
    }

    public CalendarListEntry getCalendarById(String calendarId) throws IOException, GeneralSecurityException {
        return getCalendarClient().calendarList().get(calendarId).execute();
    }

    public void deleteCalendar(String calendarId) throws IOException, GeneralSecurityException {
        getCalendarClient().calendarList().delete(calendarId).execute();
    }

    public String createCalendar(String summary) throws IOException, GeneralSecurityException {
        com.google.api.services.calendar.model.Calendar calendar = new com.google.api.services.calendar.model.Calendar().setSummary(summary).setTimeZone("UTC");

        com.google.api.services.calendar.model.Calendar createdCalendar = getCalendarClient().calendars().insert(calendar).execute();

        return createdCalendar.getId();
    }

    public Map<String, String> getDefaultCalendarDetails() throws IOException, GeneralSecurityException {
        CalendarList calendarList = getCalendarClient().calendarList().list().execute();

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
