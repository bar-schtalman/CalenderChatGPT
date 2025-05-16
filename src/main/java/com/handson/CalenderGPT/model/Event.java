package com.handson.CalenderGPT.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Event {

    private String id;
    private String summary;
    private String description;
    private String location;
    private LocalDateTime start;
    private LocalDateTime end;
    private List<String> guests;
    private String timeZone;

}
