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
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getServletPath();

        // 1) לדלג על נתיבים ציבוריים ו-OPTIONS
        boolean isPublic =
                "OPTIONS".equalsIgnoreCase(request.getMethod()) ||
                        path.equals("/api/health") ||
                        path.startsWith("/api/swagger-ui") ||
                        path.startsWith("/api/v3/api-docs") ||
                        path.equals("/api/auth/google/login") ||
                        path.startsWith("/login/oauth2/") ||
                        path.startsWith("/oauth2/") ||
                        path.startsWith("/actuator/health"); // אם באמת פתוח אצלך

        if (isPublic) {
            chain.doFilter(request, response);
            return;
        }

        // 2) אם אין Authorization Bearer — ממשיכים בלי לאמת (לא מחזירים 401 פה!)
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            // TODO: לאמת את ה-JWT, להוציא user/authorities, ולהגדיר Authentication ב-SecurityContext
            // SecurityContextHolder.getContext().setAuthentication(authentication);

            chain.doFilter(request, response);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            // טוקן לא תקין: לא שולחים 401 מפה. מנקים הקשר וממשיכים.
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
            // אם הנתיב מוגן, Spring יחזיר 401 דרך ה-EntryPoint שהגדרת.
        }
    }

}
