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

            // Handle CREATE event intent
            if (intent == IntentType.CREATE_EVENT) {
                pendingEvent = parseEventDetails(jsonNode);
                return "Detected Intent: " + intent.name() + "\n\nYou are about to create the following event:\n" +
                        formatEventDetails(pendingEvent) +
                        "\nDo you want to proceed? (yes/no)";
            }

            // Handle VIEW event intent
            if (intent == IntentType.VIEW_EVENTS) {
                return handleViewEvents(jsonNode);
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
            event.setId(jsonNode.has("id") ? jsonNode.get("id").asText() : "N/A");
            event.setSummary(jsonNode.get("summary").asText("No Title"));
            event.setDescription(jsonNode.get("description").asText(""));
            event.setLocation(jsonNode.get("location").asText(""));

            String startDateTime = jsonNode.get("start").asText();
            String endDateTime = jsonNode.get("end").asText();

            if (startDateTime != null && !startDateTime.isEmpty()) {
                event.setStart(LocalDateTime.parse(startDateTime, formatter));
            } else {
                event.setStart(LocalDateTime.now());  // Default to now if not provided
            }

            if (endDateTime != null && !endDateTime.isEmpty()) {
                event.setEnd(LocalDateTime.parse(endDateTime, formatter));
            } else {
                event.setEnd(event.getStart().plusHours(1));  // Default to 1 hour if not provided
            }
        } catch (Exception e) {
            e.printStackTrace();
            event.setId("N/A");
            event.setSummary("Default Event");
            event.setDescription("No Description");
            event.setLocation("No Location");
            event.setStart(LocalDateTime.now());
            event.setEnd(LocalDateTime.now().plusHours(1));
        }

        return event;
    }

    private String formatEventDetails(Event event) {
        return "ID: " + event.getId() + "\n" +
                "Summary: " + event.getSummary() + "\n" +
                "Description: " + event.getDescription() + "\n" +
                "Location: " + event.getLocation() + "\n" +
                "Start: " + event.getStart().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\n" +
                "End: " + event.getEnd().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String handleViewEvents(JsonNode jsonNode) {
        try {
            String calendarId = calendarContext.getCalendarId();
            String startDate = jsonNode.get("start").asText();
            String endDate = jsonNode.get("end").asText();

            System.out.println("📅 Extracted Date Range: " + startDate + " - " + endDate);

            List<Map<String, String>> events = eventService.getEventsInDateRange(calendarId, startDate, endDate);

            if (events.isEmpty()) {
                return "📌 No events found from " + startDate + " to " + endDate;
            }

            StringBuilder response = new StringBuilder("📅 Events from " + startDate + " to " + endDate + ":\n");
            for (Map<String, String> event : events) {
                response.append("\n🔹 *ID: ").append(event.get("id")).append("*")
                        .append("\n📌 Summary: ").append(event.get("summary"))
                        .append("\n📍 Location: ").append(event.getOrDefault("location", "Not specified"))
                        .append("\n🕒 Start: ").append(event.get("start"))
                        .append("\n🕒 End: ").append(event.get("end"))
                        .append("\n");
            }

            return response.toString();
        } catch (Exception e) {
            System.err.println("❌ Error fetching events: " + e.getMessage());
            return "❌ Failed to fetch events.";
        }
    }

}
