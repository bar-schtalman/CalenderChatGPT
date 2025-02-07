package com.handson.CalenderGPT.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()  // Disable CSRF for API endpoints, common for OAuth2 secured APIs
                .authorizeRequests()
                .antMatchers(
                        "/",
                        "/error",
                        "/swagger-ui/**",  // Allow Swagger UI access
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/auth/google",    // Allow Google auth endpoints without security
                        "/auth/status",
                        "/logout"
                ).permitAll()
                .antMatchers("/api/google-calendar/**").authenticated()  // Secure calendar API endpoints
                .anyRequest().authenticated()  // Secure all other requests by default
                .and()
                .oauth2Login()
                .loginPage("/auth/google")  // Redirect users to Google for login
                .and()
                .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/swagger-ui.html")  // Redirect to Swagger after logout
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll();
    }
}
