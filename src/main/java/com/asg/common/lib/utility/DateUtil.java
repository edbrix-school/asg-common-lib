package com.asg.common.lib.utility;

import com.asg.common.lib.security.util.UserContext;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class DateUtil {

    private DateUtil() {
    }

    public static LocalDateTime getCurrentDateTimeInUserTimeZone() {
        String timeZoneCode = UserContext.getTimeZoneCode();
        ZoneId zoneId = ZoneId.of(timeZoneCode);
        return ZonedDateTime.now(zoneId).toLocalDateTime();
    }

    public static LocalDate getCurrentDateInUserTimeZone() {
        String timeZoneCode = UserContext.getTimeZoneCode();
        ZoneId zoneId = ZoneId.of(timeZoneCode);
        return ZonedDateTime.now(zoneId).toLocalDate();
    }

    public static Timestamp getCurrentDateTimeInUserTimeZoneTimeStamp() {
        String timeZoneCode = UserContext.getTimeZoneCode();
        ZoneId zoneId = ZoneId.of(timeZoneCode);
        return Timestamp.valueOf(ZonedDateTime.now(zoneId).toLocalDateTime());
    }
}
