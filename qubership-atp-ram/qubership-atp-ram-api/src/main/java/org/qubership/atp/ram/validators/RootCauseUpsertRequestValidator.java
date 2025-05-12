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

package org.qubership.atp.ram.validators;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpIllegalNullableArgumentException;
import org.qubership.atp.ram.exceptions.rootcauses.RamRootCauseIllegalTypeException;
import org.qubership.atp.ram.exceptions.rootcauses.RamRootCauseInvalidTreeStructureException;
import org.qubership.atp.ram.model.request.RootCauseUpsertRequest;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.RootCauseType;
import org.qubership.atp.ram.repositories.RootCauseRepository;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RootCauseUpsertRequestValidator implements Validator {

    private final RootCauseRepository repository;

    @Override
    public boolean supports(Class<?> clazz) {
        return RootCauseUpsertRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        RootCauseUpsertRequest rootCause = (RootCauseUpsertRequest) target;

        log.debug("Validate root cause: {}", rootCause);
        final RootCauseType rootCauseType = rootCause.getType();

        if (isNull(rootCauseType)) {
            log.error("Found illegal nullable root cause type for the validated request");
            throw new AtpIllegalNullableArgumentException("environment id", "request");
        }

        switch (rootCauseType) {
            case CUSTOM:
                validateCustomRootCause(rootCause);
                break;

            case GLOBAL:
                validateGlobalRootCause(rootCause);
                break;

            default:
                log.error("Found illegal root cause type: {}", rootCauseType);
                throw new RamRootCauseIllegalTypeException(rootCauseType);
        }
    }

    private void validateCustomRootCause(RootCauseUpsertRequest rootCause) {
        final UUID projectId = rootCause.getProjectId();

        if (isNull(projectId)) {
            log.error("Found illegal nullable project id for the validated RootCauseUpsertRequest");
            throw new AtpIllegalNullableArgumentException("project id", "RootCauseUpsertRequest");
        }
    }

    private void validateGlobalRootCause(RootCauseUpsertRequest rootCause) {
        final UUID projectId = rootCause.getProjectId();
        final String rootCauseName = rootCause.getName();
        final UUID parentId = rootCause.getParentId();

        if (nonNull(parentId)) {
            final RootCause parentRootCause = repository.findByUuid(parentId);
            final RootCauseType parentRootCauseType = parentRootCause.getType();
            final boolean isParentCustom = RootCauseType.CUSTOM.equals(parentRootCauseType);

            if (isParentCustom) {
                log.error("Global root cause '{}' cannot be set under custom '{}'", rootCauseName, parentId);
                throw new RamRootCauseInvalidTreeStructureException();
            }
        }

        if (nonNull(projectId)) {
            log.error("Found illegal nullable project id for the validated RootCauseUpsertRequest");
            throw new AtpIllegalNullableArgumentException("project id", "RootCauseUpsertRequest");
        }
    }
}
