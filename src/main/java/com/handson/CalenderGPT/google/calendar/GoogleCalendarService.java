package com.handson.CalenderGPT.google.calendar;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.handson.CalenderGPT.model.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);

    private final GoogleCalendarProvider calendarProvider;

    private Calendar clientFor(User user) throws GeneralSecurityException, IOException {
        if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isBlank()) {
            throw new IllegalStateException("No refresh token available for user: " + user.getEmail());
        }

        // תמיד דרך refresh token של המשתמש
        return calendarProvider.getCalendarClient(user);
    }

    public List<Map<String, Object>> getCalendars(User user) throws IOException, GeneralSecurityException {
        if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isBlank()) {
            throw new IllegalStateException("No refresh token available for user: " + user.getEmail());
        }
        List<Map<String, Object>> calendarDetails = new ArrayList<>();

        CalendarList calendarList = clientFor(user).calendarList().list().execute();

        for (CalendarListEntry entry : calendarList.getItems()) {
            Map<String, Object> calendarInfo = new HashMap<>();
            calendarInfo.put("name", entry.getSummary());
            calendarInfo.put("id", entry.getId());
            calendarInfo.put("primary", Boolean.TRUE.equals(entry.getPrimary()));
            calendarDetails.add(calendarInfo);
        }

        return calendarDetails;
    }

    public CalendarListEntry getCalendarById(String calendarId, User user) throws IOException, GeneralSecurityException {
        if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isBlank()) {
            throw new IllegalStateException("No refresh token available for user: " + user.getEmail());
        }
        return clientFor(user).calendarList().get(calendarId).execute();
    }

    public void deleteCalendar(String calendarId, User user) throws IOException, GeneralSecurityException {
        if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isBlank()) {
            throw new IllegalStateException("No refresh token available for user: " + user.getEmail());
        }
        clientFor(user).calendarList().delete(calendarId).execute();
    }

    public String createCalendar(String summary, User user) throws IOException, GeneralSecurityException {
        if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isBlank()) {
            throw new IllegalStateException("No refresh token available for user: " + user.getEmail());
        }
        com.google.api.services.calendar.model.Calendar calendar =
                new com.google.api.services.calendar.model.Calendar()
                        .setSummary(summary)
                        .setTimeZone("UTC");

        com.google.api.services.calendar.model.Calendar created =
                clientFor(user).calendars().insert(calendar).execute();

        return created.getId();
    }

    public Map<String, String> getDefaultCalendarDetails(User user) throws IOException, GeneralSecurityException {
        if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isBlank()) {
            throw new IllegalStateException("No refresh token available for user: " + user.getEmail());
        }
        CalendarList calendarList = clientFor(user).calendarList().list().execute();

        for (CalendarListEntry entry : calendarList.getItems()) {
            if (Boolean.TRUE.equals(entry.getPrimary())) {
                Map<String, String> m = new HashMap<>();
                m.put("name", entry.getSummary());
                m.put("id", entry.getId());
                return m;
            }
        }
        throw new IOException("Default calendar not found.");
    }
}
