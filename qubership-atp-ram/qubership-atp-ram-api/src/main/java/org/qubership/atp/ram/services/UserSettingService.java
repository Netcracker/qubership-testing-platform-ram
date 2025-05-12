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

package org.qubership.atp.ram.services;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.models.usersettings.AbstractUserSetting;
import org.qubership.atp.ram.models.usersettings.UserSettingType;
import org.qubership.atp.ram.repositories.UserSettingRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSettingService extends CrudService<AbstractUserSetting> {

    private final UserSettingRepository userSettingRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;

    /**
     * Get setting by user and type.
     *
     * @param userToken current user auth token
     * @param type      setting type
     * @return found setting
     */
    public AbstractUserSetting getByUserAndType(String userToken, UserSettingType type) {
        UUID userId = userService.getUserIdFromToken(userToken);

        return userSettingRepository.findByUserIdAndType(userId, type);
    }

    /**
     * Create user setting.
     *
     * @param setting   created setting data
     * @param userToken current user auth token
     * @return created setting
     */
    public AbstractUserSetting create(AbstractUserSetting setting, String userToken) {
        UUID userId = userService.getUserIdFromToken(userToken);
        setting.setUserId(userId);

        return save(setting);
    }

    /**
     * Update user setting.
     *
     * @param id        user setting identifier
     * @param setting   updated setting data
     * @param userToken current user auth token
     * @return updated setting
     */
    public AbstractUserSetting update(UUID id, AbstractUserSetting setting, String userToken) {
        AbstractUserSetting existedSetting = get(id);
        modelMapper.map(setting, existedSetting);
        UUID userId = userService.getUserIdFromToken(userToken);
        existedSetting.setUserId(userId);

        return save(existedSetting);
    }

    @Override
    protected MongoRepository<AbstractUserSetting, UUID> repository() {
        return userSettingRepository;
    }
}
