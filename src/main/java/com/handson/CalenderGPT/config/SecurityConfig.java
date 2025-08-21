package com.handson.CalenderGPT.config;

import com.handson.CalenderGPT.jwt.JwtAuthenticationEntryPoint;
import com.handson.CalenderGPT.jwt.JwtRequestFilter;
import com.handson.CalenderGPT.google.oauth.GoogleOAuthSuccessHandler;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final GoogleOAuthSuccessHandler googleOAuthSuccessHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF לא רלוונטי כי אנחנו עובדים JWT
                .csrf(csrf -> csrf.disable())
                // Stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Authentication errors
                .exceptionHandling(e -> e.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                // Authorization rules
                .authorizeHttpRequests(authz -> authz
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
                        .anyRequest().authenticated()
                )
                // OAuth2 login
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(a ->
                                a.authorizationRequestResolver(
                                        new CustomAuthorizationRequestResolver(clientRegistrationRepository)
                                )
                        )
                        .successHandler(googleOAuthSuccessHandler)
                )
                // הוספת פילטר JWT לפני UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public Filter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);

        // 👇 כאן תכניס את הכתובות שאתה באמת רוצה לאפשר (פרונט ב־S3/CloudFront + לוקאלי)
        configuration.setAllowedOrigins(List.of(
                "https://ec2-stage.calendargpt.org",
                "https://calendargpt.runmydocker-app.com",
                "http://localhost:3000"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
