package com.handson.CalenderGPT.service;

import com.handson.CalenderGPT.model.ChatGPTRequest;
import com.handson.CalenderGPT.model.ChatGPTResponse;
import com.handson.CalenderGPT.model.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class ChatGPTService {

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String url;

    private final RestTemplate template;

    public ChatGPTService(RestTemplate template) {
        this.template = template;
    }

    public ChatGPTResponse callChatGPT(List<Message> messages) {
        ChatGPTRequest request = new ChatGPTRequest();
        request.setModel(model);
        request.setMessages(messages);
        return template.postForObject(url, request, ChatGPTResponse.class);
    }
}
