package com.handson.CalenderGPT.config;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.handson.CalenderGPT.context.CalendarContext;
import com.handson.CalenderGPT.jwt.JwtTokenUtil;
import com.handson.CalenderGPT.model.User;
import com.handson.CalenderGPT.provider.GoogleCalendarProvider;
import com.handson.CalenderGPT.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthSuccessHandler.class);

    private final OAuth2AuthorizedClientService clientService;
    private final GoogleCalendarProvider calendarProvider;
    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final CalendarContext calendarContext;

    public GoogleOAuthSuccessHandler(
            OAuth2AuthorizedClientService clientService,
            GoogleCalendarProvider calendarProvider,
            UserRepository userRepository,
            JwtTokenUtil jwtTokenUtil,
            CalendarContext calendarContext
    ) {
        this.clientService = clientService;
        this.calendarProvider = calendarProvider;
        this.userRepository = userRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.calendarContext = calendarContext;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
            return;
        }

        log.info("✅ Google OAuth2 authentication successful");

        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );

        if (client == null || client.getAccessToken() == null) {
            log.error("❌ OAuth2AuthorizedClient not available after login");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing authorized client");
            return;
        }

        Map<String, Object> attributes = oauthToken.getPrincipal().getAttributes();
        String email = (String) attributes.get("email");
        String fullName = (String) attributes.get("name");
        String firstName = (String) attributes.getOrDefault("given_name", fullName);
        String lastName = (String) attributes.getOrDefault("family_name", "");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("🆕 New user detected. Creating user: {}", email);
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(fullName);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            return newUser;
        });

        user.setJwtIssuedAt(Instant.now());

        // Store refresh token if available
        if (client.getRefreshToken() != null) {
            user.setGoogleRefreshToken(client.getRefreshToken().getTokenValue());
            log.info("🔁 Refresh token stored for user: {}", email);
        } else {
            log.warn("⚠️ No refresh token returned from Google. Re-consent may be needed.");
        }

        // Fetch and store default calendar ID
        try {
            Calendar calendarClient = calendarProvider.getCalendarClient(client);
            CalendarList list = calendarClient.calendarList().list().execute();

            for (CalendarListEntry entry : list.getItems()) {
                if (Boolean.TRUE.equals(entry.getPrimary())) {
                    user.setDefaultCalendarId(entry.getId());
                    log.info("✅ Stored primary calendar ID: {}", entry.getId());
                    break;
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to fetch default calendar", e);
        }

        userRepository.save(user);
        calendarContext.setAuthorizedClient(client); // Session scoped (legacy support if needed)

        String jwt = jwtTokenUtil.generateToken(user.getId(), user.getEmail(), user.getFullName());
        response.sendRedirect("/chat-ui?token=" + jwt);
    }
}
