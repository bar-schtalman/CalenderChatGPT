package com.handson.CalenderGPT.jwt;

import com.handson.CalenderGPT.jwt.JwtAuthenticationEntryPoint;
import com.handson.CalenderGPT.jwt.JwtTokenUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String p = req.getServletPath();
        String m = req.getMethod();

        if ("OPTIONS".equalsIgnoreCase(m)) return true;

        return p.startsWith("/actuator/")
                || p.equals("/api/health")
                || p.startsWith("/api/auth/")
                || p.startsWith("/oauth2/")
                || p.startsWith("/login/oauth2/")
                || p.startsWith("/api/swagger-ui/")
                || p.startsWith("/api/v3/api-docs")
                || p.equals("/swagger-ui.html")
                || p.startsWith("/swagger-ui/")
                || p.startsWith("/v3/api-docs")
                || p.startsWith("/api/oauth2/authorization")
                || p.startsWith("/api/login/oauth2/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // אימות הטוקן
                UsernamePasswordAuthenticationToken authentication =
                        jwtTokenUtil.buildAuthentication(token);

                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException e) {
                // טוקן לא תקף
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT: " + e.getMessage());
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
