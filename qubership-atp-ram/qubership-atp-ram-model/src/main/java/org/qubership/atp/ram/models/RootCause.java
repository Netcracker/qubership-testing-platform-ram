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

import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.javers.core.metamodel.annotation.DiffInclude;
import org.javers.core.metamodel.annotation.TypeName;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "rootCause")
@CompoundIndex(name = "_id_parentId", def = "{'_id': 1, 'parentId': 1}")
@NoArgsConstructor
@ToString(callSuper = true)
@TypeName("rootCause")
public class RootCause extends DateAuditorEntity {

    private UUID parentId;
    private UUID projectId;
    @NotNull
    @DiffInclude
    private RootCauseType type;
    @DiffInclude
    private boolean disabled;
    private boolean isDefault;

    /**
     * RootCause constructor.
     */
    public RootCause(UUID uuid, RootCauseType type, String name) {
        this.name = name;
        this.uuid = uuid;
        this.type = type;
    }
}
