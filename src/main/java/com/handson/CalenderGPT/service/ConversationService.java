package com.handson.CalenderGPT.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.context.CalendarContext;
import com.handson.CalenderGPT.model.*;
import com.handson.CalenderGPT.repository.ChatSessionRepository;
import com.handson.CalenderGPT.repository.UserMessageRepository;
import com.handson.CalenderGPT.repository.UserRepository;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

@Service
public class ConversationService {

    private final IntentService intentService;
    private final EventService eventService;
    private final ChatGPTService chatGPTService;
    private final UserMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ClarificationService clarificationService;
    private final EventResponseBuilder eventResponseBuilder;
    private final EventParser eventParser;

    private final Map<UUID, PendingEventState> pendingEvents = new HashMap<>();

    public ConversationService(IntentService intentService,
                               EventService eventService,
                               ChatGPTService chatGPTService,
                               UserMessageRepository messageRepository,
                               UserRepository userRepository,
                               ChatSessionRepository chatSessionRepository,
                               ClarificationService clarificationService,
                               EventResponseBuilder eventResponseBuilder,
                               EventParser eventParser) {
        this.intentService = intentService;
        this.eventService = eventService;
        this.chatGPTService = chatGPTService;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.clarificationService = clarificationService;
        this.eventResponseBuilder = eventResponseBuilder;
        this.eventParser = eventParser;
    }

    @Transactional
    public String handlePrompt(String prompt, User user, CalendarContext calendarContext) {
        ChatSession session = getOrCreateLatestSession(user);
        messageRepository.save(new UserMessage(user, session, true, prompt));

        PendingEventState previousState = pendingEvents.get(user.getId());
        String mergedPrompt = previousState != null ? previousState.getSummary() + " " + prompt : prompt;

        String extractedJson = intentService.extractDetailsFromPrompt(mergedPrompt);
        JsonNode json;
        try {
            json = new ObjectMapper().readTree(extractedJson);
        } catch (Exception e) {
            return buildAiMessage("⚠️ I couldn't understand that. Can you rephrase?");
        }

        String intentStr = json.path("intent").asText("");
        if (intentStr.equalsIgnoreCase("NONE")) {
            List<ChatMessage> messages = List.of(
                    new ChatMessage("system", "You are a helpful assistant."),
                    new ChatMessage("user", prompt)
            );
            ChatCompletionResult result = chatGPTService.callChatGPT(messages);
            String reply = result.getChoices().get(0).getMessage().getContent().trim();
            messageRepository.save(new UserMessage(user, session, false, reply));
            return buildAiMessage(reply);
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

                return events.isEmpty()
                        ? eventResponseBuilder.buildNoEventsFound(start, end)
                        : eventResponseBuilder.buildEventList(events, calendarId);

            } catch (Exception e) {
                return buildAiMessage("❌ Failed to fetch events: " + e.getMessage());
            }
        }

        // Build state for CREATE/EDIT
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
            return buildAiMessage(clarificationService.buildClarificationMessage(state));
        }

        pendingEvents.remove(user.getId());
        Event event = eventParser.parseFromJson(json);

        try {
            Map<String, String> created = eventService.createEvent(calendarId, event, user);
            return eventResponseBuilder.buildEventCardResponse(event, created);
        } catch (Exception e) {
            return buildAiMessage("❌ Failed to create the event: " + e.getMessage());
        }
    }

    private ChatSession getOrCreateLatestSession(User user) {
        return chatSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .findFirst()
                .orElseGet(() -> {
                    ChatSession session = new ChatSession();
                    session.setUser(user);
                    session.setTitle("New Chat");
                    return chatSessionRepository.save(session);
                });
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

    private String buildAiMessage(String content) {
        try {
            return new ObjectMapper().writeValueAsString(List.of(
                    Map.of("role", "ai", "content", content)
            ));
        } catch (Exception e) {
            return "[{\"role\":\"ai\",\"content\":\"Error generating reply\"}]";
        }
    }
}
