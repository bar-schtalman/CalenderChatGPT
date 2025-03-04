package com.handson.CalenderGPT.util;

import com.google.api.client.util.DateTime;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateTimeUtils {
    public static DateTime convertToGoogleDateTime(LocalDateTime localDateTime, String timeZoneId) {
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of(timeZoneId));
        return new DateTime(zonedDateTime.toInstant().toEpochMilli());
    }
}
