package com.handson.CalenderGPT.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class SwaggerConfig {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CalendarGPT API")
                        .version("1.0")
                        .description("API docs for CalendarGPT with Google OAuth and OpenAI")
                        .contact(new Contact().name("Your Name").email("your@email.com"))
                )
                .addSecurityItem(new SecurityRequirement().addList("oauth2"))
                .components(new Components()
                        .addSecuritySchemes("oauth2",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.OAUTH2)
                                        .flows(new OAuthFlows()
                                                .authorizationCode(new OAuthFlow()
                                                        .authorizationUrl("https://accounts.google.com/o/oauth2/auth")
                                                        .tokenUrl("https://oauth2.googleapis.com/token")
                                                        .scopes(new io.swagger.v3.oas.models.security.Scopes()
                                                                .addString("openid", "Access your identity")
                                                                .addString("profile", "Access your basic profile")
                                                                .addString("email", "Access your email")
                                                                .addString("https://www.googleapis.com/auth/calendar", "Access Google Calendar")
                                                                .addString("https://www.googleapis.com/auth/contacts.readonly", "Read your Google Contacts")
                                                        )
                                                )
                                        )

                        )
                );
    }
}
