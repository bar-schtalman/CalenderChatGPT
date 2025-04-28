package com.handson.CalenderGPT.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventHistoryDTO {
    private String eventContext;
    private String actionDescription;
    private String timestamp;
}
