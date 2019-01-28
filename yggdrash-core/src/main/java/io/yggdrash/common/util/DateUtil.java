package io.yggdrash.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    //zoneId = ZoneId.systemDefault();
    private static ZoneId zoneId = ZoneId.of("UTC");

    public static String generateDateToFormat(ZoneId zone, String format) {
        LocalDateTime dateTime = LocalDateTime.now(zone == null ? zoneId : zone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return dateTime.atZone(zone == null ? zoneId : zone).format(formatter);
    }

    public static LocalDateTime generateStringFormatToDate(String dateStr, String format) {
        return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern(format));
    }

    public static long getNowSeconds(ZoneId zone) {
        LocalDateTime dateTime = LocalDateTime.now();
        return dateTime.atZone(zone == null ? zoneId : zone).toEpochSecond();
    }
}
