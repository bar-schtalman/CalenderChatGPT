package com.handson.CalenderGPT.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.model.IntentType;
import com.handson.CalenderGPT.model.ChatGPTRequest;
import com.handson.CalenderGPT.model.ChatGPTResponse;
import com.handson.CalenderGPT.model.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class IntentService {

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String url;

    private final RestTemplate template;

    public IntentService(RestTemplate template) {
        this.template = template;
    }

    /**
     * Determines whether the user input is a CRUD operation or a regular chat.
     * If CRUD is detected, it extracts event details in JSON format.
     * Otherwise, it returns a regular chatbot response.
     */
    public String processUserPrompt(String userInput) {
        try {
            System.out.println("\n🔍 Processing User Prompt: " + userInput);

            // Send prompt to ChatGPT for intent detection & detail extraction
            String jsonResponse = extractDetailsFromPrompt(userInput);
            System.out.println("🛠 Raw Response from ChatGPT:\n" + jsonResponse);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonResponse);

            // Get detected intent
            String extractedIntent = rootNode.path("intent").asText().toUpperCase();
            System.out.println("🔹 Extracted Intent from ChatGPT: " + extractedIntent);

            IntentType intent = mapIntentType(extractedIntent);
            System.out.println("✅ Mapped Intent: " + intent);

            if (intent != IntentType.NONE) {
                // CRUD operation detected, return JSON with extracted event details
                String formattedDetails = formatExtractedEventDetails(rootNode);
                System.out.println("📅 Extracted Event Details: " + formattedDetails);
                return formattedDetails;
            } else {
                // No CRUD intent detected, return normal chatbot response
                String chatbotResponse = rootNode.path("message").asText();
                System.out.println("💬 Regular Chat Response: " + chatbotResponse);
                return chatbotResponse;
            }
        } catch (Exception e) {
            System.err.println("❌ Error processing prompt: " + e.getMessage());
            e.printStackTrace();
            return "Error processing prompt: " + e.getMessage();
        }
    }
    private String extractIntentFromPrompt(String prompt) {
        String extractionPrompt = "Analyze this text and return ONLY one of these exact values: " +
                "\"CREATE\", \"EDIT\", \"DELETE\", \"VIEW\", \"NONE\". " +
                "Do NOT return any other text, and do NOT add extra words. " +
                "Text: \"" + prompt + "\"";

        ChatGPTRequest extractionRequest = new ChatGPTRequest();
        extractionRequest.setModel(model);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", extractionPrompt));
        extractionRequest.setMessages(messages);

        ChatGPTResponse response = template.postForObject(url, extractionRequest, ChatGPTResponse.class);
        return response.getChoices().get(0).getMessage().getContent().trim();
    }


    public IntentType detectIntent(String userInput) {
        try {
            // Ask ChatGPT to classify the intent
            String extractedIntent = extractIntentFromPrompt(userInput).toUpperCase().replace(" ", "_");

            System.out.println("Extracted Intent from ChatGPT: " + extractedIntent);

            // Map ChatGPT's output to valid IntentType enums
            switch (extractedIntent) {
                case "CREATE":
                    return IntentType.CREATE_EVENT;
                case "EDIT":
                    return IntentType.EDIT_EVENT;
                case "DELETE":
                    return IntentType.DELETE_EVENT;
                case "VIEW":
                    return IntentType.VIEW_EVENTS;
                case "NONE":
                    return IntentType.NONE;
                default:
                    System.out.println("⚠️ Unknown Intent Detected: " + extractedIntent);
                    return IntentType.NONE;
            }
        } catch (Exception e) {
            System.out.println("❌ Error detecting intent: " + e.getMessage());
            return IntentType.NONE;
        }
    }


    /**
     * Extracts details from the user input using ChatGPT.
     */
    public String extractDetailsFromPrompt(String prompt) {
        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);

        String extractionPrompt = "Analyze the following text and determine if it represents an event-related request (Create, Edit, Delete, View). " +
                "If it is event-related, return structured JSON with fields: " +
                "'intent' (CREATE, EDIT, DELETE, VIEW), " +
                "'summary', 'description', 'start', 'end', 'location'. " +
                "Ensure 'start' and 'end' are in **ISO 8601 format** (YYYY-MM-DDTHH:MM:SS.SSSZ). " +
                "**If the provided date is in the past, adjust it to the next valid occurrence in the future.** " +
                "**Ensure 'end' is always after 'start'.** If 'end' is missing, set it to **1 hour after 'start'**. " +
                "**If only a day of the week is provided (e.g., 'next Friday'), determine the exact date in the future.** " +
                "If the intent is NOT event-related, return {'intent': 'NONE', 'message': 'response text'}. " +
                "Today's date is " + today + ". " +
                "Text: \"" + prompt + "\"";

        ChatGPTRequest extractionRequest = new ChatGPTRequest();
        extractionRequest.setModel(model);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", extractionPrompt));
        extractionRequest.setMessages(messages);

        ChatGPTResponse response = template.postForObject(url, extractionRequest, ChatGPTResponse.class);
        String rawResponse = response.getChoices().get(0).getMessage().getContent();

        System.out.println("🛠 Raw Response from ChatGPT:\n" + rawResponse);

        // Strip markdown-style JSON block if present
        if (rawResponse.startsWith("```json")) {
            rawResponse = rawResponse.replace("```json", "").replace("```", "").trim();
        }

        return rawResponse;
    }



    /**
     * Maps extracted intent from ChatGPT to IntentType enum.
     */
    private IntentType mapIntentType(String extractedIntent) {
        try {
            switch (extractedIntent) {
                case "CREATE":
                    return IntentType.CREATE_EVENT;
                case "EDIT":
                    return IntentType.EDIT_EVENT;
                case "DELETE":
                    return IntentType.DELETE_EVENT;
                case "VIEW":
                    return IntentType.VIEW_EVENTS;
                case "NONE":
                    return IntentType.NONE;
                default:
                    System.out.println("❗ Unknown Intent Detected: " + extractedIntent);
                    return IntentType.NONE;
            }
        } catch (IllegalArgumentException e) {
            System.err.println("🚨 Invalid Intent Mapping: " + extractedIntent);
            e.printStackTrace();
            return IntentType.NONE;
        }
    }

    /**
     * Formats extracted event details into JSON format.
     */
    private String formatExtractedEventDetails(JsonNode rootNode) {
        String summary = rootNode.path("summary").asText("No Title");
        String description = rootNode.path("description").asText("");
        String start = rootNode.path("start").asText("2025-02-17T19:13:29.676Z");
        String end = rootNode.path("end").asText("");
        String location = rootNode.path("location").asText("");

        // If end time is missing, set it 1 hour after start
        if (end.isEmpty()) {
            end = start.replace("T", "T01:"); // Default to 1 hour later
        }

        return "{\n" +
                "  \"summary\": \"" + summary + "\",\n" +
                "  \"description\": \"" + description + "\",\n" +
                "  \"start\": \"" + start + "\",\n" +
                "  \"end\": \"" + end + "\",\n" +
                "  \"location\": \"" + location + "\"\n" +
                "}";
    }

    private String buildExtractionPrompt(String prompt, IntentType intent) {
        switch (intent) {
            case VIEW_EVENTS:
                return "Analyze the following text and extract a date range for viewing events. " +
                        "Return JSON with fields: " +
                        "'intent' (VIEW), " +
                        "'start' (in ISO 8601 format), " +
                        "'end' (in ISO 8601 format). " +
                        "If only a single date is provided, set 'end' to the same date. " +
                        "Ensure dates are in the future if necessary. " +
                        "Text: \"" + prompt + "\"";

            default:
                return "Analyze the following prompt and respond accordingly: \"" + prompt + "\"";
        }
    }

}
