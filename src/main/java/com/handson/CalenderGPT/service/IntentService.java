package com.handson.CalenderGPT.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.model.ChatGPTRequest;
import com.handson.CalenderGPT.model.ChatGPTResponse;
import com.handson.CalenderGPT.model.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class IntentService {

    LocalDate todayDate = LocalDate.now();
    String today = todayDate.format(DateTimeFormatter.ISO_DATE);
    String tomorrow = todayDate.plusDays(1).format(DateTimeFormatter.ISO_DATE);
    String nextWeek = todayDate.plusWeeks(1).format(DateTimeFormatter.ISO_DATE);

    @Value("${openai.model}")
    private String model;

    private final ChatGPTService chatGPTService;

    public IntentService(ChatGPTService chatGPTService) {
        this.chatGPTService = chatGPTService;
    }

    public String extractDetailsFromPrompt(String prompt) {
        String extractionPrompt = "Analyze the following text and determine if it represents an event-related request (Create, Edit, Delete, View). " +
                "If it is event-related, return structured JSON with fields: " +
                "\"intent\" (CREATE, EDIT, DELETE, VIEW), \"summary\", \"description\", \"start\", \"end\", \"location\". " +
                "Ensure \"start\" and \"end\" are in ISO 8601 format (YYYY-MM-DDTHH:MM:SS.SSSZ). " +
                "If \"end\" is not provided, set it to 1 hour after \"start\". " +
                "However, if the intent is to view events and no specific time range is provided, assume the full day (from 00:00:00.000 to 23:59:59.999) for that date. " +
                "Consider that today's date is " + today + ", tomorrow's date is " + tomorrow + ", and one week from today is " + nextWeek + ". " +
                "If the provided \"start\" date does not include a year, determine whether that date is still upcoming in the current year; if it is, use the current year, otherwise schedule it for next year. " +
                "If the provided \"start\" date includes a year but is in the past relative to today's date, adjust it to the next valid occurrence by adding one year. " +
                "IMPORTANT: If the request is about writing a song, composing lyrics, generating a poem, writing a story, or anything that is NOT an event, return {\"intent\": \"NONE\", \"message\": \"response text\"} instead of classifying it as an event. " +
                "If the intent is NOT event-related, return a simple JSON object: {\"intent\": \"NONE\", \"message\": \"response text\"}. " +
                "Do NOT wrap the response in markdown code blocks or triple backticks (`). " +
                "Text: \"" + prompt + "\"";


        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", extractionPrompt));
        ChatGPTResponse response = chatGPTService.callChatGPT(messages);
        String rawResponse = response.getChoices().get(0).getMessage().getContent().trim();

        System.out.println("🛠 Raw Response from ChatGPT:\n" + rawResponse);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode;
        try {
            jsonNode = mapper.readTree(rawResponse);
        } catch (Exception e) {
            System.out.println("⚠️ Raw response was not valid JSON, treating as plain text.");
            jsonNode = mapper.createObjectNode()
                    .put("intent", "NONE")
                    .put("message", rawResponse);
        }
        return jsonNode.toString();
    }
}
