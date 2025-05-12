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
import java.util.UUID;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import org.javers.core.metamodel.annotation.DiffInclude;
import org.javers.core.metamodel.annotation.TypeName;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(NON_NULL)
@Document(collection = "reporttemplates")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeName("reporttemplates")
public class ReportTemplate extends DateAuditorEntity {

    @DiffInclude
    private List<WidgetType> widgets;

    @Indexed(name = "project_id_idx")
    private UUID projectId;

    @DiffInclude
    private boolean active;

    @NotBlank
    @DiffInclude
    private String subject;

    @DiffInclude
    private List<@Email String> recipients;

    /**
     * ReportTemplate constructor.
     */
    public ReportTemplate(@NotBlank String subject,
                          List<@Email String> recipients,
                          List<WidgetType> widgets,
                          boolean active) {
        this.widgets = widgets;
        this.active = active;
        this.subject = subject;
        this.recipients = recipients;
    }
}
