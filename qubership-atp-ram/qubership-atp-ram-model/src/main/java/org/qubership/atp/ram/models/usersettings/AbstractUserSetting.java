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

package org.qubership.atp.ram.models.usersettings;

import static org.qubership.atp.ram.models.RamObject.NAME_FIELD;

import java.util.UUID;

import org.qubership.atp.ram.models.RamObject;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
// Cannot map classes programmatically, see https://github.com/FasterXML/jackson-databind/issues/2950
@JsonSubTypes({
        @JsonSubTypes.Type(
                name = UserSettingType.Constants.ER_TABLE_COLUMNS_VISIBILITY,
                value = TableColumnVisibilityUserSetting.class
        ),
        @JsonSubTypes.Type(
                name = UserSettingType.Constants.TC_EXECUTION_HISTORY_TABLE_COLUMNS_VISIBILITY,
                value = TableColumnVisibilityUserSetting.class
        )
})
@Data
@Document(collection = "usersettings")
@JsonIgnoreProperties({NAME_FIELD})
@CompoundIndexes({
        @CompoundIndex(name = "_userId_type", def = "{'userId': 1, 'type': 1}", unique = true)
})
public abstract class AbstractUserSetting extends RamObject {
    protected UUID userId;
    protected UserSettingType type;
}
