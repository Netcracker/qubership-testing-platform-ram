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

package org.qubership.atp.ram.job;

import static org.qubership.atp.ram.job.ExecutionRequestScheduler.getHostsOrBuilds;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ExecutionRequestSchedulerTest {
    private final static String json = "{"
            + "\"servers\": [\"http://example.com:0000\", \"http://qstp-example.com/some-endpoint\"]"
            + "}";
    private final static String jsonEmpty = "{"
            + "\"servers\": []"
            + "}";

    @Test
    public void getHostsOrBuilds_AddServersInListFromJsonStringIfThereAreNone_OneServerAdded() {
        List<String> expectedList = new ArrayList<>();
        expectedList.add("\"http://example.com:0000\"");
        expectedList.add("\"http://qstp-example.com/some-endpoint\"");

        JsonElement element = JsonParser.parseString(json);
        JsonObject jsonObject = element.getAsJsonObject();
        List<String> result = getHostsOrBuilds(createListForTest(), jsonObject.getAsJsonArray("servers"),
                "servers", "");
        Assertions.assertEquals(expectedList, result);
    }

    @Test
    public void getHostsOrBuilds_AddServersInListFromEmptyJsonStringIfThereAreNone_NoServersAdded() {
        List<String> expectedList = new ArrayList<>();
        expectedList.add("\"http://example.com:0000\"");

        JsonElement element = JsonParser.parseString(jsonEmpty);
        JsonObject jsonObject = element.getAsJsonObject();
        List<String> result = getHostsOrBuilds(createListForTest(), jsonObject.getAsJsonArray("servers"),
                "servers", "");
        Assertions.assertEquals(expectedList, result);
    }

    @Test
    public void getHostsOrBuilds_AddServersInListFromEmptyJsonStringIfThereAreNone_NoServerExists() {
        List<String> expectedList = new ArrayList<>();
        expectedList.add("\"http://example.com:0000\"");
        expectedList.add("\"http://qstp-example.com/some-endpoint\"");

        JsonElement element = JsonParser.parseString(json);
        JsonObject jsonObject = element.getAsJsonObject();
        List<String> result = getHostsOrBuilds(null, jsonObject.getAsJsonArray("servers"),
                "servers", "");
        Assertions.assertEquals(expectedList, result);
    }

    private List<String> createListForTest() {
        List<String> hostsInTr = new ArrayList<>();
        hostsInTr.add("\"http://example.com:0000\"");
        return hostsInTr;
    }
}
