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

package org.qubership.atp.ram.service.emailsubjectmacroses;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpIllegalNullableArgumentException;
import org.qubership.atp.ram.service.emailsubjectmacros.EmailSubjectMacrosEnum;
import org.qubership.atp.ram.services.ReportService;
import org.springframework.boot.test.mock.mockito.MockBean;

public class EmailSubjectMacrosEnumTest {

    private EmailSubjectMacrosEnum failRateMacros;

    @MockBean
    private ReportService reportService;

    @BeforeEach
    public void setUp() {
        failRateMacros = EmailSubjectMacrosEnum.FAIL_RATE;
    }

    @Test
    public void getAll_testSuccessfulRetrieving_allElementsShouldMatchTheEnums() {
        // given
        final Set<EmailSubjectMacrosEnum> subjectMacrosEnums = new HashSet<>(asList(EmailSubjectMacrosEnum.values()));

        // when
        Set<EmailSubjectMacrosEnum> subjectMacrosElements = new HashSet<>(EmailSubjectMacrosEnum.getAll());

        // then
        assertThat(subjectMacrosElements)
                .isNotNull()
                .isNotEmpty()
                .isEqualTo(subjectMacrosEnums);
    }

    @Test
    public void get_testSuccessfulRetrieving_specifiedElementShouldBeFound() {
        // given
        final String failRateMacrosName = failRateMacros.getName();

        // when
        final EmailSubjectMacrosEnum foundMacros = EmailSubjectMacrosEnum.getByName(failRateMacrosName);

        // then
        assertThat(foundMacros)
                .isNotNull()
                .isEqualTo(failRateMacros);

        assertThat(foundMacros.getName())
                .isNotNull()
                .isEqualTo(failRateMacros.getName());

        assertThat(foundMacros.getDescription())
                .isNotNull()
                .isEqualTo(failRateMacros.getDescription());
    }

    @Test
    public void get_testSuccessfulRetrieving_specifiedElementShouldBeFoundWithLowerCaseName() {
        // given
        final String failRateMacrosName = failRateMacros.getName().toLowerCase();

        // when
        final EmailSubjectMacrosEnum foundMacros = EmailSubjectMacrosEnum.getByName(failRateMacrosName);

        // then
        assertThat(foundMacros)
                .isNotNull()
                .isEqualTo(failRateMacros);
    }

    @Test
    public void get_testSuccessfulRetrieving_specifiedElementShouldBeFoundWithUpperCaseName() {
        // given
        final String failRateMacrosName = failRateMacros.getName().toUpperCase();

        // when
        final EmailSubjectMacrosEnum foundMacros = EmailSubjectMacrosEnum.getByName(failRateMacrosName);

        // then
        assertThat(foundMacros)
                .isNotNull()
                .isEqualTo(failRateMacros);
    }

    @Test
    public void get_testUnsuccessfulRetrieving_exceptionExpected() {
        // given
        final String incorrectMacrosName = "Lorem Ipsum";

        // when
        Assertions.assertThrows(AtpEntityNotFoundException.class, () -> {
            EmailSubjectMacrosEnum.getByName(incorrectMacrosName);
        });
    }

    @Test
    public void get_testNullableNameRetrieving_exceptionExpected() {
        // given
        final String nullableMacrosName = null;

        // when
        Assertions.assertThrows(AtpIllegalNullableArgumentException.class, () -> {
            EmailSubjectMacrosEnum.getByName(nullableMacrosName);
        });
    }
}
