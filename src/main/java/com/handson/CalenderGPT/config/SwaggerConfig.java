package com.handson.CalenderGPT.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableSwagger2
public class SwaggerConfig {


    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientID;

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.handson.CalenderGPT.controller"))
                .paths(PathSelectors.any()) // Include all paths
                .build()
                .securitySchemes(Collections.singletonList(oauthSecurityScheme())) // Define OAuth2 scheme
                .securityContexts(Collections.singletonList(securityContext())); // Apply security context to endpoints
    }

    private SecurityScheme oauthSecurityScheme() {
        return new springfox.documentation.builders.OAuthBuilder()
                .name("oauth2")
                .grantTypes(Collections.singletonList(
                        new springfox.documentation.service.AuthorizationCodeGrant(
                                new springfox.documentation.service.TokenRequestEndpoint(
                                        "https://accounts.google.com/o/oauth2/auth",
                                        clientID,
                                        clientSecret),
                                new springfox.documentation.service.TokenEndpoint(
                                        "https://oauth2.googleapis.com/token", "access_token"))))
                .build();
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(defaultAuth())
                .forPaths(PathSelectors.regex("/calendar/.*")) // Apply OAuth2 only to calendar endpoints
                .build();
    }

    private List<SecurityReference> defaultAuth() {
        return Collections.singletonList(new SecurityReference("oauth2", new AuthorizationScope[]{}));
    }
}
