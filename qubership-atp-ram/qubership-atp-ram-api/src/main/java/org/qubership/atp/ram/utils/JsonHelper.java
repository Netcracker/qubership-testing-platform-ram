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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.exceptions.internal.RamJsonObjectParseMissedFieldException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class JsonHelper {
    private static final Logger LOG = LoggerFactory.getLogger(JsonHelper.class);

    private JsonHelper() {
    }

    /**
     * Check value in {@link JsonObject} by key and return string value.
     *
     * @param jsonObject   {@link JsonObject} for find value
     * @param key          for find value
     * @param defaultValue return when value not exist or empty
     * @return value by key or default value
     */
    public static String getStringValue(JsonObject jsonObject, String key, String defaultValue) {
        if (jsonObject.has(key) && !Strings.isNullOrEmpty(jsonObject.get(key).getAsString())) {
            return jsonObject.get(key).getAsString();
        }
        LOG.debug("JSON has not string value by key {}", key);
        return Strings.nullToEmpty(defaultValue);
    }

    /**
     * Check value in {@link JsonObject} by key and return string value.
     *
     * @param jsonObject {@link JsonObject} for find value
     * @param key        for find value
     * @return value by key or empty
     */
    public static String getStringValue(JsonObject jsonObject, String key) {
        if (jsonObject.has(key) && !Strings.isNullOrEmpty(jsonObject.get(key).getAsString())) {
            return jsonObject.get(key).getAsString();
        }
        LOG.debug("JSON has not string value by key {}", key);
        return "";
    }

    /**
     * Check value in {@link JsonObject} by key and return string array.
     *
     * @param jsonObject   {@link JsonObject} for find value
     * @param key          for find value
     * @param defaultValue return when value not exist or empty
     * @return list of string by key or default list
     */
    public static List<String> getListString(JsonObject jsonObject, String key, List<String> defaultValue) {
        if (jsonObject.has(key) && !Strings.isNullOrEmpty(jsonObject.get(key).getAsString())) {
            List<String> result = new ArrayList<>();
            List<String> valuesFromString = Arrays.asList(jsonObject.get(key).getAsString().split(";"));

            if (!valuesFromString.isEmpty()) {
                valuesFromString.forEach(elem -> result.add(elem.trim()));
            }
            return result;
        }
        LOG.debug("JSON has not string value by key {}", key);
        return defaultValue;
    }

    /**
     * Check value in {@link JsonObject} by key and return long value.
     *
     * @param jsonObject {@link JsonObject} for find value
     * @param key        for find value
     * @return long value by key or System.currentTimeMillis
     */
    public static Long getLongValue(JsonObject jsonObject, String key) throws Exception {
        if (jsonObject.has(key)) {
            return jsonObject.get(key).getAsLong();
        }
        LOG.error("Failed to find a specified field '{}' in the JSON object", key);
        throw new RamJsonObjectParseMissedFieldException();
    }

    /**
     * Check value in {@link JsonObject} by key and return {@link ExecutionStatuses}.
     *
     * @param jsonObject {@link JsonObject} for find value
     * @param key        for find value
     * @return status by key or default status
     */
    public static ExecutionStatuses getExecutionStatus(JsonObject jsonObject, String key,
                                                       ExecutionStatuses defaultStatus) {
        if (jsonObject.has(key)) {
            String status = jsonObject.get(key).getAsString();
            ExecutionStatuses res = ExecutionStatuses.findByValue(status);
            if (res != null) {
                return res;
            }
            LOG.debug("Execution status {} not exist", status);
        } else {
            LOG.debug("JSON has not string value by key {}", key);
        }

        return defaultStatus;
    }

    /**
     * Check value in {@link JsonObject} by key and return {@link TestingStatuses}.
     *
     * @param jsonObject {@link JsonObject} for find value
     * @param key        for find value
     * @return status by key or default status
     */
    public static TestingStatuses getTestingStatus(JsonObject jsonObject, String key,
                                                   TestingStatuses defaultStatus) {
        if (jsonObject.has(key)) {
            String status = jsonObject.get(key).getAsString();
            TestingStatuses res = TestingStatuses.findByValue(status);
            if (res != null) {
                return res;
            }
            LOG.debug("Testing status {} not exist", status);
        } else {
            LOG.debug("JSON has not string value by key {}", key);
        }

        return defaultStatus;
    }

    /**
     * Check value in {@link JsonObject} by key and return boolean value.
     *
     * @param request {@link JsonObject} for find value
     * @param key     for find value
     * @return value by key or false
     */
    public static boolean getBooleanValue(JsonObject request, String key) {
        return request.has(key) && request.get(key).getAsBoolean();
    }

    /**
     * Check value in {@link JsonObject} by key and return HashSet.
     *
     * @param jsonObject   {@link JsonObject} for find value.
     * @param key          for find value.
     * @param defaultValue return when value not exist or empty.
     * @return value by key or empty
     */
    public static HashSet<String> getHashSet(JsonObject jsonObject, String key, HashSet<String> defaultValue) {
        HashSet<String> newSet = new HashSet<>();
        if (jsonObject.has(key) && !jsonObject.getAsJsonArray(key).isEmpty()) {
            JsonArray jsonArray = jsonObject.getAsJsonArray(key);
            jsonArray.forEach(jsonElement -> {
                newSet.add(jsonElement.getAsString());
            });
        }
        if (defaultValue == null || defaultValue.isEmpty()) {
            return newSet;
        } else {
            defaultValue.addAll(newSet);
            return defaultValue;
        }
    }

    public static UUID getUuidValue(JsonObject jsonObject, String key) {
        String value = getStringValue(jsonObject, key);
        return Strings.isNullOrEmpty(value) ? null : UUID.fromString(value);
    }
}
