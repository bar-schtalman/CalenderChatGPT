package com.handson.CalenderGPT.controller;

import com.handson.CalenderGPT.model.IntentType;
import com.handson.CalenderGPT.model.ChatGPTRequest;
import com.handson.CalenderGPT.model.ChatGPTResponse;
import com.handson.CalenderGPT.model.Message;
import com.handson.CalenderGPT.service.IntentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@RestController
public class OpenAIAPIController {

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String url;

    @Autowired
    private IntentService intentService;

    @Autowired
    private RestTemplate template;

    private final List<Message> conversationHistory = new ArrayList<>();

    @GetMapping("/chat")
    public String chat(@RequestParam("prompt") String prompt) {
        try {
            IntentType intent = intentService.detectIntent(prompt);
            String intentMessage = "Detected Intent: " + intent.name() + "\n\n";

            if (intent != IntentType.NONE) {
                String extractedDetails = intentService.extractDetailsFromPrompt(prompt, intent);
                return intentMessage + extractedDetails;
            }

            conversationHistory.add(new Message("user", prompt));

            ChatGPTRequest chatGPTRequest = new ChatGPTRequest();
            chatGPTRequest.setModel(model);
            chatGPTRequest.setMessages(conversationHistory);

            ChatGPTResponse chatGPTResponse = template.postForObject(url, chatGPTRequest, ChatGPTResponse.class);
            String assistantReply = chatGPTResponse.getChoices().get(0).getMessage().getContent();

            conversationHistory.add(new Message("assistant", assistantReply));
            return intentMessage + assistantReply;

        } catch (HttpClientErrorException.TooManyRequests e) {
            return "Error: You have exceeded your API usage quota. Please check your OpenAI plan.";
        } catch (Exception e) {
            return "An unexpected error occurred. Please try again later. " + e.toString();
        }
    }
}
