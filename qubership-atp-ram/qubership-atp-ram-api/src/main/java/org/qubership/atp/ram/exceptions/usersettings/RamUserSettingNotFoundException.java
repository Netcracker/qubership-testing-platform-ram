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

package org.qubership.atp.ram.exceptions.usersettings;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpException;
import org.qubership.atp.ram.models.usersettings.UserSettingType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "RAM-9000")
public class RamUserSettingNotFoundException extends AtpException {

    public static final String DEFAULT_MESSAGE = "Failed to find user setting entity by type '%s'";

    public RamUserSettingNotFoundException(UserSettingType type) {
        super(String.format(DEFAULT_MESSAGE, type));
    }
}