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

import java.util.List;

import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(NON_NULL)
@Data
@Document(collection = "logrecordStepContextVariables")
public class LogRecordStepContextVariable extends LogRecordContextVariableObject {

    @JsonIgnore
    protected List<ContextVariable> stepContextVariables;

    @Override
    public List<ContextVariable> getContextVariables() {
        return stepContextVariables;
    }

    @Override
    public void setContextVariables(List<ContextVariable> contextVariables) {
        this.stepContextVariables = contextVariables;
    }
}
