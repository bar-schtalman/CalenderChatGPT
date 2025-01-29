package com.handson.CalenderGPT.model;

import java.util.List;

public class ChatGPTRequest {

    private String model;
    private List<Message> messages;

    // No-args constructor
    public ChatGPTRequest() {}

    // All-args constructor
    public ChatGPTRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    // Getters and setters
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}

