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

package org.qubership.atp.ram;

import static java.util.Arrays.asList;

import java.util.List;

import org.qubership.atp.ram.models.LogRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface RamConstants {
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    Logger log = LoggerFactory.getLogger(LogRecord.class);

    // Send report
    String NAME = "NAME";
    String STATUS = "STATUS";
    String FIRST_STATUS = "FIRST STATUS";
    String FINAL_STATUS = "FINAL STATUS";
    String PASSED_RATE = "PASSED RATE";
    String ISSUE = "ISSUE";
    String DURATION = "DURATION";
    String FAILURE_REASON = "FAILURE REASON";
    String FAILED_STEP = "FAILED STEP";
    String FINAL_RUN = "FINAL RUN";
    String LABELS = "LABELS";
    String DATA_SET = "DATA SET";
    String JIRA_TICKET = "JIRA TICKET";
    String COMMENT = "COMMENT";
    List<String> DEFAULT_COLUMN_NAMES = asList(NAME, STATUS, FIRST_STATUS, FINAL_STATUS, PASSED_RATE, ISSUE, DURATION,
            FAILURE_REASON, FAILED_STEP, FINAL_RUN, LABELS, DATA_SET, JIRA_TICKET, COMMENT);

    String BROWSER_POD = "%{browser_pod}";
    String FROM_TIMESTAMP = "%{from_timestamp}";
    String TO_TIMESTAMP = "%{to_timestamp}";

}
