package com.handson.CalenderGPT.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired private JwtTokenUtil jwtTokenUtil;
    @Autowired private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String p = req.getServletPath();
        String m = req.getMethod();

        // Preflight תמיד עובר
        if ("OPTIONS".equalsIgnoreCase(m)) return true;

        // נתיבים ציבוריים – חייבים לשקף 1:1 את מה שב-SecurityConfig (permitAll)
        return p.startsWith("/actuator/")           // למשל /actuator/health
            || p.equals("/api/health")
            || p.startsWith("/api/auth/")           // כל /api/auth/**
            || p.startsWith("/oauth2/")
            || p.startsWith("/login/oauth2/")
            || p.startsWith("/api/swagger-ui/")
            || p.startsWith("/api/v3/api-docs")
            // (אופציונלי, לשימור הכתובות ההיסטוריות)
            || p.equals("/swagger-ui.html")
            || p.startsWith("/swagger-ui/")
            || p.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // בשלב הזה אנחנו יודעים שזה **לא** נתיב ציבורי (אחרת shouldNotFilter היה מחזיר true)
        // לכן, אם אין Authorization: Bearer – פשוט נעביר הלאה בלי לאמת (ונשאיר ל-Spring לקבוע 401 על נתיב מוגן).
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            // TODO: לאמת את ה-JWT ולבנות Authentication
            // Authentication authentication = buildAuthFromToken(token);
            // SecurityContextHolder.getContext().setAuthentication(authentication);

            chain.doFilter(request, response);
        } catch (RuntimeException ex) {
            // טוקן לא תקין – מנקים הקשר; Spring יחזיר 401 דרך ה-EntryPoint אם הנתיב דורש אימות
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
        }
    }
}
