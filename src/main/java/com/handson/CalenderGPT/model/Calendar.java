package com.handson.CalenderGPT.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Calendar {
    private String id;
    private String summary;
    private String timeZone;
    private List<Event> events;
}
