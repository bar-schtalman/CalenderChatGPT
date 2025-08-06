package com.handson.CalenderGPT.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
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
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                jwtTokenUtil.validateToken(jwt);
            } catch (JwtException ex) {
                // אם ה-JWT לא תקין או פג תוקפו → החזרת 401
                jwtAuthenticationEntryPoint.commence(request, response, ex);
                return;
            }
        }
        // אם אין Header, נעבור הלאה – הביטחון יטפל בהמשך בבקשות מאובטחות

        // 3. העברת השליטה הלאה
        filterChain.doFilter(request, response);
    }
}
