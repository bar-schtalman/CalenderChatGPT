package com.handson.CalenderGPT.config;

import com.handson.CalenderGPT.google.oauth.GoogleOAuthSuccessHandler;
import com.handson.CalenderGPT.jwt.JwtAuthenticationEntryPoint;
import com.handson.CalenderGPT.jwt.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Autowired private JwtRequestFilter jwtRequestFilter;
    @Autowired private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @Autowired private GoogleOAuthSuccessHandler googleOAuthSuccessHandler;
    @Autowired private ClientRegistrationRepository clientRegistrationRepository;

    // אל תעביר דרך שרשרת הסקיוריטי בכלל (טוב ל-health פנימי)
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/actuator/**");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // חשוב: לא לכבות CORS; נשתמש בקונפיג שלמטה
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                    // פטור מ-CSRF לנתיבי ציבור
                    "/api/auth/**",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/api/swagger-ui/**",
                    "/api/v3/api-docs/**",
                    "/api/health"
            ))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // *** נתיבים ציבוריים (ללא התחברות) ***
                .requestMatchers(
                    "/", "/chat-ui", "/login/**", "/logout",
                    "/favicon.ico", "/static/**",
                    "/utils.js", "/apiClient.js", "/chatHandler.js", "/styles.css",

                    // OAuth + Swagger + Health תחת /api
                    "/api/auth/**",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/api/swagger-ui/**",
                    "/api/v3/api-docs/**",
                    "/api/health"
                ).permitAll()
                // Preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // כל השאר – דורש התחברות
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authz ->
                    authz.authorizationRequestResolver(
                        new CustomAuthorizationRequestResolver(clientRegistrationRepository)
                    )
                )
                .successHandler(googleOAuthSuccessHandler)
            );

        // ה-JWT filter חייב להגיע לפני UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // CORS מאחורי CloudFront (אותו דומיין, אבל טוב שיהיה)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of(
            "https://ec2-stage.calendargpt.org",
            "https://calendargpt.org",
            "https://www.calendargpt.org"
        ));
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true);
        c.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", c); // או "/api/**" אם אתה רוצה להגביל
        return src;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
