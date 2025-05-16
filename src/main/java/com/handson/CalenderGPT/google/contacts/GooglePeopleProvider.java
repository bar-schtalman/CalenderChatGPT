package com.handson.CalenderGPT.google.contacts;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.people.v1.PeopleService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;

@Component
public class GooglePeopleProvider {

    private static final String APPLICATION_NAME = "CalendarGPT";

    public PeopleService getPeopleService(OAuth2AuthorizedClient authorizedClient) throws Exception {
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        HttpRequestInitializer requestInitializer = request -> {
            request.getHeaders().setAuthorization("Bearer " + accessToken.getTokenValue());
        };

        return new PeopleService.Builder(httpTransport, jsonFactory, requestInitializer).setApplicationName(APPLICATION_NAME).build();
    }
}
