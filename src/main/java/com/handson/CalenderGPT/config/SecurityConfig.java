package com.handson.CalenderGPT.config;

import com.handson.CalenderGPT.google.oauth.GoogleOAuthSuccessHandler;
import com.handson.CalenderGPT.jwt.JwtAuthenticationEntryPoint;
import com.handson.CalenderGPT.jwt.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private GoogleOAuthSuccessHandler googleOAuthSuccessHandler;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    /**
     * Tell Spring Security to completely ignore all /actuator/** requests,
     * so they never hit the filter chain.
     */
   @Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .cors(cors -> cors.disable())
      .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
          // Ensure actuator is fully open:
          .requestMatchers("/actuator/**").permitAll()
          // שאר ה־endpoints:
          .requestMatchers(
              "/", "/chat-ui", "/login/**", "/logout", "/oauth2/**",
              "/swagger-ui/**", "/v3/api-docs/**",
              "/favicon.ico", "/static/**",
              "/utils.js","/apiClient.js","/chatHandler.js","/styles.css"
          ).permitAll()
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

    http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
