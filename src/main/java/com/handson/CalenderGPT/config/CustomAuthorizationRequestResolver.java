package com.handson.CalenderGPT.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest.Builder;

import java.util.HashMap;
import java.util.Map;

public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

    public CustomAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        // "/oauth2/authorization" is the default base URI for OAuth2 logins
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest defaultRequest = defaultResolver.resolve(request);
        return customizeAuthorizationRequest(defaultRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest defaultRequest = defaultResolver.resolve(request, clientRegistrationId);
        return customizeAuthorizationRequest(defaultRequest);
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(OAuth2AuthorizationRequest defaultRequest) {
        if (defaultRequest == null) return null;

        Map<String, Object> extraParams = new HashMap<>(defaultRequest.getAdditionalParameters());

        // 👇 Ensure we get a refresh token (needed for background API access)
        extraParams.put("access_type", "offline");
        extraParams.put("prompt", "consent");

        Builder builder = OAuth2AuthorizationRequest.from(defaultRequest).additionalParameters(extraParams);

        return builder.build();
    }
}
