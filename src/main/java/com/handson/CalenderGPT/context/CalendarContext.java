package com.handson.CalenderGPT.context;

import org.springframework.stereotype.Component;

@Component
public class CalendarContext {

    private String calendarId;

    public String getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }
}
