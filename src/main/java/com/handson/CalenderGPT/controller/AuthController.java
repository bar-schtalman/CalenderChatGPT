package com.handson.CalenderGPT.controller;

import com.handson.CalenderGPT.model.User;
import com.handson.CalenderGPT.model.UserSession;
import com.handson.CalenderGPT.repository.UserSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.*;

@RestController
public class AuthController {

    private final UserSessionRepository userSessionRepository;

    public AuthController(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }

    @GetMapping("/auth/status")
    public ResponseEntity<String> getAuthStatus(OAuth2AuthenticationToken auth) {
        if (auth != null) {
            return ResponseEntity.ok("✅ Logged in as " + auth.getPrincipal().getAttribute("email"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ Not logged in");
        }
    }

    @GetMapping("/api/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(HttpSession session) {
        String sessionId = session.getId();
        Optional<UserSession> sessionOpt = userSessionRepository.findBySessionId(sessionId);

        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Session not found"));
        }

        User user = sessionOpt.get().getUser();
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());

        return ResponseEntity.ok(response);
    }
}
