package com.handson.CalenderGPT.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController
{
    @GetMapping("/auth/status")
    public ResponseEntity<String> getAuthStatus(OAuth2AuthenticationToken auth) {
        if (auth != null) {
            return ResponseEntity.ok("✅ Logged in as " + auth.getPrincipal().getAttribute("email"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ Not logged in");
        }
    }

}
