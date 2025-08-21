package com.handson.CalenderGPT.google.calendar;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.handson.CalenderGPT.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Component
public class GoogleCalendarProvider {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "CalendarGPT";

    private static final com.google.api.client.http.HttpTransport HTTP_TRANSPORT;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException | IOException e) {
            throw new ExceptionInInitializerError("Failed to initialize HTTP_TRANSPORT: " + e.getMessage());
        }
    }

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    /**
     * Build Calendar client using refresh token from User (JWT stateless flow).
     */
    public Calendar getCalendarClient(User user) throws IOException {
        if (user == null || user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isBlank()) {
            throw new IllegalStateException("Missing Google refresh token for user");
        }

        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(user.getGoogleRefreshToken());

        if (!credential.refreshToken()) {
            throw new IllegalStateException("Failed to refresh Google credentials for user: " + user.getEmail());
        }

        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
