package com.handson.CalenderGPT.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.model.IntentType;
import com.handson.CalenderGPT.model.ChatGPTRequest;
import com.handson.CalenderGPT.model.ChatGPTResponse;
import com.handson.CalenderGPT.model.Message;
import com.handson.CalenderGPT.model.Event;
import com.handson.CalenderGPT.service.IntentService;
import com.handson.CalenderGPT.service.EventService;
import com.handson.CalenderGPT.context.CalendarContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
public class OpenAIAPIController {

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String url;

    @Autowired
    private IntentService intentService;

    @Autowired
    private EventService eventService;

    @Autowired
    private CalendarContext calendarContext;

    @Autowired
    private RestTemplate template;

    private final List<Message> conversationHistory = new ArrayList<>();
    private Event pendingEvent = null; // To hold the event awaiting confirmation

    @GetMapping("/chat")
    public String chat(@RequestParam("prompt") String prompt) {
        try {
            String extractedJson = intentService.extractDetailsFromPrompt(prompt);

            // Debugging Logs
            System.out.println("🛠 Extracted JSON from ChatGPT:\n" + extractedJson);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(extractedJson);

            String extractedIntent = jsonNode.get("intent").asText();
            System.out.println("Extracted Intent from ChatGPT: " + extractedIntent);

            // Try mapping to IntentType
            IntentType intent;
            switch (extractedIntent.toUpperCase()) {
                case "CREATE":
                    intent = IntentType.CREATE_EVENT;
                    break;
                case "EDIT":
                    intent = IntentType.EDIT_EVENT;
                    break;
                case "DELETE":
                    intent = IntentType.DELETE_EVENT;
                    break;
                case "VIEW":
                    intent = IntentType.VIEW_EVENTS;
                    break;
                default:
                    intent = IntentType.NONE;
            }

            // If it's an event, return extracted details
            if (intent != IntentType.NONE) {
                return "Detected Intent: " + intent.name() + "\n\nExtracted Details:\n" + jsonNode.toPrettyString();
            }

            // If no intent, proceed with regular chat
            return chatWithGPT(prompt);

        } catch (Exception e) {
            System.out.println("❌ Error processing request: " + e.getMessage());
            return "❌ Error: " + e.getMessage() + "\n\nCheck server logs for details.";
        }
    }
    private String chatWithGPT(String prompt) {
        conversationHistory.add(new Message("user", prompt));

        ChatGPTRequest chatGPTRequest = new ChatGPTRequest();
        chatGPTRequest.setModel(model);
        chatGPTRequest.setMessages(conversationHistory);

        ChatGPTResponse chatGPTResponse = template.postForObject(url, chatGPTRequest, ChatGPTResponse.class);
        String assistantReply = chatGPTResponse.getChoices().get(0).getMessage().getContent();

        conversationHistory.add(new Message("assistant", assistantReply));

        return assistantReply;
    }
    //32/






}