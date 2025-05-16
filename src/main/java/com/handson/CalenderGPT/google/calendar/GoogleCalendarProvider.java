package com.handson.CalenderGPT.google.calendar;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.handson.CalenderGPT.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Component
public class GoogleCalendarProvider {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;


    public Calendar getCalendarClient(OAuth2AuthorizedClient client) throws GeneralSecurityException, IOException {
        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException("OAuth2AuthorizedClient or access token is missing.");
        }

        return new Calendar.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, request -> request.getHeaders().setAuthorization("Bearer " + client.getAccessToken().getTokenValue())).setApplicationName("CalendarGPT").build();
    }

    public Calendar getCalendarClient(User user) throws GeneralSecurityException, IOException {
        if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isBlank()) {
            throw new IllegalStateException("Missing Google refresh token for user: " + user.getEmail());
        }

        GoogleCredential credential = new GoogleCredential.Builder().setTransport(GoogleNetHttpTransport.newTrustedTransport()).setJsonFactory(JSON_FACTORY).setClientSecrets(clientId, clientSecret).build().setRefreshToken(user.getGoogleRefreshToken());

        // Refresh the token if necessary
        try {
            credential.refreshToken();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to refresh Google credentials for user: " + user.getEmail(), e);
        }

        return new Calendar.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, credential).setApplicationName("CalendarGPT").build();
    }
}
