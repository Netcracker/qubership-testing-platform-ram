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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpIllegalNullableArgumentException;
import org.qubership.atp.ram.dto.request.WidgetConfigTemplateSearchRequest;
import org.qubership.atp.ram.dto.response.ExecutionRequestWidgetConfigTemplateResponse;
import org.qubership.atp.ram.models.ColumnVisibility;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.ExecutionRequestConfig;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.qubership.atp.ram.repositories.ExecutionRequestConfigRepository;
import org.qubership.atp.ram.repositories.WidgetConfigTemplateRepository;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WidgetConfigTemplateService extends CrudService<WidgetConfigTemplate> {

    public static final String DEFAULT_TEMPLATE_UUID = "578a9810-8e8d-49ec-ba9a-6b26a22d5d70";

    private final WidgetConfigTemplateRepository repository;
    private final ExecutionRequestConfigRepository executionRequestConfigRepository;
    private final ExecutionRequestService executionRequestService;
    private final ModelMapper modelMapper;

    /**
     * Constructor.
     */
    public WidgetConfigTemplateService(WidgetConfigTemplateRepository repository,
                                       ExecutionRequestConfigRepository executionRequestConfigRepository,
                                       @Lazy ExecutionRequestService executionRequestService,
                                       ModelMapper modelMapper) {
        this.repository = repository;
        this.executionRequestConfigRepository = executionRequestConfigRepository;
        this.executionRequestService = executionRequestService;
        this.modelMapper = modelMapper;
    }

    @Override
    protected MongoRepository<WidgetConfigTemplate, UUID> repository() {
        return repository;
    }

    /**
     * Get widget config template for execution request with id.
     *
     * @param executionRequestId execution request id
     * @return widget config template
     */
    public ExecutionRequestWidgetConfigTemplateResponse getWidgetConfigTemplateForEr(UUID executionRequestId) {
        final ExecutionRequest executionRequest = executionRequestService.get(executionRequestId);

        return getWidgetConfigTemplateForEr(executionRequest);
    }

    /**
     * Get widget config template for execution request with id.
     *
     * @param request execution request
     * @return widget config template
     */
    public ExecutionRequestWidgetConfigTemplateResponse getWidgetConfigTemplateForEr(ExecutionRequest request) {
        log.info("Get widget config template for execution request '{}'", request.getUuid());
        final ExecutionRequestConfig executionRequestConfig =
                executionRequestService.getExecutionRequestConfig(request);

        final UUID erWidgetConfigTemplateId = executionRequestConfig.getWidgetConfigTemplateId();
        log.debug("Execution request widget config template id: {}", erWidgetConfigTemplateId);

        WidgetConfigTemplate executionRequestWidgetConfigTemplate = null;
        if (nonNull(erWidgetConfigTemplateId)) {
            executionRequestWidgetConfigTemplate = get(erWidgetConfigTemplateId);
            log.debug("Found execution request widget config template: {}", executionRequestWidgetConfigTemplate);
        }

        log.debug("Result template: {}", executionRequestWidgetConfigTemplate);

        return new ExecutionRequestWidgetConfigTemplateResponse(executionRequestWidgetConfigTemplate,
                executionRequestConfig.isDefaultLabelTemplateChanged());
    }

    /**
     * Get column visibility map for specified ER and widget.
     *
     * @param executionRequestId execution request identifier
     * @param widgetId           widget identifier
     * @return visibility map
     */
    public Map<String, Boolean> getWidgetColumnVisibilityMap(UUID executionRequestId, UUID widgetId) {
        WidgetConfigTemplate template = getWidgetConfigTemplateForEr(executionRequestId).getTemplate();

        WidgetConfigTemplate.WidgetConfig widgetConfig;
        if (nonNull(template)) {
            widgetConfig = template.getWidgetConfig(widgetId);
        } else {
            widgetConfig = getDefaultTemplate().getWidgetConfig(widgetId);
        }
        List<ColumnVisibility> widgetConfigColumnVisibilities = widgetConfig.getColumnVisibilities();
        List<ColumnVisibility> columnVisibilities = !isEmpty(widgetConfigColumnVisibilities)
                ? widgetConfigColumnVisibilities : Collections.emptyList();

        return columnVisibilities.stream()
                .peek(columnVisibility -> {
                    String name = columnVisibility.getName();
                    columnVisibility.setName(name.replaceAll("_", " "));
                })
                .collect(Collectors.toMap(ColumnVisibility::getName, ColumnVisibility::isVisible));
    }

    /**
     * Find all widget config templates.
     *
     * @param searchRequest search request
     * @return result templates
     */
    public List<WidgetConfigTemplate> getAll(WidgetConfigTemplateSearchRequest searchRequest) {
        log.info("Find all widget config templates, filters: '{}'", searchRequest);
        final String name = searchRequest.getName();
        final UUID projectId = searchRequest.getProjectId();
        final UUID labelTemplateId = searchRequest.getLabelTemplateId();
        final UUID validationTemplateId = searchRequest.getValidationTemplateId();

        if (!StringUtils.isEmpty(name) && nonNull(projectId)) {
            return repository.findAllByProjectIdAndNameContains(projectId, name);
        } else if (!StringUtils.isEmpty(name)) {
            return repository.findAllByNameContains(name);
        } else if (nonNull(projectId)) {
            return repository.findAllByProjectId(projectId);
        } else if (nonNull(labelTemplateId)) {
            return repository.findAllByWidgetsLabelTemplateId(labelTemplateId);
        } else if (nonNull(validationTemplateId)) {
            return repository.findAllByWidgets_validationTemplateId(validationTemplateId);
        } else {
            return getAll();
        }
    }

    /**
     * Create new widget config template.
     *
     * @param template widget config template
     * @return saved result
     */
    public WidgetConfigTemplate create(WidgetConfigTemplate template) {
        log.info("Create new widget config template: {}", template);
        validateWidgetConfigTemplate(template);

        return save(template);
    }

    /**
     * Update widget config template.
     *
     * @param id              updated template identifier
     * @param updatedTemplate updated template
     * @return updated result
     */
    public WidgetConfigTemplate update(UUID id, WidgetConfigTemplate updatedTemplate) {
        log.info("Update widget config template '{}' with new content: {}", id, updatedTemplate);
        final WidgetConfigTemplate existedTemplate = get(id);

        modelMapper.map(updatedTemplate, existedTemplate);

        validateWidgetConfigTemplate(existedTemplate);

        return save(existedTemplate);
    }

    /**
     * Update widget config templates.
     *
     * @param templates updated templates
     */
    public void updateAll(List<WidgetConfigTemplate> templates) {
        log.info("Update widget config templates '{}'", StreamUtils.extractIds(templates));

        repository.saveAll(templates);
    }

    /**
     * Get widget config templates with specified label template id.
     *
     * @param labelTemplateId label template id
     * @return result templates list
     */
    public List<WidgetConfigTemplate> getWidgetConfigTemplatesWithLabelTemplateId(UUID labelTemplateId) {
        return repository.findAllByWidgetsLabelTemplateId(labelTemplateId);
    }

    /**
     * Get widget config templates with specified validation template id.
     *
     * @param validationTemplateId validation template id
     * @return result templates list
     */
    public List<WidgetConfigTemplate> getWidgetConfigTemplatesWithValidationTemplateId(UUID validationTemplateId) {
        return repository.findAllByWidgets_validationTemplateId(validationTemplateId);
    }

    /**
     * Delete widget config template.
     *
     * @param id deleted template identifier
     */
    public void delete(UUID id) {
        log.info("Delete widget config template '{}'", id);
        repository.deleteById(id);

        log.debug("Unset all widget config templates references in execution request configs");
        List<ExecutionRequestConfig> configs = executionRequestConfigRepository.findAllByWidgetConfigTemplateId(id);
        configs.forEach(config -> config.setWidgetConfigTemplateId(null));

        log.debug("Updated configs: {}", StreamUtils.extractIds(configs));
        executionRequestConfigRepository.saveAll(configs);
    }

    /**
     * Validate widget config template.
     *
     * @param template validated template
     */
    private void validateWidgetConfigTemplate(WidgetConfigTemplate template) {
        log.debug("Validate widget config template: {}", template);

        if (isNull(template.getProjectId())) {
            log.error("Failed to create widget config template: project id should be specified");
            throw new AtpIllegalNullableArgumentException("project id", "widget config template");
        }

        if (isNull(template.getName())) {
            log.error("Failed to create widget config template: name should be specified");
            throw new AtpIllegalNullableArgumentException("name", "widget config template");
        }
    }

    /**
     * Get validation template id for execution request.
     *
     * @param executionRequest execution request
     * @param widgetId         widget id
     * @return validation template id
     */
    public UUID getValidationTemplateIdByErWidget(ExecutionRequest executionRequest, UUID widgetId) {
        UUID validationTemplateId = null;

        WidgetConfigTemplate widgetConfigTemplate = getWidgetConfigTemplateForEr(executionRequest).getTemplate();

        if (nonNull(widgetConfigTemplate)) {
            WidgetConfigTemplate.WidgetConfig widgetConfig = widgetConfigTemplate.getWidgetConfig(widgetId);

            return widgetConfig.getValidationTemplateId();
        }

        return validationTemplateId;
    }

    /**
     * Get label template id for execution request.
     *
     * @param executionRequest execution request
     * @param widgetId         widget id
     * @return validation template id
     */
    public UUID defineLabelTemplateId(ExecutionRequest executionRequest, UUID widgetId) {
        UUID labelTemplateId = executionRequest.getLabelTemplateId();

        WidgetConfigTemplate widgetConfigTemplate = getWidgetConfigTemplateForEr(executionRequest).getTemplate();

        if (nonNull(widgetConfigTemplate)) {
            WidgetConfigTemplate.WidgetConfig widgetConfig = widgetConfigTemplate.getWidgetConfig(widgetId);

            return widgetConfig.getLabelTemplateId();
        }

        return labelTemplateId;
    }

    /**
     * default Widget Config Template with hidden UUID to avoid its modification via rest api.
     *
     * @return default Widget Config Template object
     */
    public WidgetConfigTemplate getDefaultTemplate() {
        WidgetConfigTemplate defaultTemplate = repository.findByUuid(UUID.fromString(DEFAULT_TEMPLATE_UUID));
        defaultTemplate.setUuid(null);
        return defaultTemplate;
    }

    public UUID getProjectIdByTemplateId(UUID templateId) {
        Optional<WidgetConfigTemplate> template = repository.findById(templateId);
        return template.map(WidgetConfigTemplate::getProjectId).orElse(null);
    }

    /**
     * Deleted ExecutionRequestConfig.
     */
    public void deleteAllByExecutionRequestIdIn(List<UUID> executionRequestIds) {
        executionRequestConfigRepository.deleteAllByExecutionRequestIdIn(executionRequestIds);
    }
}
