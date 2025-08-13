package com.handson.CalenderGPT.view;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
public class HomeController {

    @GetMapping("/")
    public void home(HttpServletResponse response) throws IOException {
        // מפנה לעמוד הבית בפרונט (S3/CloudFront)
        response.sendRedirect("https://calendargpt.org");
    }

    @GetMapping("/chat-ui")
    public void chatPage(HttpServletResponse response) throws IOException {
        // מפנה לעמוד הצ'אט בפרונט
        response.sendRedirect("https://calendargpt.org/app/chat.html");
    }
}
