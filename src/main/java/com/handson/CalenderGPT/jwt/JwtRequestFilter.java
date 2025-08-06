package com.handson.CalenderGPT.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * סינון שמטפל בבדיקת JWT בכל request.
 * מדלג על כל קריאה ל-/actuator/** כדי שלא יחסום את health check.
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. דילוג על actuator endpoints
        String path = request.getServletPath();
        if (path.startsWith("/actuator/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. המשך לוגיקת ה-JWT הרגילה
        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            try {
                jwtTokenUtil.validateToken(jwt);
            } catch (JwtException ex) {
                // אורזים את ה-JwtException לתוך BadCredentialsException
                AuthenticationException authEx = new BadCredentialsException("Invalid or expired JWT", ex);
                jwtAuthenticationEntryPoint.commence(request, response, authEx);
                return;
            }
        }

        // 3. העברת השליטה הלאה
        filterChain.doFilter(request, response);
    }
}
