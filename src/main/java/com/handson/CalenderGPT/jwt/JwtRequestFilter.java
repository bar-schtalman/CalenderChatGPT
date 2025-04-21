package com.handson.CalenderGPT.jwt;

import com.handson.CalenderGPT.model.User;
import com.handson.CalenderGPT.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = null;

        // 🟢 FIRST: Look for Bearer token in Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            System.out.println("Received JWT Token: " + token);  // Log the token
        }

        // 🔵 THEN: Optionally check cookies (if you still want to support them)
        if (token == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("AUTH_TOKEN".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // 🟣 If token found, try to validate and authenticate
        try {
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UUID userId = jwtTokenUtil.getUserIdFromToken(token);
                User user = userRepository.findById(userId).orElse(null);

                if (user != null) {
                    // Set the user authentication token for JWT
                    UsernamePasswordAuthenticationToken jwtAuthToken =
                            new UsernamePasswordAuthenticationToken(user, null, null);

                    // Get existing OAuth2 token from the SecurityContext (if available)
                    Authentication existingOAuth2Auth = SecurityContextHolder.getContext().getAuthentication();
                    if (existingOAuth2Auth instanceof OAuth2AuthenticationToken) {
                        // If OAuth2 authentication token exists, set both JWT and OAuth2 in the context
                        SecurityContextHolder.getContext().setAuthentication(existingOAuth2Auth); // Keep OAuth2 token
                    }

                    // Now set the JWT token as authentication
                    SecurityContextHolder.getContext().setAuthentication(jwtAuthToken);
                }
            }
        } catch (Exception ex) {
            logger.warn("JWT auth failed: " + ex.getMessage());
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }

}
