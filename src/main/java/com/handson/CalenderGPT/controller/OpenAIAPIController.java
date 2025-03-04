package com.handson.CalenderGPT.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.model.Event;
import com.handson.CalenderGPT.model.IntentType;
import com.handson.CalenderGPT.model.Message;
import com.handson.CalenderGPT.service.ChatGPTService;
import com.handson.CalenderGPT.service.EventService;
import com.handson.CalenderGPT.service.IntentService;
import com.handson.CalenderGPT.context.CalendarContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class OpenAIAPIController {

    // Common DateTimeFormatters
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DateTimeFormatter OUTPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter OUTPUT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Autowired
    private IntentService intentService;

    @Autowired
    private EventService eventService;

    @Autowired
    private CalendarContext calendarContext;

    @Autowired
    private ChatGPTService chatGPTService;

    // Conversation history still stores all messages (for context with ChatGPT)
    private final List<Message> conversationHistory = new ArrayList<>();

    @GetMapping("/chat")
    public String chat(@RequestParam("prompt") String prompt) {
        try {
            String extractedJson = intentService.extractDetailsFromPrompt(prompt);
            System.out.println("🛠 Extracted JSON from ChatGPT:\n" + extractedJson);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(extractedJson);

            IntentType intent = mapIntentType(jsonNode.get("intent").asText());
            System.out.println("Extracted Intent from ChatGPT: " + intent);

            if (intent == IntentType.VIEW_EVENTS) {
                return handleViewEvents(jsonNode);
            }
            if (intent == IntentType.CREATE_EVENT) {
                return handleCreateEvent(jsonNode);
            }
            if (intent != IntentType.NONE) {
                return "Detected Intent: " + intent.name() + "\n\nExtracted Details:\n" + jsonNode.toPrettyString();
            }
            return chatWithGPT(prompt);
        } catch (Exception e) {
            System.out.println("❌ Error processing request: " + e.getMessage());
            return "❌ Error: " + e.getMessage() + "\n\nCheck server logs for details.";
        }
    }

    // Maps the extracted intent string to our IntentType enum.
    private IntentType mapIntentType(String extractedIntent) {
        switch (extractedIntent.toUpperCase()) {
            case "CREATE":
                return IntentType.CREATE_EVENT;
            case "EDIT":
                return IntentType.EDIT_EVENT;
            case "DELETE":
                return IntentType.DELETE_EVENT;
            case "VIEW":
                return IntentType.VIEW_EVENTS;
            default:
                return IntentType.NONE;
        }
    }

    // Handles VIEW intent: builds a response listing events with formatted date, time, summary, and location (if provided).
    private String handleViewEvents(JsonNode jsonNode) throws Exception {
        String start = jsonNode.get("start").asText();
        String end = jsonNode.get("end").asText();
        String calendarId = calendarContext.getCalendarId(); // ✅ Get Calendar ID

        List<Map<String, String>> events = eventService.getEventsInDateRange(calendarId, start, end);
        if (events.isEmpty()) {
            return "[{\"role\": \"ai\", \"content\": \"No events found between " + start + " and " + end + ".\"}]";
        }

        List<Map<String, String>> responseList = new ArrayList<>();
        for (Map<String, String> event : events) {
            Map<String, String> eventData = new HashMap<>();
            eventData.put("role", "event");
            eventData.put("summary", event.get("summary"));
            eventData.put("date", event.get("start").split(" ")[0]); // Extract date only
            eventData.put("time", event.get("start").split(" ")[1] + " - " + event.get("end").split(" ")[1]); // Extract time range
            eventData.put("location", event.getOrDefault("location", "No location"));
            eventData.put("calendarId", calendarId); // ✅ Include Calendar ID
            eventData.put("id", event.get("id")); // ✅ Ensure Event ID is included
            responseList.add(eventData);
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(responseList);
    }

    // Handles CREATE intent: parses event details, creates the event, adds context, and returns a confirmation.
    private String handleCreateEvent(JsonNode jsonNode) throws Exception {
        Event event = parseEventDetails(jsonNode);
        String calendarId = calendarContext.getCalendarId();
        // Create the event and get response from the API (this part is now not returned to the user).
        eventService.createEvent(calendarId, event);

        String eventDate = event.getStart().format(OUTPUT_DATE_FORMATTER);
        String eventStartTime = event.getStart().format(OUTPUT_TIME_FORMATTER);
        String eventEndTime = event.getEnd().format(OUTPUT_TIME_FORMATTER);

        String confirmationMessage = event.getSummary() + " Event created successfully: at "
                + eventDate + ", " + eventStartTime + " - " + eventEndTime;
        // Add a system message to the conversation history with event details.
        conversationHistory.add(new Message("system", "Event created: "
                + event.getSummary() + " at " + event.getLocation()
                + " on " + eventDate + " starting at " + eventStartTime + "."));

        // Return only the confirmation message, without the Google API response.
        return confirmationMessage;
    }

    // Parses event details from the JSON and builds an Event object.
    private Event parseEventDetails(JsonNode jsonNode) {
        Event event = new Event();
        try {
            event.setId(jsonNode.has("id") ? jsonNode.get("id").asText() : "N/A");
            event.setSummary(jsonNode.get("summary").asText("No Title"));
            event.setDescription(jsonNode.get("description").asText(""));
            event.setLocation(jsonNode.get("location").asText(""));

            String startDateTime = jsonNode.get("start").asText();
            String endDateTime = jsonNode.get("end").asText();

            if (startDateTime != null && !startDateTime.isEmpty()) {
                event.setStart(LocalDateTime.parse(startDateTime, ISO_FORMATTER));
            } else {
                event.setStart(LocalDateTime.now());
            }

            if (endDateTime != null && !endDateTime.isEmpty()) {
                event.setEnd(LocalDateTime.parse(endDateTime, ISO_FORMATTER));
            } else {
                event.setEnd(event.getStart().plusHours(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Use fallback values if parsing fails.
            event.setId("N/A");
            event.setSummary("Default Event");
            event.setDescription("No Description");
            event.setLocation("No Location");
            event.setStart(LocalDateTime.now());
            event.setEnd(LocalDateTime.now().plusHours(1));
        }
        return event;
    }

    // Delegates non-event prompts to ChatGPT using the shared service.
// Delegates non-event prompts to ChatGPT using the shared service.
    private String chatWithGPT(String prompt) {
        conversationHistory.add(new Message("user", prompt));
        List<Message> messages = new ArrayList<>(conversationHistory);
        String assistantReply = chatGPTService.callChatGPT(messages)
                .getChoices().get(0).getMessage().getContent();

        // Ensures response is returned as a single message (no line breaks)
        assistantReply = assistantReply.replaceAll("\\n+", " ");

        conversationHistory.add(new Message("assistant", assistantReply));
        return assistantReply;
    }

}
