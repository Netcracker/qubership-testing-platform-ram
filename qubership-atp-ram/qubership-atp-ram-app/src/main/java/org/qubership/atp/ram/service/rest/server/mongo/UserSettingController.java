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

package org.qubership.atp.ram.service.rest.server.mongo;

import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.ram.models.usersettings.AbstractUserSetting;
import org.qubership.atp.ram.models.usersettings.UserSettingType;
import org.qubership.atp.ram.services.UserSettingService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequestMapping("/api/usersettings")
@RestController
@RequiredArgsConstructor
public class UserSettingController /*implements UserSettingControllerApi*/ {

    private final UserSettingService userSettingService;

    @GetMapping("/currentUser")
    @AuditAction(auditAction = "Get table column visibilities for current user and by type '{{#type.name}}'")
    private ResponseEntity<AbstractUserSetting> getByUserAndType(@RequestParam
                                                                 UserSettingType type,
                                                                 @RequestHeader(value = HttpHeaders.AUTHORIZATION)
                                                                 String userToken) {
        return ResponseEntity.ok(userSettingService.getByUserAndType(userToken, type));
    }

    @PostMapping
    @AuditAction(auditAction = "Create table column visibilities for current user")
    private ResponseEntity<AbstractUserSetting> create(@RequestBody AbstractUserSetting setting,
                                                       @RequestHeader(value = HttpHeaders.AUTHORIZATION)
                                                       String userToken) {
        return new ResponseEntity<>(userSettingService.create(setting, userToken), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @AuditAction(auditAction = "Update table column visibilities '{{#id}}' for current user")
    private AbstractUserSetting update(@PathVariable UUID id,
                                       @RequestBody AbstractUserSetting setting,
                                       @RequestHeader(value = HttpHeaders.AUTHORIZATION)
                                       String userToken) {
        return userSettingService.update(id, setting, userToken);
    }
}
