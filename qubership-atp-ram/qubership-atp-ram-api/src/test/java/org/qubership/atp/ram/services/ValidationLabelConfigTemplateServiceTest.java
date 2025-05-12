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
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.qubership.atp.ram.dto.request.ValidationLabelConfigTemplateSearchRequest;
import org.qubership.atp.ram.enums.ExecutionRequestWidgets;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.qubership.atp.ram.repositories.ExecutionRequestConfigRepository;
import org.qubership.atp.ram.repositories.ValidationLabelConfigTemplateRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class ValidationLabelConfigTemplateServiceTest {

    @InjectMocks
    private ValidationLabelConfigTemplateService service;

    @Mock
    private ValidationLabelConfigTemplateRepository repository;

    @Mock
    private WidgetConfigTemplateService widgetConfigTemplateService;

    @Mock
    private ExecutionRequestConfigRepository executionRequestConfigRepository;

    @Captor
    private ArgumentCaptor<List<WidgetConfigTemplate>> configsCaptor;

    @Test
    public void testGetAll_searchRequestWithAllParams_shouldBeCalledFindAllByProjectIdAndNameContains() {
        ValidationLabelConfigTemplateSearchRequest searchRequest = new ValidationLabelConfigTemplateSearchRequest();
        searchRequest.setName("Lorem ipsum");
        searchRequest.setProjectId(UUID.randomUUID());

        service.getAll(searchRequest);

        verify(repository).findAllByProjectIdAndNameContains(searchRequest.getProjectId(), searchRequest.getName());
    }

    @Test
    public void testGetAll_searchRequestWithNameParam_shouldBeCalledFindAllByNameContains() {
        ValidationLabelConfigTemplateSearchRequest searchRequest = new ValidationLabelConfigTemplateSearchRequest();
        searchRequest.setName("Lorem ipsum");

        service.getAll(searchRequest);

        verify(repository).findAllByNameContains(searchRequest.getName());
    }

    @Test
    public void testGetAll_searchRequestWithProjectIdParam_shouldBeCalledFindAllByProjectId() {
        ValidationLabelConfigTemplateSearchRequest searchRequest = new ValidationLabelConfigTemplateSearchRequest();
        searchRequest.setProjectId(UUID.randomUUID());

        service.getAll(searchRequest);

        verify(repository).findAllByProjectId(searchRequest.getProjectId());
    }

    @Test
    public void testGetAll_searchRequestWithoutSearchParams_shouldBeCalledFindAll() {
        ValidationLabelConfigTemplateSearchRequest searchRequest = new ValidationLabelConfigTemplateSearchRequest();

        service.getAll(searchRequest);

        verify(repository).findAll();
    }

    @Test
    public void testDelete_deleteWithErConfigsUpdate_shouldDeletedAndUpdated() {
        final UUID validationLabelConfigTemplateId = UUID.randomUUID();
        final UUID summaryStatisticWidgetId = ExecutionRequestWidgets.SUMMARY_STATISTIC.getWidgetId();
        final UUID statisticForUsagesWidgetId = ExecutionRequestWidgets.SUMMARY_STATISTIC_FOR_USAGES.getWidgetId();
        List<WidgetConfigTemplate> configs = asList(
                new WidgetConfigTemplate(UUID.randomUUID(), asList(
                        new WidgetConfigTemplate.WidgetConfig(summaryStatisticWidgetId, UUID.randomUUID(),
                                validationLabelConfigTemplateId, emptyList(), false, null, null),
                        new WidgetConfigTemplate.WidgetConfig(statisticForUsagesWidgetId, UUID.randomUUID(),
                                validationLabelConfigTemplateId, emptyList(), false, null, null)
                )),
                new WidgetConfigTemplate(UUID.randomUUID(), singletonList(
                        new WidgetConfigTemplate.WidgetConfig(summaryStatisticWidgetId, UUID.randomUUID(),
                                validationLabelConfigTemplateId, emptyList(), false, null, null)
                ))
        );

        when(widgetConfigTemplateService.getWidgetConfigTemplatesWithValidationTemplateId(validationLabelConfigTemplateId))
                .thenReturn(configs);

        service.delete(validationLabelConfigTemplateId);

        verify(repository).deleteById(validationLabelConfigTemplateId);
        verify(widgetConfigTemplateService).updateAll(configsCaptor.capture());

        final List<WidgetConfigTemplate> updatedConfigs = configsCaptor.getValue();

        Assertions.assertNotNull(updatedConfigs);
        Assertions.assertFalse(updatedConfigs.isEmpty());
        Assertions.assertEquals(configs.size(), updatedConfigs.size());
        Assertions.assertTrue(updatedConfigs.stream()
                .flatMap(template -> template.getWidgets().stream())
                .allMatch(widgetConfig -> widgetConfig.getValidationTemplateId() == null));
    }
}
