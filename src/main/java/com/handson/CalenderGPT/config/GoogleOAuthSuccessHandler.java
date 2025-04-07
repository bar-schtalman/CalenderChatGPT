package com.handson.CalenderGPT.config;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.handson.CalenderGPT.context.CalendarContext;
import com.handson.CalenderGPT.model.User;
import com.handson.CalenderGPT.model.UserSession;
import com.handson.CalenderGPT.provider.GoogleCalendarProvider;
import com.handson.CalenderGPT.repository.UserRepository;
import com.handson.CalenderGPT.repository.UserSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;


@Component
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthSuccessHandler.class);

    private final OAuth2AuthorizedClientService clientService;
    private final GoogleCalendarProvider calendarProvider;
    private final CalendarContext calendarContext;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;

    public GoogleOAuthSuccessHandler(
            OAuth2AuthorizedClientService clientService,
            GoogleCalendarProvider calendarProvider,
            CalendarContext calendarContext,
            UserRepository userRepository,
            UserSessionRepository userSessionRepository
    ) {
        this.clientService = clientService;
        this.calendarProvider = calendarProvider;
        this.calendarContext = calendarContext;
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            log.info("✅ Google OAuth2 authentication successful");

            OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getName()
            );
            calendarContext.setAuthorizedClient(client);
            log.info("✅ Stored OAuth2AuthorizedClient in CalendarContext");


            Map<String, Object> attributes = oauthToken.getPrincipal().getAttributes();
            String email = (String) attributes.get("email");
            String fullName = (String) attributes.get("name");
            String firstName = (String) attributes.get("given_name");
            String lastName = (String) attributes.get("family_name");

            // Find or create user
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                log.info("🆕 New user detected. Creating user: {}", email);
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setFirstName(firstName != null ? firstName : fullName);
                newUser.setLastName(lastName != null ? lastName : "");
                return userRepository.saveAndFlush(newUser);  // Save and flush to ensure user is managed
            });

            // Create new session
            String sessionId = request.getSession().getId();
            UserSession userSession = new UserSession();
            userSession.setUser(user);  // Assign existing User entity
            userSession.setSessionId(sessionId);
            userSession.setLoginTime(LocalDateTime.now());

            userSessionRepository.save(userSession);  // Save session
            log.info("📌 Stored new session: {} for user {}", sessionId, email);

            // Fetch and store calendar ID
            try {
                Calendar calendarClient = calendarProvider.getCalendarClient(client);
                CalendarList list = calendarClient.calendarList().list().execute();

                for (CalendarListEntry entry : list.getItems()) {
                    if (Boolean.TRUE.equals(entry.getPrimary())) {
                        calendarContext.setCalendarId(entry.getId());
                        log.info("✅ Stored calendar ID in session: {}", entry.getId());
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("❌ Failed to fetch calendar info", e);
            }
        }

        response.sendRedirect("/chat-ui");
    }
}
