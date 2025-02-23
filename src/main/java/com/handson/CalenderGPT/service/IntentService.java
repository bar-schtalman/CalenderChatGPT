package com.handson.CalenderGPT.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.model.ChatGPTRequest;
import com.handson.CalenderGPT.model.ChatGPTResponse;
import com.handson.CalenderGPT.model.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class IntentService {

    @Value("${openai.model}")
    private String model;

    // Remove the url and RestTemplate injection if not needed here
    // since we'll use ChatGPTService instead.

    private final ChatGPTService chatGPTService;
    String today = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);

    public IntentService(ChatGPTService chatGPTService) {
        this.chatGPTService = chatGPTService;
    }

    public String extractDetailsFromPrompt(String prompt) {
        String extractionPrompt = "Analyze the following text and determine if it represents an event-related request (Create, Edit, Delete, View). " +
                "If it is event-related, return structured JSON with fields: " +
                "\"intent\" (CREATE, EDIT, DELETE, VIEW), \"summary\", \"description\", \"start\", \"end\", \"location\". " +
                "Ensure \"start\" and \"end\" are in ISO 8601 format (YYYY-MM-DDTHH:MM:SS.SSSZ). " +
                "If \"end\" is not provided, set it to 1 hour after \"start\". " +
                "Consider that today's date is " + today + ". " +
                "If the provided \"start\" date is in the past relative to today's date, adjust it to the next valid occurrence (for example, if the event is annual, use the same day and time next year). " +
                "If the intent is NOT event-related, return a simple JSON object: {\"intent\": \"NONE\", \"message\": \"response text\"}. " +
                "Do NOT wrap the response in markdown code blocks or triple backticks (`). " +
                "Text: \"" + prompt + "\"";


        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", extractionPrompt));
        ChatGPTResponse response = chatGPTService.callChatGPT(messages);
        String rawResponse = response.getChoices().get(0).getMessage().getContent().trim();

        System.out.println("🛠 Raw Response from ChatGPT:\n" + rawResponse);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode;
        try {
            jsonNode = mapper.readTree(rawResponse);
        } catch (Exception e) {
            System.out.println("⚠️ Raw response was not valid JSON, treating as plain text.");
            jsonNode = mapper.createObjectNode()
                    .put("intent", "NONE")
                    .put("message", rawResponse);
        }
        return jsonNode.toString();
    }
}
