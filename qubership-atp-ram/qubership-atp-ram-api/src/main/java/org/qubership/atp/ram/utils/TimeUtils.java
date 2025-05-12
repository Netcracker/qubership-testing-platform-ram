/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.ram.utils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

    public static final String DEFAULT_DATE_TIME_PATTERN = "dd.MM.yyyy, HH:mm:ss";

    private TimeUtils() {
    }

    /**
     * Returns duration in seconds.
     */
    public static long getDuration(Timestamp startDate, Timestamp finishDate) {
        if (startDate == null) {
            startDate = new Timestamp(System.currentTimeMillis());
        }
        if (finishDate == null) {
            finishDate = new Timestamp(System.currentTimeMillis());
        }
        long duration = finishDate.getTime() - startDate.getTime();
        return TimeUnit.MILLISECONDS.toSeconds(duration);
    }

    public static String formatDateTime(Timestamp timestamp, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return timestamp.toLocalDateTime().format(formatter);
    }

    /**
     * Format date by provided pattern and timezone.
     *
     * @param timestamp date value
     * @param pattern pattern
     * @param timezone time zone
     * @return formatted string date value
     */
    public static String formatDateTime(Timestamp timestamp, String pattern, String timezone) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timezone));

        return simpleDateFormat.format(timestamp);
    }
}
