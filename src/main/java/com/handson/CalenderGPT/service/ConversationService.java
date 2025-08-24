package com.handson.CalenderGPT.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.context.CalendarContext;
import com.handson.CalenderGPT.controller.ChatController;
import com.handson.CalenderGPT.model.*;
import com.handson.CalenderGPT.repository.ConversationRepository;
import com.handson.CalenderGPT.repository.MessageRepository;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    private static final int HISTORY_LIMIT = 50;

    @Transactional
    public String handlePrompt(String prompt, User user, CalendarContext calendarContext) {
        String merged = mergeWithPreviousSummary(prompt, user);
        log.info("string merge: {}", merged);
        JsonNode details = parseDetailsJson(merged);
        if (details == null) {
            return wrapAsJson("⚠️ I couldn't understand that. Can you rephrase?", "ai");
        }

        String intentStr = details.path("intent").asText("");
        if (isNoneIntent(intentStr)) {
            PendingEventState currPending = pendingEvents.get(user.getId());

            if (currPending != null && !currPending.isComplete()) {
                // ננסה להעשיר את ה־PendingEventState
                PendingEventState enriched = enrichPendingState(prompt, currPending, user);

                // נשמור את ההודעה המקורית אם זו הפעם הראשונה (initialPrompt ריק)
                if (currPending.getInitialPrompt() == null || currPending.getInitialPrompt().isEmpty()) {
                    enriched.setInitialPrompt(prompt);
                }

                if (enriched.isComplete()) {
                    pendingEvents.remove(user.getId());

                    String resultJson = handleCreateEvent(
                            eventParser.toJsonNode(enriched),
                            calendarContext.getCalendarId(),
                            user
                    );

                    // שמירת הודעת המשתמש המקורית
                    saveMessageToHistory(user, currPending.getInitialPrompt(), "user");

                    // שמירת תגובת הצ'אט (האירוע שנוצר)
                    saveMessageToHistory(user, resultJson, "assistant");

                    return resultJson;
                }

                // עדיין חסרים פרטים – נבקש הבהרה
                pendingEvents.put(user.getId(), enriched);
                return wrapAsJson(clarificationService.buildClarificationMessage(enriched), "ai");
            }

            // No pending event, or nothing to clarify – handle as a normal query
            return handleNoneIntent(prompt, user);
        }

        // intent is not NONE — מתחילים פעולה מסוג CREATE/EDIT/VIEW/DELETE
        PendingEventState prev = resetStateIfIntentChanged(intentStr, user);
        IntentType intent = mapIntentType(intentStr);

        if (intent == IntentType.VIEW_EVENTS) {
            return handleViewEvents(details, calendarContext.getCalendarId(), user);
        }
        if (intent == IntentType.AVAILABILITY){
            return handleAvailability(details, calendarContext.getCalendarId(), user);
        }

        // intent מסוג CREATE/EDIT/DELETE — נבדוק אם יש מספיק פרטים
        PendingEventState state = buildOrUpdateState(details, prev);
        if (!state.isComplete()) {
            // התחלה של Pending חדש — נגדיר את initialPrompt
            if (prev == null) {
                state.setInitialPrompt(prompt);
            }

            pendingEvents.put(user.getId(), state);
            return wrapAsJson(clarificationService.buildClarificationMessage(state), "ai");
        }

        // כל הפרטים קיימים — ניצור את האירוע ונשמור היסטוריה

        pendingEvents.remove(user.getId());

        String resultJson = handleCreateEvent(details, calendarContext.getCalendarId(), user);

// ✅ שמירת הודעת המשתמש והתגובה כי האירוע נוצר מיידית (ללא פנדינג)
        saveMessageToHistory(user, prompt, "user");
        saveMessageToHistory(user, resultJson, "assistant");

        return resultJson;

    }

   private static final java.time.ZoneId IL_TZ = java.time.ZoneId.of("Asia/Jerusalem");
private static final String LRM = "\u200E"; // Left-to-Right Mark כדי לייצב את "HH:mm - HH:mm" בעברית

