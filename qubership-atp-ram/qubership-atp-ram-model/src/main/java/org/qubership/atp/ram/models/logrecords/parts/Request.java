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

package org.qubership.atp.ram.models.logrecords.parts;

import static org.qubership.atp.ram.RamConstants.OBJECT_MAPPER;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@JsonIgnoreProperties(
        ignoreUnknown = true
)
public class Request {
    private String endpoint;
    private String method;
    private Map<String, String> headers;
    private List<RequestHeader> headersList;
    private Timestamp timestamp;
    private String body;
    private boolean htmlBody;

    /**
     * Constructor.
     */
    public Request(String endpoint, String method, Map<String, String> headers, List<RequestHeader> headersList,
                   Timestamp timestamp, String body) {
        this.endpoint = endpoint;
        this.method = method;
        this.headers = headers;
        this.headersList = headersList;
        this.timestamp = timestamp;
        this.body = body;
        this.htmlBody = false;
    }

    @Override
    public String toString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("Can not parse Request(endpoint = {}; method = {}; headers = {}; timestamp = {}; "
                    + "body = {}) to json.", endpoint, method, headers, timestamp, body, e);
        }
        return "";
    }
}
