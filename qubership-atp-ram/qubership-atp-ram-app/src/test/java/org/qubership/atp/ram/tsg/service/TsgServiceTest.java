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

package org.qubership.atp.ram.tsg.service;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.LogRecordService;
import org.qubership.atp.ram.services.ProjectsService;
import org.qubership.atp.ram.services.TestRunService;
import org.qubership.atp.ram.tsg.model.TsgConfiguration;
import org.qubership.atp.ram.tsg.senders.Sender;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class TsgServiceTest {

    private ExecutionRequestService executionRequestService = mock(ExecutionRequestService.class);
    private TestRunService testRunService = mock(TestRunService.class);
    private LogRecordService logRecordService = mock(LogRecordService.class);
    private ProjectsService projectsService = mock(ProjectsService.class);
    private TsgProjectService tsgProjectService = mock(TsgProjectService.class);
    private TsgErService tsgErService = mock(TsgErService.class);
    private Sender<List<UUID>> sender = mock(Sender.class);
    private TsgConfiguration tsgConfiguration = new TsgConfiguration();

    private Project project;
    private ExecutionRequest er;
    private TestRun tr1;
    private TestRun tr2;
    private TestRun tr3;

    @BeforeEach
    public void setUp() throws Exception {
        project = new Project();
        project.setTsgIntegration(true);
        project.setTsgProjectName("testTsg");
        project.setName("testRam");
        er = new ExecutionRequest();
        er.setName("ER1");
        er.setUuid(UUID.randomUUID());
        er.setExecutionStatus(ExecutionStatuses.FINISHED);
        tr1 = new TestRun();
        tr1.setName("TR1");
        tr1.updateTestingStatus(TestingStatuses.FAILED);
        tr1.setExecutionStatus(ExecutionStatuses.FINISHED);
        tr1.setFdrWasSent(true);
        tr2 = new TestRun();
        tr2.setName("TR2");
        tr2.updateTestingStatus(TestingStatuses.FAILED);
        tr2.setExecutionStatus(ExecutionStatuses.FINISHED);
        tr2.setFdrWasSent(false);
        tr3 = new TestRun();
        tr3.setName("TR3");
        tr3.updateTestingStatus(TestingStatuses.UNKNOWN);
        tr3.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
    }

    @Test
    public void getDailyInfo_ShouldReturnJsonArrayWithStatusFields() {
        when(tsgProjectService.getAllTsgProjects()).thenReturn(Collections.singletonList(project));
        when(executionRequestService.findFinishedErByProjectAndSortByFinishDate(any(), any()))
                .thenReturn(Collections.singletonList(er));
        when(testRunService.findAllByExecutionRequestId(any())).thenReturn(Arrays.asList(tr1, tr2));
        TsgService tsgService = new TsgService(executionRequestService, testRunService, logRecordService,
                projectsService, tsgProjectService, tsgErService, tsgConfiguration, sender);
        JsonArray result = tsgService.getDailyInfo(1);
        Assertions.assertEquals(1, result.size());
        JsonObject projectActaul = result.get(0).getAsJsonObject();
        Assertions.assertTrue(projectActaul.has("hasFail"));
        Assertions.assertTrue(projectActaul.get("hasFail").getAsBoolean());
        JsonObject erObject = projectActaul.getAsJsonArray("executionRequests").get(0).getAsJsonObject();
        Assertions.assertTrue(erObject.has("hasFail"));
        Assertions.assertTrue(erObject.get("hasFail").getAsBoolean());
        JsonArray testRuns = erObject.getAsJsonArray("testRuns");
        Assertions.assertEquals(2, testRuns.size());
    }

    @Test
    public void getDailyInfo_TestRunsWithStatusInProgressShouldBeNotIncludedToReport() {
        when(tsgProjectService.getAllTsgProjects()).thenReturn(Arrays.asList(project));
        when(executionRequestService.findFinishedErByProjectAndSortByFinishDate(any(), any()))
                .thenReturn(Arrays.asList(er));
        when(testRunService.findAllByExecutionRequestId(any())).thenReturn(Arrays.asList(tr1, tr2, tr3));
        TsgService tsgService = new TsgService(executionRequestService, testRunService, logRecordService,
                projectsService, tsgProjectService, tsgErService, tsgConfiguration, sender);
        JsonArray result = tsgService.getDailyInfo(1);
        JsonObject projectActaul = result.get(0).getAsJsonObject();
        JsonObject erObject = projectActaul.getAsJsonArray("executionRequests").get(0).getAsJsonObject();
        JsonArray testRuns = erObject.getAsJsonArray("testRuns");
        Assertions.assertEquals(2, testRuns.size());
        Assertions.assertTrue(projectActaul.has("projectTestRunsCount"));
        Assertions.assertEquals(2, projectActaul.get("projectTestRunsCount").getAsInt());
    }
}
