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

import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.qubership.atp.ram.ExecutionRequestsMock;
import org.qubership.atp.ram.WidgetConfigTemplateMock;
import org.qubership.atp.ram.dto.request.WidgetConfigTemplateSearchRequest;
import org.qubership.atp.ram.dto.response.ExecutionRequestWidgetConfigTemplateResponse;
import org.qubership.atp.ram.enums.ExecutionRequestWidgets;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.ExecutionRequestConfig;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.qubership.atp.ram.repositories.ExecutionRequestConfigRepository;
import org.qubership.atp.ram.repositories.WidgetConfigTemplateRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class WidgetConfigTemplateServiceTest {

    @InjectMocks
    private WidgetConfigTemplateService service;

    @Mock
    private WidgetConfigTemplateRepository repository;

    @Mock
    private ExecutionRequestService executionRequestService;

    @Mock
    private ExecutionRequestConfigRepository executionRequestConfigRepository;

    @Captor
    private ArgumentCaptor<List<ExecutionRequestConfig>> configsCaptor;

    @Test
    public void testGetWidgetConfigTemplateForExecutionRequest_erWithoutWidgetTemplate_shouldBeCreatedDefault() {
        // given
        final ExecutionRequest executionRequest = ExecutionRequestsMock.generateRequest();
        executionRequest.setProjectId(UUID.randomUUID());
        executionRequest.setLabelTemplateId(UUID.randomUUID());

        final UUID executionRequestId = executionRequest.getUuid();
        final WidgetConfigTemplate widgetConfigTemplate = WidgetConfigTemplateMock.generateWidgetConfigTemplate(ExecutionRequestWidgets.SUMMARY_STATISTIC);
        final UUID widgetConfigTemplateId = widgetConfigTemplate.getUuid();
        final ExecutionRequestConfig executionRequestConfig = new ExecutionRequestConfig(executionRequestId, widgetConfigTemplateId, false);

        when(executionRequestService.get(executionRequestId)).thenReturn(executionRequest);
        when(executionRequestService.getExecutionRequestConfig(executionRequest)).thenReturn(executionRequestConfig);
        when(repository.findById(widgetConfigTemplateId)).thenReturn(Optional.of(widgetConfigTemplate));

        // when
        final ExecutionRequestWidgetConfigTemplateResponse result =
                service.getWidgetConfigTemplateForEr(executionRequestId);

        // then
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getTemplate());
        Assertions.assertEquals(widgetConfigTemplateId, result.getTemplate().getUuid());
    }

    @Test
    public void testGetAll_searchRequestWithAllParams_shouldBeCalledFindAllByProjectIdAndNameContains() {
        WidgetConfigTemplateSearchRequest searchRequest = new WidgetConfigTemplateSearchRequest();
        searchRequest.setName("Lorem ipsum");
        searchRequest.setProjectId(UUID.randomUUID());

        service.getAll(searchRequest);

        verify(repository).findAllByProjectIdAndNameContains(searchRequest.getProjectId(), searchRequest.getName());
    }

    @Test
    public void testGetAll_searchRequestWithNameParam_shouldBeCalledFindAllByNameContains() {
        WidgetConfigTemplateSearchRequest searchRequest = new WidgetConfigTemplateSearchRequest();
        searchRequest.setName("Lorem ipsum");

        service.getAll(searchRequest);

        verify(repository).findAllByNameContains(searchRequest.getName());
    }

    @Test
    public void testGetAll_searchRequestWithProjectIdParam_shouldBeCalledFindAllByProjectId() {
        WidgetConfigTemplateSearchRequest searchRequest = new WidgetConfigTemplateSearchRequest();
        searchRequest.setProjectId(UUID.randomUUID());

        service.getAll(searchRequest);

        verify(repository).findAllByProjectId(searchRequest.getProjectId());
    }

    @Test
    public void testGetAll_searchRequestWithoutSearchParams_shouldBeCalledFindAll() {
        WidgetConfigTemplateSearchRequest searchRequest = new WidgetConfigTemplateSearchRequest();

        service.getAll(searchRequest);

        verify(repository).findAll();
    }

    @Test
    public void testDelete_deleteWithErConfigsUpdate_shouldDeletedAndUpdated() {
        final UUID widgetConfigTemplateId = UUID.randomUUID();
        List<ExecutionRequestConfig> configs = asList(
                new ExecutionRequestConfig(UUID.randomUUID(), widgetConfigTemplateId, false),
                new ExecutionRequestConfig(UUID.randomUUID(), widgetConfigTemplateId, false),
                new ExecutionRequestConfig(UUID.randomUUID(), widgetConfigTemplateId, false)
        );

        when(executionRequestConfigRepository.findAllByWidgetConfigTemplateId(widgetConfigTemplateId)).thenReturn(configs);

        service.delete(widgetConfigTemplateId);

        verify(repository).deleteById(widgetConfigTemplateId);
        verify(executionRequestConfigRepository).saveAll(configsCaptor.capture());

        final List<ExecutionRequestConfig> updatedConfigs = configsCaptor.getValue();

        Assertions.assertNotNull(updatedConfigs);
        Assertions.assertFalse(updatedConfigs.isEmpty());
        Assertions.assertEquals(configs.size(), updatedConfigs.size());
        Assertions.assertTrue(updatedConfigs.stream().allMatch(config -> config.getWidgetConfigTemplateId() == null));
    }
}
