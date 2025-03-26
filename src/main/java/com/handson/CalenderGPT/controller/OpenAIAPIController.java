package com.handson.CalenderGPT.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.model.Event;
import com.handson.CalenderGPT.model.IntentType;
import com.handson.CalenderGPT.model.Message;
import com.handson.CalenderGPT.service.ChatGPTService;
import com.handson.CalenderGPT.service.EventService;
import com.handson.CalenderGPT.service.IntentService;
import com.handson.CalenderGPT.context.CalendarContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
public class OpenAIAPIController {

    private static final DateTimeFormatter OUTPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter OUTPUT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

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
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(extractedJson);

            IntentType intent = mapIntentType(jsonNode.get("intent").asText());

            if (intent == IntentType.VIEW_EVENTS) {
                return handleViewEvents(jsonNode);
            }
            if (intent == IntentType.CREATE_EVENT) {
                return handleCreateEvent(jsonNode);
            }
            if (intent != IntentType.NONE) {
                Map<String, String> intentMsg = new HashMap<>();
                intentMsg.put("role", "ai");
                intentMsg.put("content", "Detected Intent: " + intent.name() + "\n\nExtracted Details:\n" + jsonNode.toPrettyString());
                return mapper.writeValueAsString(Collections.singletonList(intentMsg));
            }
            return chatWithGPT(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("role", "ai");
            error.put("content", "❌ Error: " + e.getMessage());
            try {
                return new ObjectMapper().writeValueAsString(Collections.singletonList(error));
            } catch (Exception ex) {
                return "[{\"role\":\"ai\",\"content\":\"Fatal error occurred\"}]";
            }
        }
    }

    private IntentType mapIntentType(String extractedIntent) {
        switch (extractedIntent.toUpperCase()) {
            case "CREATE": return IntentType.CREATE_EVENT;
            case "EDIT": return IntentType.EDIT_EVENT;
            case "DELETE": return IntentType.DELETE_EVENT;
            case "VIEW": return IntentType.VIEW_EVENTS;
            default: return IntentType.NONE;
        }
    }

    private String handleViewEvents(JsonNode jsonNode) throws Exception {
        String start = jsonNode.get("start").asText();
        String end = jsonNode.get("end").asText();
        String calendarId = calendarContext.getCalendarId();

        List<Map<String, String>> events = eventService.getEventsInDateRange(calendarId, start, end);
        List<Map<String, String>> responseList = new ArrayList<>();

        if (events.isEmpty()) {
            Map<String, String> msg = new HashMap<>();
            msg.put("role", "ai");
            msg.put("content", "No events found between " + start + " and " + end + ".");
            responseList.add(msg);
        } else {
            for (Map<String, String> event : events) {
                Map<String, String> e = new HashMap<>();
                e.put("role", "event");
                e.put("summary", event.get("summary"));
                e.put("date", event.get("start").split(" ")[0]);
                e.put("time", event.get("start").split(" ")[1] + " - " + event.get("end").split(" ")[1]);
                e.put("location", event.getOrDefault("location", "No location"));
                e.put("calendarId", calendarId);
                e.put("id", event.get("id"));
                responseList.add(e);
            }
        }

        return new ObjectMapper().writeValueAsString(responseList);
    }

    private String handleCreateEvent(JsonNode jsonNode) throws Exception {
        Event event = parseEventDetails(jsonNode);
        String calendarId = calendarContext.getCalendarId();
        eventService.createEvent(calendarId, event);

        String eventDate = event.getStart().format(OUTPUT_DATE_FORMATTER);
        String start = event.getStart().format(OUTPUT_TIME_FORMATTER);
        String end = event.getEnd().format(OUTPUT_TIME_FORMATTER);

        Map<String, String> response = new HashMap<>();
        response.put("role", "event");
        response.put("summary", event.getSummary());
        response.put("date", eventDate);
        response.put("time", start + " - " + end);
        response.put("calendarId", calendarId);
        response.put("id", event.getId());

        return new ObjectMapper().writeValueAsString(Collections.singletonList(response));
    }

    private Event parseEventDetails(JsonNode jsonNode) {
        Event event = new Event();
        try {
            event.setId(jsonNode.has("id") ? jsonNode.get("id").asText() : "N/A");
            event.setSummary(jsonNode.get("summary").asText("No Title"));
            event.setDescription(jsonNode.get("description").asText(""));
            event.setLocation(jsonNode.get("location").asText(""));

            String startDateTime = jsonNode.get("start").asText();
            String endDateTime = jsonNode.get("end").asText();

            if (startDateTime != null && !startDateTime.isEmpty()) {
                event.setStart(LocalDateTime.parse(startDateTime, ISO_FORMATTER));
            } else {
                event.setStart(LocalDateTime.now());
            }

            if (endDateTime != null && !endDateTime.isEmpty()) {
                event.setEnd(LocalDateTime.parse(endDateTime, ISO_FORMATTER));
            } else {
                event.setEnd(event.getStart().plusHours(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
            event.setId("N/A");
            event.setSummary("Default Event");
            event.setDescription("No Description");
            event.setLocation("No Location");
            event.setStart(LocalDateTime.now());
            event.setEnd(LocalDateTime.now().plusHours(1));
        }
        return event;
    }

    private String chatWithGPT(String prompt) {
        conversationHistory.add(new Message("user", prompt));
        List<Message> messages = new ArrayList<>(conversationHistory);
        String assistantReply = chatGPTService.callChatGPT(messages).getChoices().get(0).getMessage().getContent();
        assistantReply = assistantReply.replaceAll("\\n+", " ");
        conversationHistory.add(new Message("assistant", assistantReply));

        Map<String, String> response = new HashMap<>();
        response.put("role", "ai");
        response.put("content", assistantReply);

        try {
            return new ObjectMapper().writeValueAsString(Collections.singletonList(response));
        } catch (Exception e) {
            return "[{\"role\":\"ai\",\"content\":\"Error generating reply\"}]";
        }
    }
}