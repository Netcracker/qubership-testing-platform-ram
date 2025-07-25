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

import lombok.Getter;

@Getter
public enum UserSettingType {
    ER_TABLE_COLUMNS_VISIBILITY(
            Constants.ER_TABLE_COLUMNS_VISIBILITY,
            TableColumnVisibilityUserSetting.class
    ),
    TC_EXECUTION_HISTORY_TABLE_COLUMNS_VISIBILITY(
            Constants.TC_EXECUTION_HISTORY_TABLE_COLUMNS_VISIBILITY,
            TableColumnVisibilityUserSetting.class
    );

    private Class<? extends AbstractUserSetting> settingClazz;
    private String name;

    UserSettingType(String name, Class<? extends AbstractUserSetting> settingClazz) {
        this.name = name;
        this.settingClazz = settingClazz;
    }

    public interface Constants {
        String ER_TABLE_COLUMNS_VISIBILITY = "ER_TABLE_COLUMNS_VISIBILITY";
        String TC_EXECUTION_HISTORY_TABLE_COLUMNS_VISIBILITY = "TC_EXECUTION_HISTORY_TABLE_COLUMNS_VISIBILITY";
    }
}
