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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.ram.config.EmailSubjectMacrosTestConfig;
import org.qubership.atp.ram.service.emailsubjectmacros.EmailSubjectMacrosEnum;
import org.qubership.atp.ram.service.emailsubjectmacros.EmailSubjectMacrosFactory;
import org.qubership.atp.ram.service.emailsubjectmacros.ResolvableEmailSubjectMacros;
import org.qubership.atp.ram.services.CatalogueService;
import org.qubership.atp.ram.services.EnvironmentsService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.JointExecutionRequestService;
import org.qubership.atp.ram.services.ReportService;
import org.qubership.atp.ram.services.TestRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EmailSubjectMacrosTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.cloud.consul.config.enabled=false"})
@MockBeans({
        @MockBean(CatalogueService.class),
        @MockBean(TestRunService.class),
        @MockBean(ExecutionRequestService.class)
})
public class EmailSubjectMacrosFactoryIT {

    @Autowired
    private EmailSubjectMacrosFactory factory;

    @MockBean
    private ReportService reportService;

    @MockBean
    private JointExecutionRequestService jointExecutionRequestService;

    @MockBean
    private EnvironmentsService environmentsService;

    @Test
    public void get_testSuccessfulRetrieving_specifiedMacrosShouldBeFound() {
        // given
        final EmailSubjectMacrosEnum projectNameMacrosEnum = EmailSubjectMacrosEnum.PROJECT_NAME;
        final String projectNameMacros = projectNameMacrosEnum.getName();

        // when
        ResolvableEmailSubjectMacros macros = factory.getMacros(projectNameMacros);

        // then
        assertThat(macros.getClass())
                .isNotNull()
                .isEqualTo(projectNameMacrosEnum.getClazz());
    }

    @Test
    public void get_testUnexistedMacrosRetrieving_exceptionExpected() {
        // given
        final String unexistedMacrosName = "Lorem Ipsum";

        // when
        Assertions.assertThrows(AtpEntityNotFoundException.class, () -> {
            factory.getMacros(unexistedMacrosName);
        });
    }
}
