package com.handson.CalenderGPT.config;

import com.handson.CalenderGPT.google.oauth.GoogleOAuthSuccessHandler;
import com.handson.CalenderGPT.jwt.JwtAuthenticationEntryPoint;
import com.handson.CalenderGPT.jwt.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final GoogleOAuthSuccessHandler googleOAuthSuccessHandler;

    @Autowired
    public SecurityConfig(
            JwtRequestFilter jwtRequestFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            GoogleOAuthSuccessHandler googleOAuthSuccessHandler
    ) {
        this.jwtRequestFilter = jwtRequestFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.googleOAuthSuccessHandler = googleOAuthSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS
                .cors(Customizer.withDefaults())

                // CSRF כבוי: אנחנו סטייטלס עם JWT
                .csrf(csrf -> csrf.disable())

                // 401 במקום redirect
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // סטייטלס
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // הרשאות
                .authorizeHttpRequests(auth -> auth
                        // לאפשר Preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // נתיבים ציבוריים (פתוחים):
                        .requestMatchers(
                                "/api/oauth2/authorization/**",
                                "/api/login/oauth2/**",
                                "/api/auth/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/api/swagger-ui/**",
                                "/api/v3/api-docs/**",
                                "/api/health",
                                "/actuator/health"
                        ).permitAll()

                        // כל היתר דורש אימות
                        .anyRequest().authenticated()
                )

                // OAuth2 Login (אם בשימוש; אפשר להשאיר מינימלי)
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(ae -> ae.baseUri("/api/oauth2/authorization"))
                        .redirectionEndpoint(re -> re.baseUri("/api/login/oauth2/code/*"))
                        .successHandler(googleOAuthSuccessHandler)
                );

        // JWT לפני UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        // עדכן את הרשימה לפי הדומיינים שלך (כולל www)
        c.setAllowedOrigins(List.of(
                "https://ec2-stage.calendargpt.org",
                "https://calendargpt.org",
                "https://www.calendargpt.org"
        ));
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setExposedHeaders(List.of("Authorization","Location","Set-Cookie"));
        c.setAllowCredentials(true);
        c.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/api/**", c);
        src.registerCorsConfiguration("/oauth2/**", c);
        src.registerCorsConfiguration("/login/oauth2/**", c);
        src.registerCorsConfiguration("/actuator/**", c);
        src.registerCorsConfiguration("/api/swagger-ui/**", c);
        src.registerCorsConfiguration("/api/v3/api-docs/**", c);
        src.registerCorsConfiguration("/api/oauth2/**", c);
        src.registerCorsConfiguration("/api/login/oauth2/**", c);
        return src;
    }

}