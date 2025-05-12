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

package org.qubership.atp.ram.models;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.util.UUID;

import org.qubership.atp.ram.entities.AkbContext;
import org.qubership.atp.ram.enums.DefaultRootCauseType;
import org.qubership.atp.ram.enums.MaskCondition;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "akbRecords")
@JsonInclude(NON_NULL)
public class AkbRecord extends RamObject {

    private UUID rootCauseId;
    private String messageRegularExpression;
    private UUID defectId;
    private UUID projectId;
    private String nameRegularExpression;
    private MaskCondition maskCondition;
    private String comments;
    private AkbContext akbContext;
    private DefaultRootCauseType defaultRootCauseType;
}
