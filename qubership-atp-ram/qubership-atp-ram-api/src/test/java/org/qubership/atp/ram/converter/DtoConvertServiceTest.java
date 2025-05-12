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

package org.qubership.atp.ram.converter;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.clients.api.dto.catalogue.JiraIssueCreateRequestDto;
import org.qubership.atp.ram.config.MvcConfig;
import org.qubership.atp.ram.model.jira.Fields;
import org.qubership.atp.ram.model.jira.JiraIssueCreateRequest;
import org.qubership.atp.ram.models.JiraComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = {JacksonAutoConfiguration.class, MvcConfig.class})
@ExtendWith(SpringExtension.class)
public class DtoConvertServiceTest {

    private DtoConvertService dtoConvertService;
    @Autowired
    private ModelMapper modelMapper;

    @BeforeEach
    public void setUp() {
        dtoConvertService = new DtoConvertService(modelMapper);
    }

    @Test
    public void test_dtoConverter() {
        JiraIssueCreateRequest request = new JiraIssueCreateRequest();
        Fields fields = new Fields();
        fields.setSummary("summary");
        fields.setDescription("description");
        fields.setProject(new Fields.Project("project"));
        fields.setPriority(new Fields.Priority("priority"));
        fields.setLabels(Arrays.asList("label1", "label2"));
        fields.setIssuetype(new Fields.IssueType("issueType"));
        JiraComponent comp1 = new JiraComponent();
        comp1.setId("1");
        comp1.setName("comp1");
        JiraComponent comp2 = new JiraComponent();
        comp2.setId("2");
        comp2.setName("comp2");
        fields.setComponents(Arrays.asList(comp1, comp2));
        fields.setAtpLink("atpLink");
        fields.setFoundIn(new Fields.FoundIn("foundIn"));
        fields.setStatus(new Fields.Status("status"));
        fields.setEnvironment("environment");
        request.setFields(fields);

        JiraIssueCreateRequestDto jiraIssueCreateRequestDto =
                dtoConvertService.convert(request, JiraIssueCreateRequestDto.class);

        Assertions.assertEquals("summary", jiraIssueCreateRequestDto.getFields().getSummary());
        Assertions.assertEquals("description", jiraIssueCreateRequestDto.getFields().getDescription());
        Assertions.assertEquals("project", jiraIssueCreateRequestDto.getFields().getProject().getKey());
        Assertions.assertEquals("priority", jiraIssueCreateRequestDto.getFields().getPriority().getName());
        Assertions.assertEquals(Arrays.asList("label1", "label2"), jiraIssueCreateRequestDto.getFields().getLabels());
        Assertions.assertEquals("issueType", jiraIssueCreateRequestDto.getFields().getIssuetype().getName());
        Assertions.assertNotNull(jiraIssueCreateRequestDto.getFields().getComponents());
        Assertions.assertEquals(2, jiraIssueCreateRequestDto.getFields().getComponents().size());
        Assertions.assertEquals("atpLink", jiraIssueCreateRequestDto.getFields().getCustomfield17400());
        Assertions.assertNull(jiraIssueCreateRequestDto.getFields().getCustomfield27320());
        Assertions.assertEquals("foundIn", jiraIssueCreateRequestDto.getFields().getCustomfield10014().getValue());
        Assertions.assertEquals("status", jiraIssueCreateRequestDto.getFields().getStatus().getName());
        Assertions.assertEquals("environment", jiraIssueCreateRequestDto.getFields().getEnvironment());
        Assertions.assertNull(jiraIssueCreateRequestDto.getFields().getParent());
    }

}
