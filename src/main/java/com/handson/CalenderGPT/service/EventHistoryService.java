package com.handson.CalenderGPT.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.CalenderGPT.dto.EventHistoryDTO;
import com.handson.CalenderGPT.model.EventHistory;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EventHistoryService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public List<EventHistoryDTO> formatEventHistories(List<EventHistory> histories) {
        List<EventHistoryDTO> result = new ArrayList<>();

        for (EventHistory history : histories) {
            result.add(generateDTO(history));
        }

        return result;
    }

    private EventHistoryDTO generateDTO(EventHistory history) {
        try {
            String action = history.getAction();
            Map<String, Object> oldData = history.getOldData() != null ? objectMapper.readValue(history.getOldData(), Map.class) : null;
            Map<String, Object> newData = history.getNewData() != null ? objectMapper.readValue(history.getNewData(), Map.class) : null;

            String eventTitle = (newData != null && newData.containsKey("summary")) ? newData.get("summary").toString() : (oldData != null && oldData.containsKey("summary")) ? oldData.get("summary").toString() : "Unknown Event";

            String eventDate = (newData != null && newData.containsKey("date")) ? newData.get("date").toString() : (oldData != null && oldData.containsKey("date")) ? oldData.get("date").toString() : "Unknown Date";

            String eventContext = "📅 " + eventTitle + " at " + eventDate;

            String actionDescription;

            switch (action) {
                case "CREATE":
                    actionDescription = "New event created.";
                    break;
                case "DELETE":
                    actionDescription = "Event was deleted.";
                    break;
                case "ADD_GUESTS":
                    String addedGuest = (newData != null && newData.containsKey("guests")) ? newData.get("guests").toString() : "Unknown guest";
                    actionDescription = "Guest " + addedGuest + " was added.";
                    break;
                case "REMOVE_GUESTS":
                    String removedGuest = (oldData != null && oldData.containsKey("guests")) ? oldData.get("guests").toString() : "Unknown guest";
                    actionDescription = "Guest " + removedGuest + " was removed.";
                    break;
                case "UPDATE":
                    if (oldData != null && newData != null) {
                        if (!oldData.getOrDefault("summary", "").equals(newData.getOrDefault("summary", ""))) {
                            actionDescription = "Title changed from \"" + oldData.get("summary") + "\" to \"" + newData.get("summary") + "\".";
                        } else if (!oldData.getOrDefault("time", "").equals(newData.getOrDefault("time", ""))) {
                            actionDescription = "Time changed from \"" + oldData.get("time") + "\" to \"" + newData.get("time") + "\".";
                        } else if (!oldData.getOrDefault("date", "").equals(newData.getOrDefault("date", ""))) {
                            actionDescription = "Date changed from \"" + oldData.get("date") + "\" to \"" + newData.get("date") + "\".";
                        } else {
                            actionDescription = "Event updated.";
                        }
                    } else {
                        actionDescription = "Event updated.";
                    }
                    break;
                default:
                    actionDescription = "Event change occurred.";
            }

            return new EventHistoryDTO(eventContext, actionDescription, history.getTimestamp().toString());

        } catch (Exception e) {
            return new EventHistoryDTO("Unknown event", "⚠️ Error parsing event history.", history.getTimestamp().toString());
        }
    }
}
