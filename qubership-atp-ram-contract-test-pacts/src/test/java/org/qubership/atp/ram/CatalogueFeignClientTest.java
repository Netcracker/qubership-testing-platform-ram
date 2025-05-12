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

package org.qubership.atp.ram;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.ram.client.CatalogueIntegrationFeignClient;
import org.qubership.atp.ram.client.CatalogueIssueFeignClient;
import org.qubership.atp.ram.client.CatalogueLabelFeignClient;
import org.qubership.atp.ram.client.CatalogueLabelTemplateFeignClient;
import org.qubership.atp.ram.client.CatalogueProjectFeignClient;
import org.qubership.atp.ram.client.CatalogueTestCaseFeignClient;
import org.qubership.atp.ram.client.CatalogueTestPlanFeignClient;
import org.qubership.atp.ram.client.CatalogueTestScenarioFeignClient;
import org.qubership.atp.ram.client.CatalogueTestScopeFeignClient;
import org.qubership.atp.ram.clients.api.dto.catalogue.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.gson.Gson;

@RunWith(SpringRunner.class)

@EnableFeignClients(clients = {CatalogueIntegrationFeignClient.class,
        CatalogueTestCaseFeignClient.class,
        CatalogueTestScenarioFeignClient.class,
        CatalogueProjectFeignClient.class,
        CatalogueLabelTemplateFeignClient.class,
        CatalogueIssueFeignClient.class,
        CatalogueTestPlanFeignClient.class,
        CatalogueTestScopeFeignClient.class,
        CatalogueLabelFeignClient.class})
@ContextConfiguration(classes = {CatalogueFeignClientTest.TestApp.class})
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, FeignConfiguration.class,
        FeignAutoConfiguration.class})
@TestPropertySource(
        properties = {"feign.atp.catalogue.name=atp-catalogue", "feign.atp.catalogue.route=",
                "feign.atp.catalogue.url=http://localhost:8888", "feign.httpclient.enabled=false"})
public class CatalogueFeignClientTest {
    @Rule
    public PactProviderRule mockProvider = new PactProviderRule("atp-catalogue", "localhost", 8888, this);
    @Autowired
    CatalogueIntegrationFeignClient catalogueIntegrationFeignClient;
    @Autowired
    CatalogueTestCaseFeignClient catalogueTestCaseFeignClient;
    @Autowired
    CatalogueTestScenarioFeignClient catalogueTestScenarioFeignClient;
    @Autowired
    CatalogueProjectFeignClient catalogueProjectFeignClient;
    @Autowired
    CatalogueLabelTemplateFeignClient catalogueLabelTemplateFeignClient;
    @Autowired
    CatalogueIssueFeignClient catalogueIssueFeignClient;
    @Autowired
    CatalogueTestPlanFeignClient catalogueTestPlanFeignClient;
    @Autowired
    CatalogueTestScopeFeignClient catalogueTestScopeFeignClient;
    @Autowired
    CatalogueLabelFeignClient catalogueLabelFeignClient;

