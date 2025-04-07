package com.handson.CalenderGPT.provider;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Component
public class GoogleCalendarProvider {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public Calendar getCalendarClient(OAuth2AuthorizedClient client) throws GeneralSecurityException, IOException {
        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                request -> request.getHeaders().setAuthorization("Bearer " + client.getAccessToken().getTokenValue())
        )
                .setApplicationName("CalendarGPT")
                .build();
    }
}
