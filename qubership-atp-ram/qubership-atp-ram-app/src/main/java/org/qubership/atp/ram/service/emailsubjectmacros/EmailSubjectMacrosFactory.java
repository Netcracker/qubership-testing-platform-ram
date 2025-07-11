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

package org.qubership.atp.ram.service.emailsubjectmacros;

import static java.util.Objects.isNull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailSubjectMacrosFactory {

    private final List<ResolvableEmailSubjectMacros> macrosList;

    private Map<Class<? extends ResolvableEmailSubjectMacros>, ResolvableEmailSubjectMacros> macrosMap;

    @PostConstruct
    private void initMacroMap() {
        this.macrosMap = macrosList.stream()
                .collect(Collectors.toMap(ResolvableEmailSubjectMacros::getClass, Function.identity()));
    }

    /**
     * Get macros bean from context by specified name.
     * Make sure that macros bean defined in {@link EmailSubjectMacrosEnum}.
     *
     * @param macrosName macros name
     * @return macros bean
     */
    public ResolvableEmailSubjectMacros getMacros(String macrosName) {
        Class<? extends ResolvableEmailSubjectMacros> macrosClazz =
                EmailSubjectMacrosEnum.getByName(macrosName).getClazz();
        log.debug("Trying to find macros by name '{}' and class '{}'", macrosName, macrosClazz);

        ResolvableEmailSubjectMacros emailSubjectMacros = macrosMap.get(macrosClazz);

        if (isNull(emailSubjectMacros)) {
            log.error("Failed to find Email subject macros implementation by class: {}", macrosClazz);
            throw new AtpEntityNotFoundException("Email subject macros implementation", "class", macrosClazz);
        }
        log.debug("Macros was found successfully");

        return emailSubjectMacros;
    }
}
