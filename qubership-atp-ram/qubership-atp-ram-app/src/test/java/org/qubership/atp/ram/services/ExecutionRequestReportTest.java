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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.qubership.atp.integration.configuration.service.MailSenderService;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.service.mail.ExecutionRequestReport;
import org.qubership.atp.ram.service.mail.MailSenderConfig;
import org.qubership.atp.ram.testdata.ExecutionRequestServiceMock;
import org.qubership.atp.ram.testdata.TestMailConstants;
import org.qubership.atp.ram.testdata.TestRunServiceMock;

// todo fix UUID mocks for ER & TR
@Disabled
public class ExecutionRequestReportTest {
    private final static UUID erUuid = UUID.randomUUID();
    private ExecutionRequestService executionRequestService = mock(ExecutionRequestService.class);
    private TestRunService testRunService = mock(TestRunService.class);
    private TestPlansService testPlansService = mock(TestPlansService.class);
    private DefectsService defectsService = mock(DefectsService.class);
    private RootCauseService rootCauseService = mock(RootCauseService.class);
    private MailSenderService mailSender = mock(MailSenderService.class);
    private TestRunServiceMock testRunServiceMock = new TestRunServiceMock();
    private MailSenderConfig mailSenderConfig = new MailSenderConfig();
    private ExecutionRequestReport report = new ExecutionRequestReport(executionRequestService, testRunService,
            testPlansService, defectsService, rootCauseService, mailSenderConfig, mailSender);
    private String expectedString = TestMailConstants.HEADER_ER_REPORT
            + TestMailConstants.SUMMARY_TABLE_TEMPLATE
            + TestMailConstants.ROOT_CAUSES_STATISTICS
            + TestMailConstants.TR_TABLE_TEMPLATE;

    @BeforeEach
    public void before() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        ExecutionRequest er = ExecutionRequestServiceMock.newExecutionRequest(erUuid);
        expectedString = expectedString.replace("${DATE}", er.getStartDate().toLocalDateTime().format(formatter));
        RootCause rootCause = new RootCause();
        rootCause.setName("Not Analyzed");

        when(executionRequestService.findById(any()))
                .thenReturn(er);
        when(rootCauseService.getById(any())).thenReturn(rootCause);
        when(testRunService.findTestRunsWithFillStatusByRequestId(any()))
                .thenReturn(testRunServiceMock.findByExecutionRequestIdWithFailedTr());
        when(testRunService.getAllSectionNotCompoundLogRecords(any()))
                .thenReturn(testRunServiceMock.getTopLevelLogRecords(TestingStatuses.PASSED))
                .thenReturn(testRunServiceMock.getTopLevelLogRecords(TestingStatuses.FAILED));
        when(testRunService.getAllFailedLogRecords(UUID.randomUUID()))
                .thenReturn(testRunServiceMock.getTopLevelLogRecords(TestingStatuses.FAILED));
    }

    @Test
    public void buildEmailBody_BuildsHtmlStringContainingReportOnGeneratedEr_ReturnedStringEqualToExpectedString() {
        report.setProperties(null, null, null, null, "http://home");
        String result = report.buildEmailBody(erUuid);
        Assertions.assertEquals(expectedString, result);
    }

    @Test
    public void buildEmailBody_PreviousErIsNull_HandledNullEr() {
        ExecutionRequest er = ExecutionRequestServiceMock.newExecutionRequest(erUuid);
        when(executionRequestService.findById(erUuid))
                .thenReturn(er);
        when(executionRequestService.findById(UUID.randomUUID()))
                .thenReturn(null);
        report.setProperties(null, null, null, null, "http://home");
        String result = report.buildEmailBody(erUuid);
        Assertions.assertEquals(expectedString, result);
    }
}
