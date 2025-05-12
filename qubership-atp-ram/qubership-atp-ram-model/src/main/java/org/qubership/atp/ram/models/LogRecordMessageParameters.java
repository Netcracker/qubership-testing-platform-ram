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
import static org.qubership.atp.ram.models.RamObject.ID_FIELD;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.dto.response.MessageParameter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "logrecordMessageParameters")
public class LogRecordMessageParameters {

    @Id
    @Field(ID_FIELD)
    private UUID id;

    @CreatedDate
    private Timestamp createdDate;

    @JsonIgnore
    private List<MessageParameter> messageParameters;

    @Indexed(background = true)
    private UUID testRunId;
}
