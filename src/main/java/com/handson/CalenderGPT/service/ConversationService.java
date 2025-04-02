package com.handson.CalenderGPT.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.context.CalendarContext;
import com.handson.CalenderGPT.model.Event;
import com.handson.CalenderGPT.model.IntentType;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ConversationService {

    private final IntentService intentService;
    private final EventService eventService;
    private final CalendarContext calendarContext;
    private final ChatGPTService chatGPTService;

    private final List<ChatMessage> conversationHistory = new ArrayList<>();
    private static final DateTimeFormatter OUTPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter OUTPUT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public ConversationService(IntentService intentService,
                               EventService eventService,
                               CalendarContext calendarContext,
                               ChatGPTService chatGPTService) {
        this.intentService = intentService;
        this.eventService = eventService;
        this.calendarContext = calendarContext;
        this.chatGPTService = chatGPTService;
    }

    public String handlePrompt(String prompt) {
        try {
            String extractedJson = intentService.extractDetailsFromPrompt(prompt);
            JsonNode jsonNode = new ObjectMapper().readTree(extractedJson);

            IntentType intent = mapIntentType(jsonNode.get("intent").asText());

            return switch (intent) {
                case VIEW_EVENTS -> handleViewEvents(jsonNode);
                case CREATE_EVENT -> handleCreateEvent(jsonNode);
                case EDIT_EVENT, DELETE_EVENT -> handleIntentPreview(intent, jsonNode);
                case NONE -> chatWithGPT(prompt);
            };
        } catch (Exception e) {
            e.printStackTrace();
            return buildErrorResponse("❌ Error: " + e.getMessage());
        }
    }

    private IntentType mapIntentType(String extractedIntent) {
        return switch (extractedIntent.toUpperCase()) {
            case "CREATE" -> IntentType.CREATE_EVENT;
            case "EDIT" -> IntentType.EDIT_EVENT;
            case "DELETE" -> IntentType.DELETE_EVENT;
            case "VIEW" -> IntentType.VIEW_EVENTS;
            default -> IntentType.NONE;
        };
    }

    private String handleIntentPreview(IntentType intent, JsonNode jsonNode) throws Exception {
        Map<String, String> intentMsg = new HashMap<>();
        intentMsg.put("role", "ai");
        intentMsg.put("content", "Detected Intent: " + intent.name() + "\n\nExtracted Details:\n" + jsonNode.toPrettyString());
        return new ObjectMapper().writeValueAsString(Collections.singletonList(intentMsg));
    }

    private String handleViewEvents(JsonNode jsonNode) throws Exception {
        String start = jsonNode.get("start").asText();
        String end = jsonNode.get("end").asText();
        String calendarId = calendarContext.getCalendarId();

        List<Map<String, String>> events = eventService.getEventsInDateRange(calendarId, start, end);
        List<Map<String, String>> responseList = new ArrayList<>();

        if (events.isEmpty()) {
            responseList.add(Map.of("role", "ai", "content", "No events found between " + start + " and " + end + "."));
        } else {
            for (Map<String, String> event : events) {
                responseList.add(Map.of(
                        "role", "event",
                        "summary", event.get("summary"),
                        "date", event.get("start").split(" ")[0],
                        "time", event.get("start").split(" ")[1] + " - " + event.get("end").split(" ")[1],
                        "location", event.getOrDefault("location", "No location"),
                        "calendarId", calendarId,
                        "id", event.get("id")
                ));
            }
        }

        return new ObjectMapper().writeValueAsString(responseList);
    }

    private String handleCreateEvent(JsonNode jsonNode) throws Exception {
        Event event = parseEventDetails(jsonNode);
        String calendarId = calendarContext.getCalendarId();
        eventService.createEvent(calendarId, event);

        Map<String, String> response = new HashMap<>();
        response.put("role", "event");
        response.put("summary", event.getSummary());
        response.put("date", event.getStart().format(OUTPUT_DATE_FORMATTER));
        response.put("time", event.getStart().format(OUTPUT_TIME_FORMATTER) + " - " + event.getEnd().format(OUTPUT_TIME_FORMATTER));
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

            String start = jsonNode.get("start").asText();
            String end = jsonNode.get("end").asText();

            event.setStart(LocalDateTime.parse(start, ISO_FORMATTER));
            event.setEnd(LocalDateTime.parse(end, ISO_FORMATTER));
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
        conversationHistory.add(new ChatMessage("user", prompt));
        ChatCompletionResult result = chatGPTService.callChatGPT(conversationHistory);
        String reply = result.getChoices().get(0).getMessage().getContent().replaceAll("\\n+", " ");
        conversationHistory.add(new ChatMessage("assistant", reply));

        return buildAiMessage(reply);
    }

    private String buildAiMessage(String content) {
        try {
            return new ObjectMapper().writeValueAsString(Collections.singletonList(
                    Map.of("role", "ai", "content", content)
            ));
        } catch (Exception e) {
            return "[{\"role\":\"ai\",\"content\":\"Error generating reply\"}]";
        }
    }

    private String buildErrorResponse(String errorMsg) {
        try {
            return new ObjectMapper().writeValueAsString(Collections.singletonList(
                    Map.of("role", "ai", "content", errorMsg)
            ));
        } catch (Exception e) {
            return "[{\"role\":\"ai\",\"content\":\"Unexpected fatal error\"}]";
        }
    }
}
