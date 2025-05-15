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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public String handlePrompt(String prompt, User user, CalendarContext calendarContext) {
        String merged = mergeWithPreviousSummary(prompt, user);
        JsonNode details = parseDetailsJson(merged);
        if (details == null) {
            return wrapAsJson("⚠️ I couldn't understand that. Can you rephrase?", "ai");
        }

        String intentStr = details.path("intent").asText("");
        if (isNoneIntent(intentStr)) {
            return handleNoneIntent(prompt);
        }

        PendingEventState prev = resetStateIfIntentChanged(intentStr, user);
        IntentType intent = mapIntentType(intentStr);

        if (intent == IntentType.VIEW_EVENTS) {
            return handleViewEvents(details, calendarContext.getCalendarId(), user);
        }

        PendingEventState state = buildOrUpdateState(details, prev);
        if (!state.isComplete()) {
            pendingEvents.put(user.getId(), state);
            return wrapAsJson(clarificationService.buildClarificationMessage(state), "ai");
        }

        pendingEvents.remove(user.getId());
        return handleCreateEvent(details, calendarContext.getCalendarId(), user);
    }

    private String mergeWithPreviousSummary(String prompt, User user) {
        PendingEventState prev = pendingEvents.get(user.getId());
        return prev != null ? prev.getSummary() + " " + prompt : prompt;
    }

    private JsonNode parseDetailsJson(String text) {
        try {
            String extracted = intentService.extractDetailsFromPrompt(text);
            return objectMapper.readTree(extracted);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isNoneIntent(String intent) {
        return intent.equalsIgnoreCase("NONE");
    }

    private String handleNoneIntent(String prompt) {
        List<ChatMessage> conv = List.of(
                new ChatMessage("system", "You are a helpful assistant."),
                new ChatMessage("user", prompt)
        );
        ChatCompletionResult res = chatGPTService.callChatGPT(conv);
        String reply = res.getChoices().get(0).getMessage().getContent().trim();
        return wrapAsJson(reply, "ai");
    }

    private PendingEventState resetStateIfIntentChanged(String intentStr, User user) {
        PendingEventState prev = pendingEvents.get(user.getId());
        if (prev != null && !prev.getIntent().equalsIgnoreCase(intentStr)) {
            pendingEvents.remove(user.getId());
            return null;
        }
        return prev;
    }

    private String handleViewEvents(JsonNode details, String calendarId, User user) {
        try {
            String start = details.get("start").asText();
            String end = details.get("end").asText();
            List<Map<String,String>> events = eventService.getEventsInDateRange(calendarId, start, end, user);
            if (events.isEmpty()) {
                return eventResponseBuilder.buildNoEventsFound(start, end);
            }
            return eventResponseBuilder.buildEventList(events, calendarId);
        } catch (Exception e) {
            return wrapAsJson("❌ Failed to fetch events: " + e.getMessage(), "ai");
        }
    }

    private PendingEventState buildOrUpdateState(JsonNode details, PendingEventState prev) {
        PendingEventState state = new PendingEventState();
        state.setIntent(details.path("intent").asText(""));
        state.setSummary(details.path("summary").asText(""));
        state.setStart(details.path("start").asText(""));
        state.setEnd(details.path("end").asText(""));
        state.setLocation(details.path("location").asText(""));
        state.setDescription(details.path("description").asText(""));
        if (prev != null) state.mergeFrom(prev);
        return state;
    }

    private String handleCreateEvent(JsonNode details, String calendarId, User user) {
        Event event = eventParser.parseFromJson(details);
        try {
            Map<String, String> created = eventService.createEvent(calendarId, event, user);
            String resp = eventResponseBuilder.buildEventCardResponse(event, created);
            return wrapAsJson(resp, "event");
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

    private String wrapAsJson(String content, String role) {
        try {
            List<Map<String, String>> out = List.of(Map.of("role", role, "content", content));
            return objectMapper.writeValueAsString(out);
        } catch (Exception ex) {
            return "[{\"role\":\"ai\",\"content\":\"Error generating reply\"}]";
        }
    }
}