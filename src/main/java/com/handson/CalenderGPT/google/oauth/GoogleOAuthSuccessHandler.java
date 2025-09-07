package com.handson.CalenderGPT.google.oauth;

import com.handson.CalenderGPT.context.CalendarContext;
import com.handson.CalenderGPT.model.User;
import com.handson.CalenderGPT.service.UserService;
import jakarta.servlet.http.Cookie;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService clientService;
    private final UserService userService;
    private final CalendarContext calendarContext;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendError(SC_UNAUTHORIZED, "Authentication failed");
            return;
        }

        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );

        if (client == null || client.getAccessToken() == null) {
            response.sendError(SC_UNAUTHORIZED, "Missing authorized client");
            return;
        }

        log.info("✅ Google OAuth2 login successful for {}", oauthToken.getName());

        // 1. עדכון/שמירת המשתמש במסד
        User user = userService.handleOAuthLogin(oauthToken, client);

        // 2. שמירת ה־AuthorizedClient בקונטקסט
        calendarContext.setAuthorizedClient(client);

        // 3. לוג על refresh_token אם קיים (לא צריך לשמור שוב – זה כבר נעשה ב־handleOAuthLogin)
        if (client.getRefreshToken() != null) {
            log.info("🔑 Received Google refresh_token for {}", user.getEmail());
        } else {
            log.warn("⚠ No Google refresh_token received for {}", user.getEmail());
        }

        // 4. יצירת JWT פנימי
        String jwtToken = userService.createJwtFor(user);

        // 5. טיפול ב־redirect (כולל קריאה ל־POST_LOGIN_NEXT אם קיים)
        String redirectUrl = "https://calendargpt.org/app/chat.html"; // fallback
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("POST_LOGIN_NEXT".equals(c.getName())) {
                    try {
                        redirectUrl = URLDecoder.decode(c.getValue(), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        redirectUrl = c.getValue();
                    }
                }
            }
        }

        // מחיקת העוגייה
        Cookie removeCookie = new Cookie("POST_LOGIN_NEXT", "");
        removeCookie.setPath("/");
        removeCookie.setMaxAge(0);
        response.addCookie(removeCookie);

        // הוספת ה־JWT ל־redirect
        redirectUrl += (redirectUrl.contains("?") ? "&" : "?") + "token=" + jwtToken;
        response.sendRedirect(redirectUrl);
    }
}
