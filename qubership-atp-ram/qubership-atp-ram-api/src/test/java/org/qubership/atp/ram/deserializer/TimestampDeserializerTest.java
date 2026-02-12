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

package org.qubership.atp.ram.deserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.ram.deserializers.TimestampDeserializer;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
public class TimestampDeserializerTest {

    private ObjectMapper mapper;
    private TimestampDeserializer deserializer;

    @BeforeEach
    public void setup() {
        mapper = new ObjectMapper();
        deserializer = new TimestampDeserializer();
    }

    @Test
    public void deserialize_dateWithoutDot_passedResult() throws IOException {
        long timeStamp = 1698934523549L;
        Date expectedDate = new Date(timeStamp);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        Date date = deserializer.deserialize(prepareParser("{ \"value\":\"" + timeStamp + "\" }"),
                mapper.getDeserializationContext());
        Assertions.assertEquals(expectedDate.toInstant().atZone(ZoneId.systemDefault()).format(formatter),
                date.toString()); // "2023-11-02 14:15:23.549" - in case we are in GMT+0
    }

    @Test
    public void deserialize_dateWithDot_passedResult() throws IOException {
        long seconds = 1698934523L;
        int millis = 549;
        Date expectedDate = new Date(seconds * 1000 + millis);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        Date date = deserializer.deserialize(prepareParser("{ \"value\":\"" + seconds + "." + millis + "\" }"),
                mapper.getDeserializationContext());
        Assertions.assertEquals(expectedDate.toInstant().atZone(ZoneId.systemDefault()).format(formatter),
                date.toString()); // "2023-11-02 14:15:23.549" - in case we are in GMT+0
    }

    @Test
    public void deserialize_dateWithDotAndZeros_passedResult() throws IOException {
        long seconds = 1699346917L;
        int millis = 180;
        Date expectedDate = new Date(seconds * 1000 + millis);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS");
        Date date = deserializer.deserialize(prepareParser("{ \"value\":\"" + seconds + "." + millis + "000000\" }"),
                mapper.getDeserializationContext());
        Assertions.assertEquals(expectedDate.toInstant().atZone(ZoneId.systemDefault()).format(formatter),
                date.toString()); // "2023-11-07 08:48:37.18" - in case we are in GMT+0
    }

    @Test
    public void deserialize_dateTimeFormat_passedResult() throws IOException {
        String dateString = "2019-06-13T13:04:48.301+02:00";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("GMT+2"));
        Date expectedDate = Date.from(Instant.from(formatter.parse(dateString.substring(0, 23).replace('T', ' '))));
        Date date = deserializer.deserialize(prepareParser("{ \"value\":\"" + dateString + "\" }"),
                mapper.getDeserializationContext());
        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        Assertions.assertEquals(expectedDate.toInstant().atZone(ZoneId.systemDefault()).format(formatter1),
                date.toString()); // "2019-06-13 11:04:48.301" - in case we are in GMT+0
    }

    @Test
    public void deserialize_dateNull_passedResult() throws IOException {
        Date date = deserializer.deserialize(prepareParser("{ \"value\":\"\" }"), mapper.getDeserializationContext());
        Assertions.assertNull(date);
    }

    private JsonParser prepareParser(String json) throws IOException {
        JsonParser parser = mapper.getFactory().createParser(json);
        while (parser.nextToken() != JsonToken.VALUE_STRING);
        return parser;
    }

}
