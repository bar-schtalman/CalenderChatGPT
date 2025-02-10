package com.handson.CalenderGPT.service;

import com.handson.CalenderGPT.model.IntentType;
import com.handson.CalenderGPT.model.ChatGPTRequest;
import com.handson.CalenderGPT.model.ChatGPTResponse;
import com.handson.CalenderGPT.model.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    public IntentType detectIntent(String userInput) {
        String input = userInput.toLowerCase();

        if (input.contains("create") || input.contains("schedule") || input.contains("add event") || input.contains("set up") || input.contains("plan")) {
            return IntentType.CREATE_EVENT;
        } else if (input.contains("edit") || input.contains("update") || input.contains("reschedule") || input.contains("change time") || input.contains("modify")) {
            return IntentType.EDIT_EVENT;
        } else if (input.contains("delete") || input.contains("remove") || input.contains("cancel") || input.contains("discard event") || input.contains("erase")) {
            return IntentType.DELETE_EVENT;
        } else if (input.contains("show") || input.contains("view") || input.contains("list") || input.contains("what's on my calendar") || input.contains("display")) {
            return IntentType.VIEW_EVENTS;
        }

        return IntentType.NONE;  // No matching intent found
    }

    public String extractDetailsFromPrompt(String prompt, IntentType intent) {
        String extractionPrompt = buildExtractionPrompt(prompt, intent);

        ChatGPTRequest extractionRequest = new ChatGPTRequest();
        extractionRequest.setModel(model);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", extractionPrompt));
        extractionRequest.setMessages(messages);

        ChatGPTResponse response = template.postForObject(url, extractionRequest, ChatGPTResponse.class);
        return response.getChoices().get(0).getMessage().getContent();
    }

    private String buildExtractionPrompt(String prompt, IntentType intent) {
        switch (intent) {
            case CREATE_EVENT:
                return "Extract the event creation details from the following text and return them in JSON format with these fields: " +
                        "summary, date, time (if provided), location (if provided), description (if provided). Text: \"" + prompt + "\"";

            case EDIT_EVENT:
                return "Extract the event editing details from the following text and return them in JSON format with these fields: " +
                        "event_id, fields_to_update (summary, date, time, location, description). Text: \"" + prompt + "\"";

            case DELETE_EVENT:
                return "Extract the event deletion details from the following text and return them in JSON format with this field: " +
                        "event_id. Text: \"" + prompt + "\"";

            case VIEW_EVENTS:
                return "Extract the date range from the following text for viewing events and return it in JSON format with these fields: " +
                        "start_date, end_date, start_time (if provided), end_time (if provided). Text: \"" + prompt + "\"";

            default:
                return "Analyze the following prompt and respond accordingly: \"" + prompt + "\"";
        }
    }
}
