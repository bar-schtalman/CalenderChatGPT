package com.handson.CalenderGPT.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.model.IntentType;
import com.handson.CalenderGPT.model.Message;
import com.handson.CalenderGPT.service.IntentService;
import com.handson.CalenderGPT.service.EventService;
import com.handson.CalenderGPT.context.CalendarContext;
import com.handson.CalenderGPT.service.ChatGPTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class OpenAIAPIController {

    @Autowired
    private IntentService intentService;

    @Autowired
    private EventService eventService;

    @Autowired
    private CalendarContext calendarContext;

    @Autowired
    private ChatGPTService chatGPTService;

    private final List<Message> conversationHistory = new ArrayList<>();

    @GetMapping("/chat")
    public String chat(@RequestParam("prompt") String prompt) {
        try {
            String extractedJson = intentService.extractDetailsFromPrompt(prompt);
            System.out.println("🛠 Extracted JSON from ChatGPT:\n" + extractedJson);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(extractedJson);

            IntentType intent = mapIntentType(jsonNode.get("intent").asText());
            System.out.println("Extracted Intent from ChatGPT: " + intent);

            if (intent == IntentType.VIEW_EVENTS) {
                return handleViewEvents(jsonNode);
            }
            if (intent != IntentType.NONE) {
                return "Detected Intent: " + intent.name() + "\n\nExtracted Details:\n" + jsonNode.toPrettyString();
            }
            return chatWithGPT(prompt);
        } catch (Exception e) {
            System.out.println("❌ Error processing request: " + e.getMessage());
            return "❌ Error: " + e.getMessage() + "\n\nCheck server logs for details.";
        }
    }

    // Maps the extracted intent string to our IntentType enum.
    private IntentType mapIntentType(String extractedIntent) {
        switch (extractedIntent.toUpperCase()) {
            case "CREATE":
                return IntentType.CREATE_EVENT;
            case "EDIT":
                return IntentType.EDIT_EVENT;
            case "DELETE":
                return IntentType.DELETE_EVENT;
            case "VIEW":
                return IntentType.VIEW_EVENTS;
            default:
                return IntentType.NONE;
        }
    }

    // Handles VIEW intent: builds a response listing events with their id, date, and summary.
    private String handleViewEvents(JsonNode jsonNode) throws Exception {
        String start = jsonNode.get("start").asText();
        String end = jsonNode.get("end").asText();
        String calendarId = calendarContext.getCalendarId();

        List<Map<String, String>> events = eventService.getEventsInDateRange(calendarId, start, end);
        if (events.isEmpty()) {
            return "No events found between " + start + " and " + end + ".";
        }

        StringBuilder response = new StringBuilder("Events between " + start + " and " + end + ":\n");
        for (Map<String, String> event : events) {
            response.append("Event ID: ").append(event.get("id"))
                    .append(" - Date: ").append(event.get("start"))
                    .append(" - Summary: ").append(event.get("summary"))
                    .append("\n");
        }
        return response.toString();
    }

    // Delegates non-event prompts to ChatGPT using the shared service.
    private String chatWithGPT(String prompt) {
        conversationHistory.add(new Message("user", prompt));
        List<Message> messages = new ArrayList<>(conversationHistory);
        String assistantReply = chatGPTService.callChatGPT(messages)
                .getChoices().get(0).getMessage().getContent();
        conversationHistory.add(new Message("assistant", assistantReply));
        return assistantReply;
    }
}
