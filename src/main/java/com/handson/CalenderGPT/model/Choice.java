package com.handson.CalenderGPT.model; // Adjust if needed

public class Choice {

    private int index;
    private Message message;   // Must exist if you want .getMessage()
    private String finish_reason;

    // Constructors
    public Choice() {
    }

    public Choice(int index, Message message, String finish_reason) {
        this.index = index;
        this.message = message;
        this.finish_reason = finish_reason;
    }

    // Getters and Setters
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Message getMessage() {   // <-- This is critical
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public String getFinish_reason() {
        return finish_reason;
    }

    public void setFinish_reason(String finish_reason) {
        this.finish_reason = finish_reason;
    }
}
