package com.handson.CalenderGPT.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.model.IntentType;
import com.handson.CalenderGPT.model.ChatGPTRequest;
import com.handson.CalenderGPT.model.ChatGPTResponse;
import com.handson.CalenderGPT.model.Message;
import com.handson.CalenderGPT.model.Event;
import com.handson.CalenderGPT.service.IntentService;
import com.handson.CalenderGPT.service.EventService;
import com.handson.CalenderGPT.context.CalendarContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
public class OpenAIAPIController {

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String url;

    @Autowired
    private IntentService intentService;

    @Autowired
    private EventService eventService;

    @Autowired
    private CalendarContext calendarContext;

    @Autowired
    private RestTemplate template;

    private final List<Message> conversationHistory = new ArrayList<>();
    private Event pendingEvent = null; // To hold the event awaiting confirmation

    @GetMapping("/chat")
    public String chat(@RequestParam("prompt") String prompt) {
        try {
            // Check for pending event confirmation
            if (pendingEvent != null) {
                if (isAffirmativeResponse(prompt)) {
                    String calendarId = calendarContext.getCalendarId();
                    String eventResponse = eventService.createEvent(calendarId, pendingEvent);
                    pendingEvent = null; // Reset after confirmation
                    return "✅ Event created successfully:\n" + eventResponse;
                } else {
                    pendingEvent = null; // Reset if user cancels
                    return "❌ Event creation canceled. Continuing chat...";
                }
            }

            String extractedJson = intentService.extractDetailsFromPrompt(prompt);

            // Debugging Logs
            System.out.println("🛠 Extracted JSON from ChatGPT:\n" + extractedJson);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(extractedJson);

            String extractedIntent = jsonNode.get("intent").asText();
            System.out.println("Extracted Intent from ChatGPT: " + extractedIntent);

            // Try mapping to IntentType
            IntentType intent;
            switch (extractedIntent.toUpperCase()) {
                case "CREATE":
                    intent = IntentType.CREATE_EVENT;
                    break;
                case "EDIT":
                    intent = IntentType.EDIT_EVENT;
                    break;
                case "DELETE":
                    intent = IntentType.DELETE_EVENT;
                    break;
                case "VIEW":
                    intent = IntentType.VIEW_EVENTS;
                    break;
                default:
                    intent = IntentType.NONE;
            }

            // If it's an event, return extracted details with confirmation
            if (intent == IntentType.CREATE_EVENT) {
                pendingEvent = parseEventDetails(jsonNode);
                return "Detected Intent: " + intent.name() + "\n\nYou are about to create the following event:\n" +
                        formatEventDetails(pendingEvent) +
                        "\nDo you want to proceed? (yes/no)";
            }

            // If no intent, proceed with regular chat
            return jsonNode.has("message") ? jsonNode.get("message").asText() : "Could not process request.";

        } catch (Exception e) {
            System.out.println("❌ Error processing request: " + e.getMessage());
            return "❌ Error: " + e.getMessage() + "\n\nCheck server logs for details.";
        }
    }

    private boolean isAffirmativeResponse(String response) {
        try {
            String analysisPrompt = "Analyze the following text and determine if it expresses agreement. " +
                    "Respond only with 'yes' or 'no'. Text: \"" + response + "\"";

            ChatGPTRequest request = new ChatGPTRequest();
            request.setModel(model);
            List<Message> messages = new ArrayList<>();
            messages.add(new Message("system", analysisPrompt));
            request.setMessages(messages);

            ChatGPTResponse chatGPTResponse = template.postForObject(url, request, ChatGPTResponse.class);
            String result = chatGPTResponse.getChoices().get(0).getMessage().getContent().trim().toLowerCase();
            return "yes".equals(result);
        } catch (Exception e) {
            System.err.println("❌ Error determining user response: " + e.getMessage());
            return false;
        }
    }


    private Event parseEventDetails(JsonNode jsonNode) {
        Event event = new Event();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        try {
            event.setSummary(jsonNode.get("summary").asText("No Title"));
            event.setDescription(jsonNode.get("description").asText(""));
            event.setLocation(jsonNode.get("location").asText(""));

            String startDateTime = jsonNode.get("start").asText();
            String endDateTime = jsonNode.get("end").asText();

            System.out.println("🔹 Extracted Start Time: " + startDateTime);
            System.out.println("🔹 Extracted End Time: " + endDateTime);

            // Parse start time
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = startDateTime != null && !startDateTime.isEmpty()
                    ? LocalDateTime.parse(startDateTime, formatter)
                    : now;

            // If the start time is in the past, move it to the next future occurrence
            if (start.isBefore(now)) {
                System.err.println("🚨 Start time is in the past! Adjusting to future occurrence...");
                start = start.plusYears(1);
            }

            // Parse end time or default to 1 hour later
            LocalDateTime end;
            if (endDateTime != null && !endDateTime.isEmpty()) {
                end = LocalDateTime.parse(endDateTime, formatter);

                // Ensure the end time is not before start
                if (end.isBefore(start)) {
                    System.err.println("🚨 End time is before start time! Adjusting...");
                    end = start.plusHours(1);
                }
            } else {
                System.out.println("🔄 No end time provided. Defaulting to 1 hour after start.");
                end = start.plusHours(1);
            }

            event.setStart(start);
            event.setEnd(end);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error parsing event details. Raw JSON: " + jsonNode.toPrettyString());
            event.setSummary("Default Event");
            event.setDescription("No Description");
            event.setLocation("No Location");
            event.setStart(LocalDateTime.now());
            event.setEnd(LocalDateTime.now().plusHours(1));
        }

        return event;
    }








    private String formatEventDetails(Event event) {
        return "Summary: " + event.getSummary() + "\n" +
                "Description: " + event.getDescription() + "\n" +
                "Location: " + event.getLocation() + "\n" +
                "Start: " + event.getStart().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\n" +
                "End: " + event.getEnd().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
