package com.handson.CalenderGPT.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeRequests()
                .antMatchers(
                        "/",                         // root
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v2/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/auth/google",
                        "/auth/status",
                        "/api/google-calendar/**",
                        "/chat",                    // AI chat endpoint
                        "/logout",

                        // ✅ allow static resources
                        "/styles.css",
                        "/script.js",
                        "/static/**"                // allow everything under static folder
                ).permitAll()
                .anyRequest().authenticated()
                .and()
                .oauth2Login()
                .loginPage("/auth/google")
                .and()
                .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/swagger-ui.html")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll();
    }


}
