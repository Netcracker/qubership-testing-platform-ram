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

package org.qubership.atp.ram.deserializers;

import java.io.IOException;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.qubership.atp.ram.utils.DateTimeFormatUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DateDeserializer extends JsonDeserializer<Date> {

    private static final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime();

    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        String text = jsonParser.getText().trim();
        try {
            if (DateTimeFormatUtils.isMilliSecondsFormat(text)) {
                long milliSeconds = (long) (Double.parseDouble(text) * 1000);
                return new Date(milliSeconds);
            }

            DateTime dateTime = dateTimeFormatter.parseDateTime(text);
            return dateTime.toDate();
        } catch (Exception e) {
            log.error("Unable to parse provided string to date: " + text, e);
            return null;
        }
    }
}