    String DATE_TIME_1 = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    @Test
    @PactVerification()
    public void allPass() {
        UUID id = UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304");
        Set<UUID> uuids = Collections.singleton(id);

        TestCaseLastStatusDto testCaseLastStatusDto = new TestCaseLastStatusDto();
        testCaseLastStatusDto.setStatus("status");
        testCaseLastStatusDto.setTestCaseId(id.toString());

        Map<String, String> hashSums = new HashMap<>();
        hashSums.put(id.toString(), "sum");

        CheckSumResponseDto checkSumResponseDto = new CheckSumResponseDto();
        checkSumResponseDto.setIsValid(true);

        FieldsPriorityDto priorityDto = new FieldsPriorityDto();
        priorityDto.setName("name");

        FieldsIssueTypeDto issueTypeDto = new FieldsIssueTypeDto();
        issueTypeDto.setName("name");

        JiraComponentDto jiraComponentDto = new JiraComponentDto();
        jiraComponentDto.setId("id");
        jiraComponentDto.setName("name");

        FieldsFoundInDto foundInDto = new FieldsFoundInDto();
        foundInDto.setValue("value");

        FieldsStatusDto statusDto = new FieldsStatusDto();
        statusDto.setName("name");

        FieldsParentDto parentDto = new FieldsParentDto();
        parentDto.setKey("key");

        FieldsProjectDto projectInFieldsDto = new FieldsProjectDto();
        projectInFieldsDto.setKey("key");

        FieldsDto fieldsDto = new FieldsDto();
        fieldsDto.setDescription("description");
        fieldsDto.setSummary("summary");
        fieldsDto.setProject(projectInFieldsDto);
        fieldsDto.setPriority(priorityDto);
        fieldsDto.setLabels(asList("label"));
        fieldsDto.setIssuetype(issueTypeDto);
        fieldsDto.setComponents(asList(jiraComponentDto));
        fieldsDto.setCustomfield17400("atpLinkTms");
        fieldsDto.setCustomfield27320("atpLinkPsup");
        fieldsDto.setCustomfield10014(foundInDto);
        fieldsDto.setStatus(statusDto);
        fieldsDto.setParent(parentDto);
        fieldsDto.setEnvironment("environment");

        JiraIssueCreateRequestDto jiraIssueCreateRequestDto = new JiraIssueCreateRequestDto();
        jiraIssueCreateRequestDto.setFields(fieldsDto);

        CaseSearchRequestDto caseSearchRequestDto = new CaseSearchRequestDto();
        caseSearchRequestDto.setGroupId(id);
        caseSearchRequestDto.setLabelId(id);
        caseSearchRequestDto.setProjectId(id);
        caseSearchRequestDto.setUuids(uuids);

        LabelDto labelDto = new LabelDto();
        labelDto.setDescription("description");
        labelDto.setProjectId(id);
        labelDto.setUuid(id);
        labelDto.setName("name");
        labelDto.setTestPlanId(id);

        List<TestRunToJiraInfoDto> testRunToJiraInfos = new ArrayList<TestRunToJiraInfoDto>(){{
            add(generateTestRunToJiraInfoDto());
        }};

        UUID projectId = UUID.fromString("5a3e9627-630c-48cd-a58f-af157f0771c7");

        ResponseEntity<Void> result1 = catalogueIntegrationFeignClient.propagateTestCasesToJira(uuids);
        Assert.assertEquals(result1.getStatusCode().value(), 204);

        ResponseEntity<Void> result2 = catalogueIntegrationFeignClient.autoSyncTestRunsWithJira(projectId, id,
                true, true, testRunToJiraInfos);
        Assert.assertEquals(result2.getStatusCode().value(), 204);

        ResponseEntity<Void> result3 = catalogueTestCaseFeignClient.updateCaseStatuses(asList(testCaseLastStatusDto));
        Assert.assertEquals(result3.getStatusCode().value(), 200);

        ResponseEntity<UUID> result4 = catalogueTestCaseFeignClient.getScenarioIdByTestCaseId(id);
        Assert.assertEquals(result4.getStatusCode().value(), 200);
        Assert.assertTrue(result4.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<CheckSumResponseDto> result5 = catalogueTestScenarioFeignClient.checkHashSum(hashSums);
        Assert.assertEquals(result5.getStatusCode().value(), 200);
        Assert.assertTrue(result5.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<ProjectDto> result6 = catalogueProjectFeignClient.getProjectById(id);
        Assert.assertEquals(result6.getStatusCode().value(), 200);
        Assert.assertTrue(result6.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<List<TestCaseLabelResponseDto>> result7 = catalogueTestCaseFeignClient.getCaseLabels(uuids);
        Assert.assertEquals(result7.getStatusCode().value(), 200);
        Assert.assertTrue(result7.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<LabelTemplateDto> result8 = catalogueLabelTemplateFeignClient.get(id);
        Assert.assertEquals(result8.getStatusCode().value(), 200);
        Assert.assertTrue(result8.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<Void> result9 = catalogueLabelTemplateFeignClient.delete(id);
        Assert.assertEquals(result9.getStatusCode().value(), 200);

        ResponseEntity<List<IssueDto>> result10 = catalogueIssueFeignClient.getByIds(new ArrayList<>(uuids));
        Assert.assertEquals(result10.getStatusCode().value(), 200);
        Assert.assertTrue(result10.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<TestPlanDtoDto> result11 = catalogueTestPlanFeignClient.getTestPlanByUuid(id);
        Assert.assertEquals(result11.getStatusCode().value(), 200);
        Assert.assertTrue(result11.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<TestScopeDto> result12 = catalogueTestScopeFeignClient.getTestScopeByUuid(id);
        Assert.assertEquals(result12.getStatusCode().value(), 200);
        Assert.assertTrue(result12.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<List<TestCaseDto>> result13 = catalogueTestCaseFeignClient.getTestCasesByIds(caseSearchRequestDto);
        Assert.assertEquals(result13.getStatusCode().value(), 200);
        Assert.assertTrue(result13.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<TestCaseDto> result14 = catalogueTestCaseFeignClient.getTestCaseWithLabelsByUuid(id);
        Assert.assertEquals(result14.getStatusCode().value(), 200);
        Assert.assertTrue(result14.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<List<LabelDto>> result15 = catalogueLabelFeignClient.getLabelsByIds(uuids);
        Assert.assertEquals(result15.getStatusCode().value(), 200);
        Assert.assertTrue(result15.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<JiraIssueCreateResponseDto> result16 = catalogueIntegrationFeignClient.createJiraTicket(id, jiraIssueCreateRequestDto);
        Assert.assertEquals(result16.getStatusCode().value(), 200);
        Assert.assertTrue(result16.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<JiraIssueDto> result17 = catalogueIntegrationFeignClient.getJiraTicketByKey(id, "key");
        Assert.assertEquals(result17.getStatusCode().value(), 200);
        Assert.assertTrue(result17.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<List<JiraComponentDto>> result18 = catalogueIntegrationFeignClient.getTestPlanJiraComponents(id);
        Assert.assertEquals(result18.getStatusCode().value(), 200);
        Assert.assertTrue(result18.getHeaders().get("Content-Type").contains("application/json"));
    }

    private TestRunToJiraInfoDto generateTestRunToJiraInfoDto() {
        return new TestRunToJiraInfoDto()
                .environmentInfo("environmentInfo")
                .executionRequestId(UUID.randomUUID())
                .jiraTicket("jiraTicket")
                .lastRun(false)
                .name("name")
                .testCaseId(UUID.randomUUID())
                .testRunAtpLink("testRunAtpLink")
                .testingStatus("testingStatus")
                .uuid(UUID.randomUUID());
    }

    @Pact(consumer = "atp-ram")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        DslPart uuids = PactDslJsonArray
                .arrayEachLike(PactDslJsonRootValue.uuid("e2490de5-5bd3-43d5-b7c4-526e33f71304"));

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("e2490de5-5bd3-43d5-b7c4-526e33f71304", "sum");

        DslPart id = PactDslJsonRootValue.uuid("e2490de5-5bd3-43d5-b7c4-526e33f71304");

        DslPart testCaseLastStatusDto = new PactDslJsonBody()
                .stringType("status", "status")
                .stringType("testCaseId", "testCaseId")
                ;

        DslPart testCaseLastStatusDtoList = new PactDslJsonArray().template(testCaseLastStatusDto);

        DslPart checkSumResponseDto = new PactDslJsonBody()
                .booleanType("valid", true)
                ;

        DslPart roles = PactDslJsonArray
                .arrayEachLike(PactDslJsonRootValue.stringType("role"));

        DslPart userInfoDto = new PactDslJsonBody()
                .stringType("email","email")
                .stringType("firstName", "firstName")
                .uuid("id", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .stringType("username", "username")
                .stringType("lastName", "lastName")
                .object("roles", roles);

        DslPart objectOperationDto = new PactDslJsonBody()
                .stringType("name", "name")
                .stringType("operationType", ObjectOperationDto.OperationTypeEnum.ADD.toString())
                ;

        DslPart datasets = PactDslJsonArray
                .arrayEachLike(PactDslJsonRootValue.stringType("dataset"));

        DslPart projectDto = new PactDslJsonBody()
                .object("atpRunners", uuids)
                .eachLike("childrenOperations", objectOperationDto)
                .object("createdBy", userInfoDto)
                .datetime("createdWhen", DATE_TIME_1)
                .object("dataSets", datasets)
                .stringType("datasetFormat", ProjectDto.DatasetFormatEnum.DEFAULT.toString())
                .stringType("description", "description")
                .object("devOpsEngineers", uuids)
                .booleanType("disableAutoSyncAtpTestCasesWithJiraTickets", true)
                .booleanType("disableWarnMsgSizeExceed", true)
                .booleanType("disableWarnOutOfSyncTime", true)
                .object("leads", uuids)
                .stringType("missionControlToolUrl", "missionControlToolUrl")
                .object("modifiedBy", userInfoDto)
                .datetime("modifiedWhen", DATE_TIME_1)
                .stringType("monitoringToolUrl", "monitoringToolUrl")
                .stringType("name", "name")
                .stringType("notificationMessageSubjectTemplate", "notificationMessageSubjectTemplate")
                .integerType("numberOfThreshold", 1)
                .stringType("projectLabel", "projectLabel")
                .stringType("projectType", ProjectDto.ProjectTypeEnum.OTHER.toString())
                .object("qaTaEngineers", uuids)
                .object("taTools", uuids)
                .stringType("tshooterUrl","tshooterUrl")
                .uuid("uuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                ;

        DslPart labels = new PactDslJsonBody()
                .stringType("description", "description")
                .uuid("projectId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("uuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .stringType("name", "name")
                .uuid("testPlanId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"));

        DslPart components = new PactDslJsonBody()
                .stringType("id", "id")
                .stringType("name", "name")
                ;

        DslPart testCaseLabelResponseDto = new PactDslJsonBody()
                .stringType("description", "description")
                .stringType("jiraTicket", "jiraTicket")
                .eachLike("labels", labels)
                .eachLike("components", components)
                .stringType("name", "name")
                .uuid("projectId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("scenarioId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("testPlanId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("uuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"));

        DslPart testCaseLabelResponseDtoList = new PactDslJsonArray().template(testCaseLabelResponseDto);

        DslPart labelTemplateNodeDto = new PactDslJsonBody()
                .uuid("labelId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .stringType("labelName", "labelName")
                .object("children", null)

                ;

        DslPart labelTemplateDto = new PactDslJsonBody()
                .eachLike("labelNodes", labelTemplateNodeDto)
                .stringType("description", "description")
                .uuid("projectId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("uuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .stringType("name", "name")
                .uuid("sourceId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                ;

        DslPart issueDto = new PactDslJsonBody()
                .stringType("businessMessage", "businessMessage")
                .stringType("component", "component")
                .stringType("description", "description")
                .stringType("errorMessage", "errorMessage")
                .stringType("failPattern", "failPattern")
                .stringType("name", "name")
                .stringType("priority", IssueDto.PriorityEnum.NORMAL.toString())
                .uuid("projectId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("uuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"));

        DslPart issueDtoList = new PactDslJsonArray().template(issueDto);

        DslPart flags = PactDslJsonArray
                .arrayEachLike(PactDslJsonRootValue.stringType(TestScopeDto.FlagsEnum.COLLECT_LOGS.toString()));

        DslPart solutionBuild = PactDslJsonArray
                .arrayEachLike(PactDslJsonRootValue.stringType("solutionBuild"));

        DslPart systemUnderTestHost = PactDslJsonArray
                .arrayEachLike(PactDslJsonRootValue.stringType("systemUnderTestHost"));

        DslPart jiraComponentDto = new PactDslJsonBody()
                .stringType("id", "id")
                .stringType("name", "name")
                ;

        DslPart listJiraComponentDto = new PactDslJsonArray().template(jiraComponentDto);

        DslPart synchronization = new PactDslJsonBody()
                .stringType("authorizationKey", "authorizationKey")
                .object("components", listJiraComponentDto)
                .stringType("defaultTestCaseMapping",
                        BugTrackingSystemSynchronizationDtoDto.DefaultTestCaseMappingEnum.E2E.toString())
                .stringType("projectKey", "projectKey")
                .stringType("projectName", "projectName")
                .stringType("synchronizationType",
                        BugTrackingSystemSynchronizationDtoDto.SynchronizationTypeEnum.AUTOMATIC.toString())
                .stringType("systemType", BugTrackingSystemSynchronizationDtoDto.SystemTypeEnum.JIRA.toString())
                .stringType("systemUrl", "systemUrl")
                ;

        DslPart testPlanDtoDto = new PactDslJsonBody()
                .integerType("createdDate", 1)
                .integerType("lastModifyDate", 1)
                .object("lastModifiedBy", userInfoDto)
                .object("createdBy", userInfoDto)
                .stringType("description", "description")
                .booleanType("disableAutoSyncAtpTestCasesWithJiraTickets", true)
                .integerType("executionRequestClosingTimeoutMin", 1)
                .booleanType("jiraTicketsAutoClosing", true)
                .stringType("name", "name")
                .stringType("notificationMessageSubjectTemplate")
                .uuid("projectUuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("qaDslLibraryId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .object("synchronization", synchronization)
                .object("taTools", uuids)
                .uuid("uuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                ;

        DslPart testScopeDto1 = new PactDslJsonBody()
                .object("modifiedBy", userInfoDto)
                .datetime("modifiedWhen", DATE_TIME_1)
                .object("createdBy", userInfoDto)
                .datetime("createdWhen", DATE_TIME_1)
                .stringType("description", "description")
                .object("flags", flags)
                .uuid("environmentUuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .object("executionCases", uuids)
                .object("prerequisitesCases", uuids)
                .object("validationCases", uuids)
                .stringType("name", "name")
                .uuid("groupUuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("projectUuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("taToolsUuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("testPlanUuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("uuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("widgetConfigTemplateId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .integerType("numberOfThreshold", 1)
                .object("solutionBuild", solutionBuild)
                .object("systemUnderTestHost", systemUnderTestHost)
                ;

        DslPart testCaseDependencyDto = new PactDslJsonBody()
                .uuid("testCaseId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("testScopeId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                ;

        DslPart testCaseDependencyDtoList = new PactDslJsonArray().template(testCaseDependencyDto);

        DslPart testCaseOrderDto = new PactDslJsonBody()
                .uuid("testScopeId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .integerType("sequenceNumber", 1)
                ;

        DslPart testCaseOrderDtoList = new PactDslJsonArray().template(testCaseOrderDto);

        DslPart testCaseRepeatCountDto = new PactDslJsonBody()
                .uuid("testScopeId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .integerType("repeatCount", 1)
                ;

        DslPart testCaseRepeatCountDtoList = new PactDslJsonArray().template(testCaseRepeatCountDto);

        DslPart testCaseFlagsDto = new PactDslJsonBody()
                .uuid("testScopeId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .object("flags", flags)
                ;

        DslPart testCaseFlagsDtoList = new PactDslJsonArray().template(testCaseFlagsDto);

        DslPart userInfoResDto = new PactDslJsonBody()
                .stringType("email","email")
                .stringType("firstName", "firstName")
                .stringType("fullName", "fullName")
                .uuid("id", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .stringType("username", "userName")
                .stringType("lastName", "lastName")
                .object("roles", roles);

        DslPart testCaseDto1 = new PactDslJsonBody()
                .object("assignee", userInfoResDto)
                .object("components", listJiraComponentDto)
                .object("createdBy", userInfoResDto)
                .datetime("createdWhen", DATE_TIME_1)
                .uuid("datasetStorageUuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("datasetUuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .object("dependsOn", testCaseDependencyDtoList)
                .stringType("description", "description")
                .object("flags", testCaseFlagsDtoList)
                .uuid("groupId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .stringType("jiraTicket", "jiraTicket")
                .object("labelIds", uuids)
                .uuid("lastRun", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .stringType("lastRunStatus", "lastRunStatus")
                .object("modifiedBy", userInfoResDto)
                .datetime("modifiedWhen", DATE_TIME_1)
                .stringType("name", "name")
                .object("order", testCaseOrderDtoList)
                .stringType("priority", TestCaseDto.PriorityEnum.NORMAL.toString())
                .uuid("projectUuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .stringType("status", TestCaseDto.StatusEnum.PASSED.toString())
                .object("testCaseRepeatCounts", testCaseRepeatCountDtoList)
                .uuid("testPlanUuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("testScenarioUuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("uuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                ;

        DslPart listTestCaseDto = new PactDslJsonArray().template(testCaseDto1);

        DslPart testCaseDto2 = new PactDslJsonBody()
                .object("assignee", userInfoResDto)
                .object("components", listJiraComponentDto)
                .object("createdBy", userInfoResDto)
                .datetime("createdWhen", DATE_TIME_1)
                .uuid("datasetStorageUuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("datasetUuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .object("dependsOn", testCaseDependencyDtoList)
                .stringType("description", "description")
                .object("flags", testCaseFlagsDtoList)
                .uuid("groupId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .stringType("jiraTicket", "jiraTicket")
                .object("labelIds", uuids)
                .uuid("lastRun", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .stringType("lastRunStatus")
                .object("modifiedBy", userInfoResDto)
                .datetime("modifiedWhen", DATE_TIME_1)
                .stringType("name", "name")
                .object("order", testCaseOrderDtoList)
                .stringType("priority", TestCaseDto.PriorityEnum.NORMAL.toString())
                .uuid("projectUuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .stringType("status", TestCaseDto.StatusEnum.PASSED.toString())
                .object("testCaseRepeatCounts", testCaseRepeatCountDtoList)
                .uuid("testPlanUuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("testScenarioUuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("uuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                ;

        DslPart caseSearchRequestDto = new PactDslJsonBody()
                .uuid("groupId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("labelId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("projectId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .object("uuids", uuids)
                ;

        DslPart labelDto = new PactDslJsonBody()
                .stringType("description", "description")
                .stringType("name", "name")
                .uuid("testPlanId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("uuid", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                .uuid("projectId", UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
                ;

        DslPart listLabelDto = new PactDslJsonArray().template(labelDto);

        DslPart fieldsProjectDto = new PactDslJsonBody()
                .stringType("key", "key2")
                ;

        DslPart labelList = PactDslJsonArray
                .arrayEachLike(PactDslJsonRootValue.stringType("label2"));

        DslPart issueTypeDto = new PactDslJsonBody()
                .stringType("name", "name2")
                ;

        DslPart priorityDto = new PactDslJsonBody()
                .stringType("name", "name2")
                ;

        DslPart jiraComponentInFieldsDto = new PactDslJsonBody()
                .stringType("id", "id2")
                .stringType("name", "name2")
                ;

        DslPart listJiraComponentInFieldsDto = new PactDslJsonArray().template(jiraComponentInFieldsDto);


        DslPart foundInDto = new PactDslJsonBody()
                .stringType("value", "value2")
                ;

        DslPart statusDto = new PactDslJsonBody()
                .stringType("name", "name2")
                ;

        DslPart parentDto = new PactDslJsonBody()
                .stringType("key", "key2")
                ;

        DslPart fieldsDto = new PactDslJsonBody()
                .stringType("description", "description2")
                .stringType("summary", "summary2")
                .object("project", fieldsProjectDto)
                .object("priority", priorityDto)
                .object("labels", labelList)
                .object("issuetype", issueTypeDto)
                .object("components", listJiraComponentInFieldsDto)
                .stringType("customfield_17400", "atpLinkTms2")
                .stringType("customfield_27320", "atpLinkPsup2")
                .object("customfield_10014", foundInDto)
                .object("status", statusDto)
                .object("parent", parentDto)
                .stringType("environment", "environment2")
                ;

        DslPart jiraIssueCreateRequestDto = new PactDslJsonBody()
                .object("fields", fieldsDto)
                ;

        DslPart jiraIssueCreateResponseDto = new PactDslJsonBody()
                .stringType("id", "id")
                .stringType("key", "key")
                .stringType("self", "self")
                .stringType("errorMessage", "errorMessage")
                ;

        DslPart jiraIssueDto = new PactDslJsonBody()
                .stringType("id", "id")
                .stringType("key", "key")
                .stringType("self", "self")
                .object("fields", fieldsDto)
                ;

        DslPart testRunToJiraInfoDto = new PactDslJsonBody()
                .stringType("environmentInfo", "environmentInfo")
                .uuid("executionRequestId")
                .stringType("jiraTicket")
                .booleanType("lastRun")
                .stringType("name")
                .uuid("testCaseId")
                .stringType("testRunAtpLink")
                .stringType("testingStatus")
                .uuid("uuid");

        PactDslResponse response = builder

                .given("all ok")
                .uponReceiving("POST /catalog/api/v1/integrations/propagate/testcases/catalog/api/v1/integrations/propagate/testcases OK")
                .path("/catalog/api/v1/integrations/propagate/testcases")
                .method("POST")
                .headers(headers)
                .body(uuids)
                .willRespondWith()
                .status(204)

                .given("all ok")
                .uponReceiving("POST /catalog/api/v1/integrations/project/{projectId}/executionrequest/{uuid}/autosync OK")
                .method("POST")
                .path("/catalog/api/v1/integrations/project/5a3e9627-630c-48cd-a58f-af157f0771c7/executionrequest/e2490de5-5bd3-43d5-b7c4-526e33f71304/autosync")
                .query("syncTestCases=true&syncTestRuns=true")
                .headers(headers)
                .body(new PactDslJsonArray().template(testRunToJiraInfoDto))
                .willRespondWith()
                .status(204)

                .given("all ok")
                .uponReceiving("POST /catalog/api/v1/testcases/status-update OK")
                .path("/catalog/api/v1/testcases/status-update")
                .method("POST")
                .headers(headers)
                .body(testCaseLastStatusDtoList)
                .willRespondWith()
                .status(200)

                .given("all ok")
                .uponReceiving("GET /catalog/api/v1/testcases/{uuid}/testscenario-id OK")
                .path("/catalog/api/v1/testcases/e2490de5-5bd3-43d5-b7c4-526e33f71304/testscenario-id")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(id)

                .given("all ok")
                .uponReceiving("POST /catalog/api/v1/testscenarios/checkHashSum OK")
                .path("/catalog/api/v1/testscenarios/checkHashSum")
                .method("POST")
                .headers(headers)
                .body(new Gson().toJson(requestBody))
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(checkSumResponseDto)

                .given("all ok")
                .uponReceiving("GET /catalog/api/v1/projects/{uuid} OK")
                .path("/catalog/api/v1/projects/e2490de5-5bd3-43d5-b7c4-526e33f71304")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(projectDto)

                .given("all ok")
                .uponReceiving("POST /catalog/api/v1/testcases/labels/search OK")
                .path("/catalog/api/v1/testcases/labels/search")
                .method("POST")
                .headers(headers)
                .body(uuids)
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(testCaseLabelResponseDtoList)

                .given("all ok")
                .uponReceiving("GET /catalog/api/v1/label-templates/{id} OK")
                .path("/catalog/api/v1/label-templates/e2490de5-5bd3-43d5-b7c4-526e33f71304")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(labelTemplateDto)

                .given("all ok")
                .uponReceiving("DELETE /catalog/api/v1/label-templates/{id} OK")
                .path("/catalog/api/v1/label-templates/e2490de5-5bd3-43d5-b7c4-526e33f71304")
                .method("DELETE")
                .willRespondWith()
                .status(200)

                .given("all ok")
                .uponReceiving("GET /catalog/api/v1/issues/by_ids OK")
                .path("/catalog/api/v1/issues/by_ids")
                .method("GET")
                .query("issueIds=e2490de5-5bd3-43d5-b7c4-526e33f71304")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(issueDtoList)

                .given("all ok")
                .uponReceiving("GET /catalog/api/v1/testplans/{uuid} OK")
                .path("/catalog/api/v1/testplans/e2490de5-5bd3-43d5-b7c4-526e33f71304")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(testPlanDtoDto)

                .given("all ok")
                .uponReceiving("GET /catalog/api/v1/testscopes/{uuid} OK")
                .path("/catalog/api/v1/testscopes/e2490de5-5bd3-43d5-b7c4-526e33f71304")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(testScopeDto1)

                .given("all ok")
                .uponReceiving("POST /catalog/api/v1/testcases/search/by_ids OK")
                .path("/catalog/api/v1/testcases/search/by_ids")
                .method("POST")
                .headers(headers)
                .body(caseSearchRequestDto)
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(listTestCaseDto)

                .given("all ok")
                .uponReceiving("GET /catalog/api/v1/testcases/{uuid} OK")
                .path("/catalog/api/v1/testcases/e2490de5-5bd3-43d5-b7c4-526e33f71304")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(testCaseDto2)

                .given("all ok")
                .uponReceiving("GET /catalog/api/v1/labels/search OK")
                .path("/catalog/api/v1/labels/search")
                .method("GET")
                .query("labelIds=e2490de5-5bd3-43d5-b7c4-526e33f71304")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(listLabelDto)

                .given("all ok")
                .uponReceiving("POST /catalog/api/v1/integrations/jira/ticket/create OK")
                .path("/catalog/api/v1/integrations/jira/ticket/create")
                .method("POST")
                .headers(headers)
                .query("testPlanId=e2490de5-5bd3-43d5-b7c4-526e33f71304")
                .body(jiraIssueCreateRequestDto)
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(jiraIssueCreateResponseDto)

                .given("all ok")
                .uponReceiving("GET /catalog/api/v1/integrations/jira/ticket OK")
                .path("/catalog/api/v1/integrations/jira/ticket")
                .method("GET")
                .query("testPlanId=e2490de5-5bd3-43d5-b7c4-526e33f71304&key=key")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(jiraIssueDto)

                .given("all ok")
                .uponReceiving("GET /catalog/api/v1/integrations/testplan/{uuid}/components OK")
                .path("/catalog/api/v1/integrations/testplan/e2490de5-5bd3-43d5-b7c4-526e33f71304/components")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(listJiraComponentDto)
                ;

        return response.toPact();
    }


    @Configuration
    public static class TestApp {

    }
}
