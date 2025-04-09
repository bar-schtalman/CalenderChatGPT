package com.handson.CalenderGPT.service;

import com.handson.CalenderGPT.model.PendingEventState;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class ClarificationService {

    private static final List<String> summaryPrompts = List.of(
            "What would you like to call this event?",
            "Give me a title for your event 🙂",
            "How should I name this event?",
            "Let’s give your event a name."
    );

    private static final List<String> timePrompts = List.of(
            "Got it! When should \"%s\" start and end?",
            "Cool, just tell me when \"%s\" happens.",
            "Let’s add this! What’s the time range for \"%s\"?",
            "Awesome. When do you want \"%s\" to take place?",
            "When does \"%s\" begin and end?"
    );

    private static final List<String> bothPrompts = List.of(
            "Let’s create an event! What should we call it, and when should it happen?",
            "Tell me the event name and its time.",
            "What’s the title and time range for your event?"
    );

    private final Random random = new Random();

    public String buildClarificationMessage(PendingEventState state) {
        boolean summaryMissing = state.getSummary().isEmpty();
        boolean startMissing = state.getStart().isEmpty();
        boolean endMissing = state.getEnd().isEmpty();

        // 🟠 All missing
        if (summaryMissing && startMissing && endMissing) {
            return randomFrom(bothPrompts);
        }

        // 🟡 Only summary missing
        if (summaryMissing) {
            return randomFrom(summaryPrompts);
        }

        // 🟢 Summary exists, time missing
        if (startMissing || endMissing) {
            return String.format(randomFrom(timePrompts), state.getSummary());
        }

        return "Please provide the missing event details.";
    }

    private String randomFrom(List<String> list) {
        return list.get(random.nextInt(list.size()));
    }
}
