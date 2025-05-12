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

import static java.util.Objects.nonNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.ram.dto.request.ValidationLabelConfigTemplateSearchRequest;
import org.qubership.atp.ram.models.ValidationLabelConfigTemplate;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.qubership.atp.ram.repositories.ValidationLabelConfigTemplateRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationLabelConfigTemplateService extends CrudService<ValidationLabelConfigTemplate> {

    private final ValidationLabelConfigTemplateRepository repository;
    private final WidgetConfigTemplateService widgetConfigTemplateService;

    @Override
    protected MongoRepository<ValidationLabelConfigTemplate, UUID> repository() {
        return repository;
    }

    /**
     * Find all validation label config templates.
     *
     * @param searchRequest search request
     * @return result templates
     */
    public List<ValidationLabelConfigTemplate> getAll(ValidationLabelConfigTemplateSearchRequest searchRequest) {
        log.info("Find all validation label config templates, filters: '{}'", searchRequest);
        final String name = searchRequest.getName();
        final UUID projectId = searchRequest.getProjectId();

        if (!StringUtils.isEmpty(name) && nonNull(projectId)) {
            return repository.findAllByProjectIdAndNameContains(projectId, name);
        } else if (!StringUtils.isEmpty(name)) {
            return repository.findAllByNameContains(name);
        } else if (nonNull(projectId)) {
            return repository.findAllByProjectId(projectId);
        } else {
            return getAll();
        }
    }

    /**
     * Create new validation label config template.
     *
     * @param template validation label config template
     * @return saved result
     */
    public ValidationLabelConfigTemplate create(ValidationLabelConfigTemplate template) {
        log.info("Create new validation label config template: {}", template);
        return save(template);
    }

    /**
     * Delete validation label config template.
     *
     * @param validationTemplateId deleted template identifier
     */
    public void delete(UUID validationTemplateId) {
        log.info("Delete validation label config template '{}'", validationTemplateId);

        repository.deleteById(validationTemplateId);

        List<WidgetConfigTemplate> referencedWidgetConfigTemplates =
                widgetConfigTemplateService.getWidgetConfigTemplatesWithValidationTemplateId(validationTemplateId);

        referencedWidgetConfigTemplates.stream()
                .flatMap(template -> template.getWidgets()
                        .stream()
                        .filter(widgetConfig -> nonNull(widgetConfig.getValidationTemplateId())))
                .filter(widgetConfig -> validationTemplateId.equals(widgetConfig.getValidationTemplateId()))
                .forEach(widgetConfig -> widgetConfig.setValidationTemplateId(null));

        widgetConfigTemplateService.updateAll(referencedWidgetConfigTemplates);
    }

    /**
     * Update validation label config template.
     *
     * @param id              updated template identifier
     * @param updatedTemplate updated template
     * @return updated result
     */
    public ValidationLabelConfigTemplate update(UUID id, ValidationLabelConfigTemplate updatedTemplate) {
        log.info("Update validation label config template '{}' with new content: {}", id, updatedTemplate);
        return save(updatedTemplate);
    }

    public UUID getProjectIdByConfigId(UUID id) {
        Optional<ValidationLabelConfigTemplate> configTemplate = repository.findById(id);
        return configTemplate.map(ValidationLabelConfigTemplate::getProjectId).orElse(null);
    }
}
