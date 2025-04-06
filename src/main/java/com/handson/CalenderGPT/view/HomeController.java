package com.handson.CalenderGPT.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index"; // Resolves to src/main/resources/templates/index.html
    }

    @GetMapping("/chat-ui")
    public String chatPage() {
        return "chat"; // Resolves to chat.html
    }
}
