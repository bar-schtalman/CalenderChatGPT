package com.handson.CalenderGPT.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired private JwtTokenUtil jwtTokenUtil;
    @Autowired private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    // ✅ חייבת להיות רק מתודה אחת! (מאחדים הכל לכאן)
    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String p = req.getServletPath();
        return p.startsWith("/actuator/")      // בריאות פנימי
            || p.equals("/api/health")         // בריאות חיצוני
            || p.startsWith("/api/auth/")      // לוגין/אימות
            || p.startsWith("/oauth2/")
            || p.startsWith("/login/oauth2/")
            || p.startsWith("/api/swagger-ui/")// Swagger UI
            || p.startsWith("/api/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // ❗ אין טוקן? אל תזרוק 401; תן ל-chain להתקדם (נתיבים ציבוריים/לא מוגנים)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        try {
            // ✅ אמת את הטוקן והצב Authentication בקונטקסט
            var authentication = jwtTokenUtil.buildAuthentication(jwt); // ← ודא שקיימת מתודה שיוצרת UsernamePasswordAuthenticationToken עם Authorities
            SecurityContextHolder.getContext().setAuthentication(authentication);

            chain.doFilter(request, response);
        } catch (JwtException ex) {
            jwtAuthenticationEntryPoint.commence(
                request, response,
                new BadCredentialsException("Invalid or expired JWT", ex)
            );
        }
    }
}