private String handleAvailability(JsonNode details, String calendarId, User user) {
    try {
        String startStr = details.path("start").asText("");
        String endStr   = details.path("end").asText("");
        if (startStr.isEmpty() || endStr.isEmpty()) {
            return wrapAsJson("❌ Missing start/end for availability check.", "ai");
        }

        // נפרש תמיד UTC-Z ואז נהפוך לזמן ישראל
        var sUTC = java.time.OffsetDateTime.parse(startStr);
        var eUTC = java.time.OffsetDateTime.parse(endStr);
        var s    = sUTC.withOffsetSameInstant(java.time.ZoneOffset.UTC).atZoneSameInstant(IL_TZ);
        var e    = eUTC.withOffsetSameInstant(java.time.ZoneOffset.UTC).atZoneSameInstant(IL_TZ);

        // --- נרמול "יום מלא" ---
        // יש מקרים שה-LLM מחזיר טווח שקרוב ל-24 שעות אבל עם היסט (למשל 03:00 -> למחרת 02:59:59),
        // נזהה "יום מלא" לפי משך >= 20 שעות או דפוסים נפוצים, ונכריח 00:00–23:59:59 מקומי.
        var dur = java.time.Duration.between(s, e);
        boolean looksLikeFullDay =
                dur.toHours() >= 20
             || (s.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)
                 && (e.toLocalDate().equals(s.toLocalDate())
                     && (e.toLocalTime().equals(java.time.LocalTime.of(23,59))
                         || e.toLocalTime().equals(java.time.LocalTime.of(23,59,59)))))
             || (e.toLocalDate().equals(s.toLocalDate().plusDays(1))
                 && (s.toLocalTime().equals(java.time.LocalTime.of(3,0))
                     && (e.toLocalTime().equals(java.time.LocalTime.of(2,59))
                         || e.toLocalTime().equals(java.time.LocalTime.of(2,59,59)))));

        if (looksLikeFullDay) {
            var dayStart = s.toLocalDate().atStartOfDay(IL_TZ);
            var dayEnd   = dayStart.plusDays(1).minusSeconds(1);
            s = dayStart;
            e = dayEnd;
        }

        boolean sameDay = s.toLocalDate().equals(e.toLocalDate());

        // דיבאג עדין (יעזור לך לראות מה הגיע אחרי הנרמול)
        org.slf4j.LoggerFactory.getLogger(ConversationService.class)
                .info("AVAILABILITY range (IL): {} -> {}", s, e);

        // חישוב כל חלונות הזמן הפנויים בטווח
        java.util.List<String> windows = eventService.findFreeWindows(calendarId, s, e, user);

        if (windows.isEmpty()) {
            if (sameDay && s.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)
                        && e.toLocalTime().equals(java.time.LocalTime.of(23, 59, 59))) {
                return wrapAsJson("אין זמינות ביום " + s.toLocalDate() + ".", "ai");
            }
            return wrapAsJson("אין זמינות בטווח שביקשת.", "ai");
        }

        // עיטוף ב-LRM כדי למנוע היפוך כיווניות ב-RTL (19:00 - 21:00 יוצג בסדר נכון)
        java.util.function.Function<String,String> rtlSafe = t -> LRM + t + LRM;

        if (windows.size() == 1) {
            String label = rtlSafe.apply(windows.get(0));
            if (sameDay && label.contains("00:00") && (label.contains("23:59"))) {
                return wrapAsJson("כל היום פנוי (" + s.toLocalDate() + ").", "ai");
            }
            if (sameDay) {
                return wrapAsJson("אתה פנוי לכל הטווח: " + label + " ביום " + s.toLocalDate() + ".", "ai");
            } else {
                return wrapAsJson("אתה פנוי בטווח: " + label + ".", "ai");
            }
        }

        String joined = rtlSafe.apply(String.join(", ", windows));
        if (sameDay) {
            return wrapAsJson("הזמינות ביום " + s.toLocalDate() + ": " + joined + ".", "ai");
        } else {
            return wrapAsJson("חלונות זמינות בטווח שביקשת: " + joined + ".", "ai");
        }
    } catch (Exception ex) {
        return wrapAsJson("❌ Failed to compute availability: " + ex.getMessage(), "ai");
    }
}


    private String mergeWithPreviousSummary(String prompt, User user) {
        PendingEventState prev = pendingEvents.get(user.getId());
        return prev != null ? prev.getSummary() + " " + prompt : prompt;
    }

    private JsonNode parseDetailsJson(String text) {
        try {
            log.info("string text: {}", text);
            String extracted = intentService.extractDetailsFromPrompt(text);
            log.info("string extracted: {}", extracted);
            return objectMapper.readTree(extracted);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isNoneIntent(String intent) {
        return intent.equalsIgnoreCase("NONE");
    }

    private String handleNoneIntent(String prompt, User user) {
        Conversation conversation = conversationRepository.findByUser(user)
                .orElseGet(() -> conversationRepository.save(Conversation.builder().user(user).build()));

        List<Message> history = messageRepository.findByConversationOrderByTimestampAsc(conversation);

        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage("system", "You are a helpful assistant."));
        for (Message msg : history) {
            chatMessages.add(new ChatMessage(msg.getRole(), msg.getContent()));
        }
        chatMessages.add(new ChatMessage("user", prompt));

        String reply;
        try {
            ChatCompletionResult result = chatGPTService.callChatGPT(chatMessages);
            reply = result.getChoices().get(0).getMessage().getContent().trim();
        } catch (Exception e) {
            reply = "❌ Sorry, I couldn't generate a response.";
        }

        messageRepository.save(Message.builder()
                .conversation(conversation)
                .role("user")
                .content(prompt)
                .build());

        messageRepository.save(Message.builder()
                .conversation(conversation)
                .role("assistant")
                .content(reply)
                .build());

        long count = messageRepository.countByConversation(conversation);
        if (count > HISTORY_LIMIT) {
            long toDelete = count - HISTORY_LIMIT;
            for (int i = 0; i < toDelete; i++) {
                messageRepository.findFirstByConversationOrderByTimestampAsc(conversation)
                        .ifPresent(messageRepository::delete);
            }
        }

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
            List<Map<String, String>> events = eventService.getEventsInDateRange(calendarId, start, end, user);
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
            case "AVAILABILITY" -> IntentType.AVAILABILITY;
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

    private PendingEventState enrichPendingState(String prompt, PendingEventState prev, User user) {
        try {
            String extractedJson = intentService.extractDetailsFromPrompt(prompt);
            JsonNode extractedDetails = objectMapper.readTree(extractedJson);

            PendingEventState update = new PendingEventState();
            update.setIntent(extractedDetails.path("intent").asText(""));
            update.setSummary(extractedDetails.path("summary").asText(""));
            update.setStart(extractedDetails.path("start").asText(""));
            update.setEnd(extractedDetails.path("end").asText(""));
            update.setLocation(extractedDetails.path("location").asText(""));
            update.setDescription(extractedDetails.path("description").asText(""));

            if (prev != null) {
                update.mergeFrom(prev);
            }

            return update;

        } catch (Exception e) {
            log.warn("⚠️ Failed to enrich pending state: {}", e.getMessage());
            return prev; // נמשיך עם הקיים
        }
    }

    private void saveMessageToHistory(User user, String content, String role) {
        Conversation conversation = conversationRepository.findByUser(user)
                .orElseGet(() -> conversationRepository.save(Conversation.builder().user(user).build()));

        messageRepository.save(Message.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .build());

        long count = messageRepository.countByConversation(conversation);
        if (count > HISTORY_LIMIT) {
            long toDelete = count - HISTORY_LIMIT;
            for (int i = 0; i < toDelete; i++) {
                messageRepository.findFirstByConversationOrderByTimestampAsc(conversation)
                        .ifPresent(messageRepository::delete);
            }
        }
    }
    private List<ChatMessage> loadHistory(User user, String newUserPrompt) {
        Conversation conversation = conversationRepository.findByUser(user)
                .orElseGet(() -> conversationRepository.save(Conversation.builder().user(user).build()));

        List<Message> history = messageRepository.findByConversationOrderByTimestampAsc(conversation);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", "You are a helpful assistant."));
        for (Message msg : history) {
            messages.add(new ChatMessage(msg.getRole(), msg.getContent()));
        }
        messages.add(new ChatMessage("user", newUserPrompt));
        return messages;
    }



}
