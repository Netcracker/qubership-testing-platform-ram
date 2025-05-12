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

package org.qubership.atp.ram.dto.response.logrecord;

import java.util.List;
import java.util.Map;

import org.qubership.atp.ram.dto.response.LogRecordShort;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * R_B_M log record.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RbmLogRecordShort extends LogRecordShort {

    /**
     * ER/AR.
     */
    private ValidationTable validationTable;

    /**
     * Query name and query.
     */
    private String sqlCommand;

    /**
     * Query result.
     */
    private Map<String, List<String>> result;
}
