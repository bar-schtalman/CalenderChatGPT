package com.handson.CalenderGPT.google.oauth;

import com.handson.CalenderGPT.context.CalendarContext;
import com.handson.CalenderGPT.model.User;
import com.handson.CalenderGPT.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService clientService;
    private final UserService userService;
    private final CalendarContext calendarContext;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendError(SC_UNAUTHORIZED, "Authentication failed");
            return;
        }


        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
        if (client == null || client.getAccessToken() == null) {
            response.sendError(SC_UNAUTHORIZED, "Missing authorized client");
            return;
        }

        log.info("✅ Google OAuth2 login successful for {}", oauthToken.getName());

        // Delegate all user upsert, refresh-token storage, calendar lookup, and JWT creation
        User user = userService.handleOAuthLogin(oauthToken, client);

        // Preserve the OAuth2AuthorizedClient in session context if needed downstream
        calendarContext.setAuthorizedClient(client);

        // Generate the JWT and redirect
        String jwt = userService.createJwtFor(user);
        response.sendRedirect("/chat-ui?token=" + jwt);
    }
}
