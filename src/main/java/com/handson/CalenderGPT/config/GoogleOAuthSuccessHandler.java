package com.handson.CalenderGPT.config;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.handson.CalenderGPT.context.CalendarContext;
import com.handson.CalenderGPT.config.GoogleCalendarProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.*;
import java.io.IOException;

@Component
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthSuccessHandler.class);

    private final OAuth2AuthorizedClientService clientService;
    private final GoogleCalendarProvider calendarProvider;
    private final CalendarContext calendarContext;

    public GoogleOAuthSuccessHandler(OAuth2AuthorizedClientService clientService,
                                     GoogleCalendarProvider calendarProvider,
                                     CalendarContext calendarContext) {
        this.clientService = clientService;
        this.calendarProvider = calendarProvider;
        this.calendarContext = calendarContext;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (authentication instanceof OAuth2AuthenticationToken) {
            log.info("✅ Google OAuth2 authentication successful");

            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

            OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getName()
            );

            try {
                Calendar calendarClient = calendarProvider.getCalendarClient(client);
                CalendarList list = calendarClient.calendarList().list().execute();

                for (CalendarListEntry entry : list.getItems()) {
                    if (Boolean.TRUE.equals(entry.getPrimary())) {
                        calendarContext.setCalendarId(entry.getId());
                        log.info("✅ Stored session calendar ID: {}", entry.getId());
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("❌ Failed to fetch calendar info", e);
            }
        }

        response.sendRedirect("/");
    }
}
