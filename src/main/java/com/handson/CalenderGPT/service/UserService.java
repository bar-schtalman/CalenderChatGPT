package com.handson.CalenderGPT.service;

import com.google.api.services.calendar.Calendar;
import com.handson.CalenderGPT.google.calendar.GoogleCalendarProvider;
import com.handson.CalenderGPT.jwt.JwtTokenUtil;
import com.handson.CalenderGPT.model.User;
import com.handson.CalenderGPT.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repo;
    private final GoogleCalendarProvider calendarProvider;
    private final JwtTokenUtil jwtUtil;

    @Transactional
    public User handleOAuthLogin(OAuth2AuthenticationToken oauthToken, OAuth2AuthorizedClient client) {
        // 1. Extract attributes
        Map<String, Object> attrs = oauthToken.getPrincipal().getAttributes();
        String email = (String) attrs.get("email");
        String fullName = (String) attrs.get("name");
        String first = (String) attrs.getOrDefault("given_name", fullName);
        String last = (String) attrs.getOrDefault("family_name", "");

        // 2. Find or create user
        User user = repo.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setFullName(fullName);
            u.setFirstName(first);
            u.setLastName(last);
            return u;
        });
        user.setJwtIssuedAt(Instant.now());

        // 3. Store refresh token (if exists)
        if (client.getRefreshToken() != null) {
            user.setGoogleRefreshToken(client.getRefreshToken().getTokenValue());
        }

        // 4. Fetch primary calendar ID using User + GoogleCalendarProvider
        String calId = fetchPrimaryCalendarId(user);
        if (calId != null) {
            user.setDefaultCalendarId(calId);
        }

        // 5. Save & return
        User saved = repo.saveAndFlush(user);

        System.out.println("💾 User " + saved.getEmail() + " saved with id=" + saved.getId() +
                " saved at " + saved.getJwtIssuedAt());
        return saved;
    }

    private String fetchPrimaryCalendarId(User user) {
        try {
            Calendar calSvc = calendarProvider.getCalendarClient(user);
            for (var entry : calSvc.calendarList().list().execute().getItems()) {
                if (Boolean.TRUE.equals(entry.getPrimary())) {
                    return entry.getId();
                }
            }
        } catch (Exception e) {
            // log.warn("Failed to fetch default calendar", e);
        }
        return null;
    }

    public String createJwtFor(User user) {
        return jwtUtil.generateToken(user.getId(), user.getEmail(), user.getFullName());
    }
}
