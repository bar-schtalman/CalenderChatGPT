package com.handson.CalenderGPT.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.context.CalendarContext;
import com.handson.CalenderGPT.model.Event;
import com.handson.CalenderGPT.model.IntentType;
import com.handson.CalenderGPT.model.PendingEventState;
import com.handson.CalenderGPT.model.User;
import com.handson.CalenderGPT.model.UserMessage;
import com.handson.CalenderGPT.repository.UserMessageRepository;
import com.handson.CalenderGPT.repository.UserRepository;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class ConversationService {

    private final IntentService intentService;
    private final EventService eventService;
    private final CalendarContext calendarContext;
    private final ChatGPTService chatGPTService;
    private final UserMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ClarificationService clarificationService;
    private final EventResponseBuilder eventResponseBuilder;
    private final EventParser eventParser;
    private final Map<UUID, PendingEventState> pendingEvents = new HashMap<>();

    public ConversationService(IntentService intentService,
                               EventService eventService,
                               CalendarContext calendarContext,
                               ChatGPTService chatGPTService,
                               UserMessageRepository messageRepository,
                               UserRepository userRepository,
                               ClarificationService clarificationService,
                               EventResponseBuilder eventResponseBuilder,
                               EventParser eventParser) {
        this.intentService = intentService;
        this.eventService = eventService;
        this.calendarContext = calendarContext;
        this.chatGPTService = chatGPTService;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.clarificationService = clarificationService;
        this.eventResponseBuilder = eventResponseBuilder;
        this.eventParser = eventParser;
    }

    @Transactional
    public String handlePrompt(String prompt, UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        messageRepository.save(new UserMessage(user, true, prompt));

        // Step 1: Clarify ongoing prompt
        PendingEventState previousState = pendingEvents.get(userId);
        String mergedPrompt = previousState != null
                ? previousState.getSummary() + " " + prompt
                : prompt;

        // Step 2: Extract event intent
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
            messageRepository.save(new UserMessage(user, false, reply));
            return buildAiMessage(reply);
        }

        // Step 3: Clear if intent changed
        if (previousState != null && !previousState.getIntent().equalsIgnoreCase(intentStr)) {
            pendingEvents.remove(userId);
            previousState = null;
        }

        // Step 4: Handle VIEW
        IntentType intent = mapIntentType(intentStr);
        if (intent == IntentType.VIEW_EVENTS) {
            try {
                String start = json.get("start").asText();
                String end = json.get("end").asText();
                List<Map<String, String>> events = eventService.getEventsInDateRange(calendarContext.getCalendarId(), start, end);

                return events.isEmpty()
                        ? eventResponseBuilder.buildNoEventsFound(start, end)
                        : eventResponseBuilder.buildEventList(events, calendarContext.getCalendarId());

            } catch (Exception e) {
                return buildAiMessage("❌ Failed to fetch events: " + e.getMessage());
            }
        }

        // Step 5: Create state
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

        // Step 6: Missing info?
        if (!state.isComplete()) {
            pendingEvents.put(userId, state);
            return buildAiMessage(clarificationService.buildClarificationMessage(state));
        }

        // Step 7: Create event
        pendingEvents.remove(userId);
        Event event = eventParser.parseFromJson(json);

        try {
            Map<String, String> created = eventService.createEvent(calendarContext.getCalendarId(), event);
            return eventResponseBuilder.buildEventCardResponse(event, created);
        } catch (IOException e) {
            return buildAiMessage("❌ Failed to create the event: " + e.getMessage());
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
