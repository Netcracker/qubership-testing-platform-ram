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
        Date date = deserializer.deserialize(prepareParser("{ \"value\":\"1698934523549\" }"), mapper.getDeserializationContext());
        Assertions.assertEquals("2023-11-02 14:15:23.549", date.toString());
    }

    @Test
    public void deserialize_dateWithDot_passedResult() throws IOException {
        Date date = deserializer.deserialize(prepareParser("{ \"value\":\"1698934523.549\" }"), mapper.getDeserializationContext());
        Assertions.assertEquals("2023-11-02 14:15:23.549", date.toString());
    }

    @Test
    public void deserialize_dateWithDotAndZeros_passedResult() throws IOException {
        Date date = deserializer.deserialize(prepareParser("{ \"value\":\"1699346917.180000000\" }"), mapper.getDeserializationContext());
        Assertions.assertEquals("2023-11-07 08:48:37.18", date.toString());
    }

    @Test
    public void deserialize_dateTimeFormat_passedResult() throws IOException {
        Date date = deserializer.deserialize(prepareParser("{ \"value\":\"2019-06-13T13:04:48.301+02:00\" }"), mapper.getDeserializationContext());
        Assertions.assertEquals("2019-06-13 11:04:48.301", date.toString());
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
