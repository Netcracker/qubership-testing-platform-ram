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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpIllegalNullableArgumentException;
import org.qubership.atp.ram.service.emailsubjectmacros.impl.CurrentUserEmailSubjectMacros;
import org.qubership.atp.ram.service.emailsubjectmacros.impl.EnvironmentNameEmailSubjectMacros;
import org.qubership.atp.ram.service.emailsubjectmacros.impl.ExecutionRequestNameEmailSubjectMacros;
import org.qubership.atp.ram.service.emailsubjectmacros.impl.ExecutionStatusEmailSubjectMacros;
import org.qubership.atp.ram.service.emailsubjectmacros.impl.FailRateEmailSubjectMacros;
import org.qubership.atp.ram.service.emailsubjectmacros.impl.GroupLabelsEmailSubjectMacros;
import org.qubership.atp.ram.service.emailsubjectmacros.impl.PassedPlusWarningRateEmailSubjectMacros;
import org.qubership.atp.ram.service.emailsubjectmacros.impl.PassedRateEmailSubjectMacros;
import org.qubership.atp.ram.service.emailsubjectmacros.impl.ProjectNameEmailSubjectMacros;
import org.qubership.atp.ram.service.emailsubjectmacros.impl.TestPlanEmailSubjectMacros;
import org.qubership.atp.ram.service.emailsubjectmacros.impl.WarningRateEmailSubjectMacros;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Enumeration of email subject macros.
 * Be careful when adding a new macros implementation. It should implement {@link ResolvableEmailSubjectMacros} and
 * be annotated as spring context bean.
 * Note, {@link EmailSubjectMacrosEnum#name} should be defined in uppercase
 */
@Slf4j
@AllArgsConstructor
@Getter
public enum EmailSubjectMacrosEnum {
    PROJECT_NAME(
            "${PROJECT_NAME}",
            "project name",
            ProjectNameEmailSubjectMacros.class
    ),
    EXECUTION_REQUEST_STATUS(
            "${EXECUTION_REQUEST_STATUS}",
            "execution request status",
            ExecutionStatusEmailSubjectMacros.class

    ),
    EXECUTION_REQUEST_NAME(
            "${EXECUTION_REQUEST_NAME}",
            "execution request name (with identification prefix)",
            ExecutionRequestNameEmailSubjectMacros.class
    ),
    GROUP_LABELS(
            "${GROUP_LABELS}",
            "list of labels from test run",
            GroupLabelsEmailSubjectMacros.class
    ),
    FAIL_RATE(
            "${FAIL_RATE}",
            "execution request fail rate",
            FailRateEmailSubjectMacros.class
    ),
    PASS_RATE(
            "${PASS_RATE}",
            "execution request pass rate",
            PassedRateEmailSubjectMacros.class
    ),
    WARNING_RATE(
            "${WARNING_RATE}",
            "execution request warning rate",
            WarningRateEmailSubjectMacros.class
    ),
    PASSED_PLUS_WARNING_RATE(
            "${PASSED_PLUS_WARNING_RATE}",
            "sum of pass and warning rate of execution request",
            PassedPlusWarningRateEmailSubjectMacros.class
    ),
    TEST_PLAN_NAME(
            "${TEST_PLAN_NAME}",
            "test plan name",
            TestPlanEmailSubjectMacros.class
    ),
    ENVIRONMENT_NAME(
            "${ENVIRONMENT_NAME}",
            "environment name",
            EnvironmentNameEmailSubjectMacros.class
    ),
    CURRENT_USER(
           "${CURRENT_USER}",
            "current user info",
            CurrentUserEmailSubjectMacros.class
    );

    private static Map<String, EmailSubjectMacrosEnum> macrosMap = Arrays.stream(values())
            .collect(Collectors.toMap(EmailSubjectMacrosEnum::getName, Function.identity()));

    private String name;
    private String description;
    private Class<? extends ResolvableEmailSubjectMacros> clazz;

    public static List<EmailSubjectMacrosEnum> getAll() {
        return new ArrayList<>(macrosMap.values());
    }

    /**
     * Get email subject macros by specified name.
     *
     * @param name subject macros name
     * @return macros
     */
    public static EmailSubjectMacrosEnum getByName(String name) {
        log.debug("Trying to get email subject macros enum by name: {}", name);

        if (isNull(name)) {
            log.error("Found illegal nullable email subject macros name for the validated method parameter");
            throw new AtpIllegalNullableArgumentException("email subject macros name", "method parameter");
        }

        EmailSubjectMacrosEnum macros = macrosMap.get(name.toUpperCase());

        if (isNull(macros)) {
            log.error("Failed to find Email subject macros by name: {}", name);
            throw new AtpEntityNotFoundException("Email subject macros", "name", name);
        }

        log.debug("Found macros enum: {}", macros);

        return macros;
    }
}
