package com.handson.CalenderGPT.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.context.CalendarContext;
import com.handson.CalenderGPT.model.*;
import com.handson.CalenderGPT.repository.UserMessageRepository;
import com.handson.CalenderGPT.repository.UserRepository;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ConversationService {

    private final IntentService intentService;
    private final EventService eventService;
    private final CalendarContext calendarContext;
    private final ChatGPTService chatGPTService;
    private final UserMessageRepository messageRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter OUTPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter OUTPUT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public ConversationService(IntentService intentService,
                               EventService eventService,
                               CalendarContext calendarContext,
                               ChatGPTService chatGPTService,
                               UserMessageRepository messageRepository,
                               UserRepository userRepository) {
        this.intentService = intentService;
        this.eventService = eventService;
        this.calendarContext = calendarContext;
        this.chatGPTService = chatGPTService;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public String handlePrompt(String prompt, UUID userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            messageRepository.save(new UserMessage(user, true, prompt));

            List<UserMessage> pastMessages = messageRepository.findByUserIdOrderByTimestampAsc(userId);
            List<ChatMessage> openAiHistory = new ArrayList<>();
            for (UserMessage msg : pastMessages) {
                openAiHistory.add(new ChatMessage(msg.isUser() ? "user" : "assistant", msg.getContent()));
            }

            String extractedJson = intentService.extractDetailsFromPrompt(prompt);
            JsonNode jsonNode = new ObjectMapper().readTree(extractedJson);

            IntentType intent = mapIntentType(jsonNode.get("intent").asText());

            return switch (intent) {
                case VIEW_EVENTS -> handleViewEvents(jsonNode);
                case CREATE_EVENT -> handleCreateEvent(jsonNode);
                case EDIT_EVENT, DELETE_EVENT -> handleIntentPreview(intent, jsonNode);
                case NONE -> chatWithGPT(prompt, user, openAiHistory);
            };
        } catch (Exception e) {
            e.printStackTrace();
            return buildErrorResponse("❌ Error: " + e.getMessage());
        }
    }

    private String chatWithGPT(String prompt, User user, List<ChatMessage> history) {
        ChatCompletionResult result = chatGPTService.callChatGPT(history);
        String reply = result.getChoices().get(0).getMessage().getContent().replaceAll("\n+", " ");
        messageRepository.save(new UserMessage(user, false, reply));
        return buildAiMessage(reply);
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
        return new ObjectMapper().writeValueAsString(List.of(
                Map.of("role", "ai", "content", "Detected Intent: " + intent + "\n\nDetails:\n" + jsonNode.toPrettyString())
        ));
    }

    private String handleViewEvents(JsonNode jsonNode) throws Exception {
        String start = jsonNode.get("start").asText();
        String end = jsonNode.get("end").asText();
        String calendarId = calendarContext.getCalendarId();

        List<Map<String, String>> events = eventService.getEventsInDateRange(calendarId, start, end);

        if (events.isEmpty()) {
            return new ObjectMapper().writeValueAsString(List.of(
                    Map.of("role", "ai", "content", "📭 No events found between " + start + " and " + end + ".")
            ));
        }

        List<Map<String, String>> responseList = new ArrayList<>();

        // 📅 Date range header
        String title = start.equals(end)
                ? "📅 Events for " + formatDisplayDate(start)
                : "📅 Events from " + formatDisplayDate(start) + " to " + formatDisplayDate(end);

        responseList.add(Map.of("role", "ai", "content", title));

        for (Map<String, String> event : events) {
            responseList.add(Map.of(
                    "role", "event",
                    "summary", event.get("summary"),
                    "date", event.get("start").split(" ")[0],
                    "time", event.get("start").split(" ")[1] + " - " + event.get("end").split(" ")[1],
                    "location", event.getOrDefault("location", "No location"),
                    "calendarId", calendarId,
                    "id", event.get("id"),
                    "guests", event.getOrDefault("guests", "")
            ));
        }

        return new ObjectMapper().writeValueAsString(responseList);
    }

    private String handleCreateEvent(JsonNode jsonNode) throws Exception {
        Event event = parseEventDetails(jsonNode);
        String calendarId = calendarContext.getCalendarId();
        Map<String, String> createdEvent = eventService.createEvent(calendarId, event);

        return new ObjectMapper().writeValueAsString(List.of(Map.of(
                "role", "event",
                "summary", createdEvent.get("summary"),
                "date", createdEvent.get("start").split(" ")[0],
                "time", createdEvent.get("start").split(" ")[1] + " - " + createdEvent.get("end").split(" ")[1],
                "calendarId", calendarId,
                "id", createdEvent.get("id"),
                "guests", createdEvent.getOrDefault("guests", "")
        )));
    }

    private Event parseEventDetails(JsonNode jsonNode) {
        try {
            Event event = new Event();
            event.setSummary(jsonNode.get("summary").asText("No Title"));
            event.setDescription(jsonNode.get("description").asText(""));
            event.setLocation(jsonNode.get("location").asText(""));
            event.setStart(LocalDateTime.parse(jsonNode.get("start").asText(), ISO_FORMATTER));
            event.setEnd(LocalDateTime.parse(jsonNode.get("end").asText(), ISO_FORMATTER));
            event.setTimeZone(ZoneId.systemDefault().toString());
            return event;
        } catch (Exception e) {
            e.printStackTrace();
            Event fallback = new Event();
            fallback.setSummary("Default Event");
            fallback.setDescription("No Description");
            fallback.setLocation("No Location");
            fallback.setStart(LocalDateTime.now());
            fallback.setEnd(LocalDateTime.now().plusHours(1));
            fallback.setTimeZone(ZoneId.systemDefault().toString());
            return fallback;
        }
    }

    private String buildAiMessage(String content) {
        try {
            return new ObjectMapper().writeValueAsString(List.of(
                    Map.of("role", "ai", "content", content)
            ));
        } catch (Exception e) {
            return "[{\"role\":\"ai\",\"content\":\"Error generating reply\"}]";
        }
    }

    private String buildErrorResponse(String errorMsg) {
        try {
            return new ObjectMapper().writeValueAsString(List.of(
                    Map.of("role", "ai", "content", errorMsg)
            ));
        } catch (Exception e) {
            return "[{\"role\":\"ai\",\"content\":\"Unexpected fatal error\"}]";
        }
    }

    private String formatDisplayDate(String isoDateTime) {
        try {
            return LocalDate.parse(isoDateTime.substring(0, 10)).format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        } catch (Exception e) {
            return isoDateTime;
        }
    }
}
