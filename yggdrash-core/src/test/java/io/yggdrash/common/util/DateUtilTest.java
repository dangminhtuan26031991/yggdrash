package io.yggdrash.common.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class DateUtilTest {
    @Test
    public void generateDateToFormat() {
        String format = "yyyy-MM-dd'T'HH-mm-ss.SSSSSS'Z'";
        String dateStr = DateUtil.generateDateToFormat(null, format);
        assertNotNull(dateStr);
        assertNotNull(DateUtil.generateStringFormatToDate(dateStr, format));
    }

    @Test
    public void getNowSeconds() throws Exception {
        long seconds = DateUtil.getNowSeconds(null);
        Thread.sleep(1000);
        long secondss = DateUtil.getNowSeconds(null);

        assertNotEquals(0, seconds);
        assertNotEquals(0, secondss);
    }
}
