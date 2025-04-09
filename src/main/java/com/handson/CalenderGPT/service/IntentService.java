package com.handson.CalenderGPT.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class IntentService {

    private final ChatGPTService chatGPTService;

    private final String today;
    private final String tomorrow;
    private final String nextWeek;

    public IntentService(ChatGPTService chatGPTService) {
        this.chatGPTService = chatGPTService;

        LocalDate todayDate = LocalDate.now();
        this.today = todayDate.format(DateTimeFormatter.ISO_DATE);
        this.tomorrow = todayDate.plusDays(1).format(DateTimeFormatter.ISO_DATE);
        this.nextWeek = todayDate.plusWeeks(1).format(DateTimeFormatter.ISO_DATE);
    }

    public String extractDetailsFromPrompt(String prompt) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate nextWeek = today.plusWeeks(1);

        String extractionPrompt =
                "Analyze the following text and determine if it represents an event-related request (Create, Edit, Delete, View). " +
                        "If it is event-related, return a structured JSON object with the following fields: " +
                        "\"intent\" (CREATE, EDIT, DELETE, VIEW), " +
                        "\"summary\", \"description\", \"start\", \"end\", \"location\". " +

                        // Format enforcement
                        "Ensure both \"start\" and \"end\" are in full ISO 8601 format with milliseconds and Z (e.g., 2025-04-09T15:00:00.000Z). " +

                        // ⚠️ Main Rule: only default when start is present
                        "IMPORTANT: For ALL intents, if \"start\" is provided but \"end\" is missing, set \"end\" to 1 hour after \"start\". " +
                        "If \"start\" is not provided at all, leave both \"start\" and \"end\" as empty strings (\"\"). " +
                        "Do NOT assume a full day or any default times based on the intent. " +

                        // 🗓️ Natural date handling
                        "Use these time references when interpreting phrases: today = " + today + ", tomorrow = " + tomorrow + ", next week = " + nextWeek + ". " +
                        "If a date does not include a year, use the current year if the date is still upcoming, otherwise use the next year. " +
                        "If a date includes a past year, adjust it to the next valid future occurrence by adding one year. " +

                        // 🧠 Not an event? Return NONE
                        "If the request is not related to an event (e.g., a song, story, poem), return {\"intent\": \"NONE\", \"message\": \"response text\"}. " +

                        // ✅ Output
                        "Respond only with raw JSON — no markdown, no extra text, and no formatting. " +
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
