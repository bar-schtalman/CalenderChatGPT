package com.handson.CalenderGPT.controller;

import com.handson.CalenderGPT.model.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AuthController {

    @GetMapping("/api/auth/google/login")
    public void handleGoogleLogin(@RequestParam("next") String next, HttpServletResponse response) throws IOException {
        String encoded = URLEncoder.encode(next, StandardCharsets.UTF_8);

        // בונים עוגייה עם SameSite=Lax + Secure + HttpOnly
        ResponseCookie cookie = ResponseCookie.from("POST_LOGIN_NEXT", encoded)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMinutes(2))
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        // מפנה להפעלת OAuth2 של ספרינג
        response.sendRedirect("/api/oauth2/authorization/google");
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
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie clearedCookie = ResponseCookie.from("AUTH_TOKEN", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0) // מחיקת העוגייה
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, clearedCookie.toString());

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // או redirect
    }

}
