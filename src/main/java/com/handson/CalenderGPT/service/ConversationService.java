package com.handson.CalenderGPT.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.context.CalendarContext;
import com.handson.CalenderGPT.model.Event;
import com.handson.CalenderGPT.model.IntentType;
import com.handson.CalenderGPT.model.PendingEventState;
import com.handson.CalenderGPT.model.User;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final IntentService intentService;
    private final EventService eventService;
    private final ChatGPTService chatGPTService;
    private final ClarificationService clarificationService;
    private final EventResponseBuilder eventResponseBuilder;
    private final EventParser eventParser;

    private final Map<UUID, PendingEventState> pendingEvents = new HashMap<>();

    @Transactional
    public String handlePrompt(String prompt, User user, CalendarContext calendarContext) {
        PendingEventState previousState = pendingEvents.get(user.getId());
        String mergedPrompt = previousState != null ? previousState.getSummary() + " " + prompt : prompt;

        JsonNode json;
        try {
            String extractedJson = intentService.extractDetailsFromPrompt(mergedPrompt);
            json = new ObjectMapper().readTree(extractedJson);
        } catch (Exception e) {
            return wrapAsJson("⚠️ I couldn't understand that. Can you rephrase?", "ai");
        }

        String intentStr = json.path("intent").asText("");
        if (intentStr.equalsIgnoreCase("NONE")) {
            List<ChatMessage> messages = List.of(
                    new ChatMessage("system", "You are a helpful assistant."),
                    new ChatMessage("user", prompt)
            );
            ChatCompletionResult result = chatGPTService.callChatGPT(messages);
            String reply = result.getChoices().get(0).getMessage().getContent().trim();
            return wrapAsJson(reply, "ai");
        }

        if (previousState != null && !previousState.getIntent().equalsIgnoreCase(intentStr)) {
            pendingEvents.remove(user.getId());
            previousState = null;
        }

        IntentType intent = mapIntentType(intentStr);
        String calendarId = calendarContext.getCalendarId();

        if (intent == IntentType.VIEW_EVENTS) {
            try {
                String start = json.get("start").asText();
                String end = json.get("end").asText();
                List<Map<String, String>> events = eventService.getEventsInDateRange(calendarId, start, end, user);

                if (events.isEmpty()) {
                    return eventResponseBuilder.buildNoEventsFound(start, end); // already JSON
                } else {
                    return eventResponseBuilder.buildEventList(events, calendarId); // already JSON
                }
            } catch (Exception e) {
                return wrapAsJson("❌ Failed to fetch events: " + e.getMessage(), "ai");
            }
        }

        PendingEventState state = new PendingEventState();
        state.setIntent(intentStr);
        state.setSummary(json.path("summary").asText(""));
        state.setStart(json.path("start").asText(""));
        state.setEnd(json.path("end").asText(""));
        state.setLocation(json.path("location").asText(""));
        state.setDescription(json.path("description").asText(""));

        if (previousState != null) {
            state.mergeFrom(previousState);
        }

        if (!state.isComplete()) {
            pendingEvents.put(user.getId(), state);
            String clarification = clarificationService.buildClarificationMessage(state);
            return wrapAsJson(clarification, "ai");
        }

        pendingEvents.remove(user.getId());
        Event event = eventParser.parseFromJson(json);

        try {
            Map<String, String> created = eventService.createEvent(calendarId, event, user);
            String response = eventResponseBuilder.buildEventCardResponse(event, created);
            return wrapAsJson(response, "event");
        } catch (Exception e) {
            return wrapAsJson("❌ Failed to create the event: " + e.getMessage(), "ai");
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

    // ✅ Helper: wraps simple replies into structured JSON with given role
    private String wrapAsJson(String replyContent, String role) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, String>> replyList = List.of(
                    Map.of("role", role, "content", replyContent)
            );
            return mapper.writeValueAsString(replyList);
        } catch (Exception e) {
            return "[{\"role\":\"ai\",\"content\":\"Error generating reply\"}]";
        }
    }
}
