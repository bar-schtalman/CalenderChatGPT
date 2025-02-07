package com.handson.CalenderGPT.controller;

import com.handson.CalenderGPT.model.ChatGPTRequest;
import com.handson.CalenderGPT.model.ChatGPTResponse;
import com.handson.CalenderGPT.model.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

    // Injecting RestTemplate from the configuration class
    @Autowired
    private RestTemplate template;

    // Maintain the conversation across calls (for demo).
    // In production, you'd store this per user/session.
    private final List<Message> conversationHistory = new ArrayList<>();

    @GetMapping("/chat")
    public String chat(@RequestParam("prompt") String prompt) {
        try {
            // 1. Add user message to conversation history
            conversationHistory.add(new Message("user", prompt));

            // 2. Build the ChatGPTRequest with ALL messages so far
            ChatGPTRequest chatGPTRequest = new ChatGPTRequest();
            chatGPTRequest.setModel("gpt-3.5-turbo"); // or use your 'model' variable
            chatGPTRequest.setMessages(conversationHistory);

            // 3. Call the chat completions endpoint
            ChatGPTResponse chatGPTResponse = template.postForObject(
                    "https://api.openai.com/v1/chat/completions",
                    chatGPTRequest,
                    ChatGPTResponse.class
            );

            // 4. Extract the assistant's response
            String assistantReply = chatGPTResponse
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

            // 5. Add the assistant's reply to the conversation
            conversationHistory.add(new Message("assistant", assistantReply));

            // 6. Return the assistant's response
            return assistantReply;
        } catch (HttpClientErrorException.TooManyRequests e) {
            return "Error: You have exceeded your API usage quota. Please check your OpenAI plan.";
        } catch (Exception e) {
            return "An unexpected error occurred. Please try again later." + e.toString();
        }
    }


}
