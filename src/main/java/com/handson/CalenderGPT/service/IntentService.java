package com.handson.CalenderGPT.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
public class IntentService {

    private static final ZoneId IL_TZ = ZoneId.of("Asia/Jerusalem");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_DATE;

    private final ChatGPTService chatGPTService;

    public IntentService(ChatGPTService chatGPTService) {
        this.chatGPTService = chatGPTService;
    }

    public String extractDetailsFromPrompt(String prompt) {
        // עוגנים דינמיים לפי Asia/Jerusalem
        LocalDate today = LocalDate.now(IL_TZ);
        LocalDate tomorrow = today.plusDays(1);

        // "השבוע": מהיום ועד שבת הקרובה (מקסימום 7 ימים)
        LocalDate thisWeekStart = today;
        LocalDate thisWeekEndSaturday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        // "שבוע הבא": ראשון–שבת הבאים (תמיד 7 ימים)
        LocalDate nextWeekStartSunday = today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        LocalDate nextWeekEndSaturday = nextWeekStartSunday.plusDays(6);

        String extractionPrompt =
                // תיאור כללי
                "Analyze the following text and determine if it represents an event-related request (Create, Edit, Delete, View, Availability ). " +
                "If it is event-related, return a structured JSON object with the following fields: " +
                "\"intent\" (CREATE, EDIT, DELETE, VIEW, AVAILABILITY), " +
                "\"summary\", \"description\", \"start\", \"end\", \"location\". " +

                // פורמט חובה של start/end
                "Ensure both \"start\" and \"end\" are in full ISO 8601 format with milliseconds and Z (e.g., 2025-04-09T15:00:00.000Z). " +

                // כלל ברירת מחדל ל-end
                "IMPORTANT: For ALL intents, if \"start\" is provided but \"end\" is missing, set \"end\" to 1 hour after \"start\". " +
                "If \"start\" is not provided at all, leave both \"start\" and \"end\" as empty strings (\"\"). " +

                // כללי "השבוע" ו"שבוע הבא" (Asia/Jerusalem; שבוע ראשון–שבת)
                "INTERPRETATION RULES (Asia/Jerusalem; week = Sunday–Saturday): " +
                "\"this week\" means from TODAY until the UPCOMING SATURDAY (at most 7 days). " +
                "\"next week\" means the NEXT Sunday–Saturday block (always 7 days). " +
                "When the user says \"this week\", set start to TODAY 00:00:00.000Z and end to SATURDAY 23:59:59.000Z of that week. " +
                "When the user says \"next week\", set start to NEXT SUNDAY 00:00:00.000Z and end to NEXT SATURDAY 23:59:59.000Z. " +

                // 🔧 חוקים לזמינות
                "AVAILABILITY rules (\"when am I free\", \"am I free at\", \"מתי אני פנוי\", \"האם אני פנוי\"): " +
                "If only a DATE is mentioned, set intent=AVAILABILITY and set start=DATE 00:00:00.000Z, end=DATE 23:59:59.000Z. " +
                "If a TIME RANGE is mentioned with the date, set that exact start/end. " +
                "If a single TIME is mentioned with the date, set a 1-hour window starting at that time. " +

                // 🔧 הוספה קריטית: כל השעות שהמשתמש כותב הן מקומיות לישראל; הפלט חייב להיות UTC (Z)
                "All user-provided times MUST be interpreted in the Asia/Jerusalem local time zone. " +
                "Before returning JSON, CONVERT those local times to UTC and output them with a 'Z' suffix. " +
                "For example: if the user says \"tomorrow at 17:00\" and Asia/Jerusalem is UTC+3, " +
                "then start must be 14:00:00.000Z on that date (not 17:00:00.000Z). " +
                "If only a DATE is given for availability, use local day bounds (00:00:00 and 23:59:59 in Asia/Jerusalem), " +
                "then convert both to UTC Z before returning. " +

                // עוגנים מספריים כדי לייצב את הפענוח
                ("Use these time anchors (computed for Asia/Jerusalem): " +
                 "today=" + today.format(ISO_DATE) +
                 ", tomorrow=" + tomorrow.format(ISO_DATE) +
                 ", this_week_start=" + thisWeekStart.format(ISO_DATE) +
                 ", this_week_end=" + thisWeekEndSaturday.format(ISO_DATE) +
                 ", next_week_start=" + nextWeekStartSunday.format(ISO_DATE) +
                 ", next_week_end=" + nextWeekEndSaturday.format(ISO_DATE) + ". ") +

                // טיפול בשנה
                "If a date does not include a year, use the current year if the date is still upcoming, otherwise use the next year. " +
                "If a date includes a past year, adjust it to the next valid future occurrence by adding one year. " +

                // אם לא אירוע – NONE (נשאר בדיוק בנוסח שלך)
                "If the request is not related to an event, return {\"intent\": \"NONE\", \"message\": \"...\"}. " +

                // פלט JSON גולמי בלבד
                "Respond only with raw JSON — no markdown, no extra text. " +

                // הטקסט לניתוח
                "Text: \"" + prompt + "\"";

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", extractionPrompt));

        ChatCompletionResult result = chatGPTService.callChatGPT(messages);
        String rawResponse = result.getChoices().get(0).getMessage().getContent().trim();

        System.out.println("🛠 Raw Response from ChatGPT:\n" + rawResponse);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode;
        try {
            jsonNode = mapper.readTree(rawResponse);
        } catch (Exception e) {
            System.out.println("⚠️ Raw response was not valid JSON, treating as plain text.");
            jsonNode = mapper.createObjectNode().put("intent", "NONE").put("message", rawResponse);
        }
        return jsonNode.toString();
    }
}