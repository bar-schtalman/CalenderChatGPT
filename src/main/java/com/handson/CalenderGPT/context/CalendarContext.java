package com.handson.CalenderGPT.context;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CalendarContext {

    private String calendarId;

    public String getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }

    private OAuth2AuthorizedClient authorizedClient;

    public void setAuthorizedClient(OAuth2AuthorizedClient client) {
        this.authorizedClient = client;
    }

    public OAuth2AuthorizedClient getAuthorizedClient() {
        return authorizedClient;
    }

}
