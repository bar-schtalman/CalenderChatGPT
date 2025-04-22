package com.handson.CalenderGPT.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.context.CalendarContext;
import com.handson.CalenderGPT.model.*;
import com.handson.CalenderGPT.repository.ChatMessageRepository;
import com.handson.CalenderGPT.repository.ConversationRepository;
import com.handson.CalenderGPT.repository.UserRepository;
import com.handson.CalenderGPT.service.helper.ConversationHelper;
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
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ClarificationService clarificationService;
    private final EventResponseBuilder eventResponseBuilder;
    private final ConversationHelper conversationHelper;

    private final EventParser eventParser;

    private final Map<UUID, PendingEventState> pendingEvents = new HashMap<>();

    @Transactional
    public String handlePrompt(String prompt, User user, CalendarContext calendarContext) {
        Conversation conversation = conversationHelper.getOrCreateLatestConversation(user);
        chatMessageRepository.save(new com.handson.CalenderGPT.model.ChatMessage(user, conversation, true, prompt));

        PendingEventState previousState = pendingEvents.get(user.getId());
        String mergedPrompt = previousState != null ? previousState.getSummary() + " " + prompt : prompt;

        JsonNode json;
        try {
            String extractedJson = intentService.extractDetailsFromPrompt(mergedPrompt);
            json = new ObjectMapper().readTree(extractedJson);
        } catch (Exception e) {
            return storeAssistantMessage(user, conversation, "⚠️ I couldn't understand that. Can you rephrase?");
        }

        String intentStr = json.path("intent").asText("");
        if (intentStr.equalsIgnoreCase("NONE")) {
            List<ChatMessage> messages = List.of(
                    new ChatMessage("system", "You are a helpful assistant."),
                    new ChatMessage("user", prompt)
            );
            ChatCompletionResult result = chatGPTService.callChatGPT(messages);
            String reply = result.getChoices().get(0).getMessage().getContent().trim();
            return storeAssistantMessage(user, conversation, reply);
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

                String response = events.isEmpty()
                        ? eventResponseBuilder.buildNoEventsFound(start, end)
                        : eventResponseBuilder.buildEventList(events, calendarId);

                return storeAssistantMessage(user, conversation, response);

            } catch (Exception e) {
                return storeAssistantMessage(user, conversation, "❌ Failed to fetch events: " + e.getMessage());
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
            return storeAssistantMessage(user, conversation, clarificationService.buildClarificationMessage(state));
        }

        pendingEvents.remove(user.getId());
        Event event = eventParser.parseFromJson(json);

        try {
            Map<String, String> created = eventService.createEvent(calendarId, event, user);
            String response = eventResponseBuilder.buildEventCardResponse(event, created);
            return storeAssistantMessage(user, conversation, response);
        } catch (Exception e) {
            return storeAssistantMessage(user, conversation, "❌ Failed to create the event: " + e.getMessage());
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

    private String storeAssistantMessage(User user, Conversation conversation, String content) {
        chatMessageRepository.save(new com.handson.CalenderGPT.model.ChatMessage(user, conversation, false, content));
        return content;
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
