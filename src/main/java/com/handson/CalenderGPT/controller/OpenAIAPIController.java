package com.handson.CalenderGPT.controller;

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
            IntentType intent = intentService.detectIntent(prompt);
            String intentMessage = "Detected Intent: " + intent.name() + "\n\n";

            if (intent != IntentType.NONE) {
                String extractedDetails = intentService.extractDetailsFromPrompt(prompt, intent);

                if (intent == IntentType.CREATE_EVENT) {
                    System.out.println("Extracted Details: " + extractedDetails);
                    Event eventRequest = parseEventDetails(extractedDetails);
                    pendingEvent = eventRequest;
                    return intentMessage + "You are about to create the following event:\n" +
                            formatEventDetails(eventRequest) +
                            "\nDo you want to proceed? (yes/no)";
                }

                return intentMessage + extractedDetails;
            }

            if (pendingEvent != null && (prompt.equalsIgnoreCase("yes") || prompt.equalsIgnoreCase("no"))) {
                if (prompt.equalsIgnoreCase("yes")) {
                    String calendarId = calendarContext.getCalendarId();
                    String eventResponse = eventService.createEvent(calendarId, pendingEvent);
                    pendingEvent = null; // Reset after confirmation
                    return "Event created successfully: " + eventResponse;
                } else {
                    pendingEvent = null; // Reset if user cancels
                    return "Event creation canceled.";
                }
            }

            conversationHistory.add(new Message("user", prompt));

            ChatGPTRequest chatGPTRequest = new ChatGPTRequest();
            chatGPTRequest.setModel(model);
            chatGPTRequest.setMessages(conversationHistory);

            ChatGPTResponse chatGPTResponse = template.postForObject(url, chatGPTRequest, ChatGPTResponse.class);
            String assistantReply = chatGPTResponse.getChoices().get(0).getMessage().getContent();

            conversationHistory.add(new Message("assistant", assistantReply));
            return intentMessage + assistantReply;

        } catch (HttpClientErrorException.TooManyRequests e) {
            return "Error: You have exceeded your API usage quota. Please check your OpenAI plan.";
        } catch (Exception e) {
            return "An unexpected error occurred. Please try again later. " + e.toString();
        }
    }

    // Updated method to parse extracted details into an Event object
    private Event parseEventDetails(String extractedDetails) {
        ObjectMapper mapper = new ObjectMapper();
        Event event = new Event();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        try {
            Map<String, String> detailsMap = mapper.readValue(extractedDetails, Map.class);
            event.setSummary(detailsMap.getOrDefault("summary", "No Title"));
            event.setDescription(detailsMap.getOrDefault("description", ""));
            event.setLocation(detailsMap.getOrDefault("location", ""));

            String startDateTime = detailsMap.get("start");
            String endDateTime = detailsMap.get("end");

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
            // Fallback in case parsing fails
            event.setSummary("Default Event");
            event.setDescription("No Description");
            event.setLocation("No Location");
            event.setStart(LocalDateTime.now());
            event.setEnd(LocalDateTime.now().plusHours(1));
        }

        return event;
    }

    // Format event details for confirmation message
    private String formatEventDetails(Event event) {
        return "Summary: " + event.getSummary() + "\n" +
                "Description: " + event.getDescription() + "\n" +
                "Location: " + event.getLocation() + "\n" +
                "Start: " + event.getStart().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\n" +
                "End: " + event.getEnd().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
