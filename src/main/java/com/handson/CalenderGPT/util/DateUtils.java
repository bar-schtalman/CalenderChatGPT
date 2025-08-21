package com.handson.CalenderGPT.util;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.EventDateTime;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final DateTimeFormatter HUMAN_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final String DEFAULT_TZ = "Asia/Jerusalem";

    /**
     * Converts a LocalDateTime + timeZone to Google’s millisecond‐since‐epoch DateTime.
     */
    public static DateTime toGoogle(LocalDateTime ldt, String timeZone) {
        ZoneId zone = (timeZone != null && !timeZone.isEmpty()) ? ZoneId.of(timeZone) : ZoneId.of(DEFAULT_TZ);
        Instant instant = ldt.atZone(zone).toInstant();
        return new DateTime(instant.toEpochMilli());
    }

    /**
     * Converts Google’s DateTime back to a LocalDateTime in system default zone.
     */
    public static LocalDateTime fromGoogle(DateTime dt) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(dt.getValue()), ZoneId.of(DEFAULT_TZ));
    }

    /**
     * Formats a Google EventDateTime as “dd-MM-yyyy HH:mm” or “N/A” if missing.
     */
    public static String format(EventDateTime edt) {
        String tz = (edt.getTimeZone() != null && !edt.getTimeZone().isBlank())
                ? edt.getTimeZone()
                : DEFAULT_TZ;
        if (edt.getDateTime() == null){
            LocalDateTime ldt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(edt.getDate().getValue()),
                    ZoneId.of(tz)
            );
            return ldt.toLocalDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        }
        LocalDateTime ldt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(edt.getDateTime().getValue()),
                ZoneId.of(tz)
        );
        return ldt.format(HUMAN_FORMAT);

    }
}
