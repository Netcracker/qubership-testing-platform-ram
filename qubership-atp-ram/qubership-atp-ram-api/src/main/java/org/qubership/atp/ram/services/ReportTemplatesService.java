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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.qubership.atp.auth.springbootstarter.utils.ExceptionUtils;
import org.qubership.atp.ram.exceptions.reporttemplates.RamReportTemplateAlreadyExistsException;
import org.qubership.atp.ram.exceptions.reporttemplates.RamReportTemplateWidgetContentNotFoundException;
import org.qubership.atp.ram.models.ReportTemplate;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.repositories.ReportTemplatesRepository;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportTemplatesService extends CrudService<ReportTemplate> {

    public static final String SYSTEM_DEFAULT_TEMPLATE_UUID = "97b9d3b2-dc83-4a9d-9b75-f8a1307f89f7";
    private final ReportTemplatesRepository repository;

    @Override
    protected MongoRepository<ReportTemplate, UUID> repository() {
        return repository;
    }

    public List<ReportTemplate> getTemplatesByProjectId(UUID projectUuid) {
        return repository.findAllByProjectId(projectUuid);
    }

    public void deleteByUuid(UUID uuid) {
        repository.deleteByUuid(uuid);
    }


    /**
     * system default Report Template with hidden UUID to avoid its modification via rest api.
     *
     * @return default Report Template object
     */
    public ReportTemplate getDefaultTemplate() {
        ReportTemplate defaultTemplate = repository.findByUuid(UUID.fromString(SYSTEM_DEFAULT_TEMPLATE_UUID));
        defaultTemplate.setUuid(null);
        return defaultTemplate;
    }

    /**
     * Search and return first available active Report Template for project.
     * If it is unavailable Default Report Template {@link #SYSTEM_DEFAULT_TEMPLATE_UUID} is returned.
     *
     * @param projectId projectId of the Project associated with Report Templates.
     * @return report template object {@link ReportTemplate}
     */
    public ReportTemplate getActiveTemplateByProjectId(UUID projectId) {
        List<ReportTemplate> templates = repository.findAllByProjectId(projectId);
        return templates.stream()
                .filter(ReportTemplate::isActive)
                .findFirst()
                .orElseGet(this::getDefaultTemplate);
    }

    @Override
    public ReportTemplate save(ReportTemplate reportTemplate) {
        final String templateName = reportTemplate.getName();
        final UUID projectId = reportTemplate.getProjectId();

        boolean isReportTemplateNameExists = false;
        if (reportTemplate.getUuid() == null) {
            isReportTemplateNameExists = repository.existsByNameAndProjectId(templateName, projectId);
        }
        if (isReportTemplateNameExists) {
            log.error("Report template with provided name '{}' already exists in the project '{}'",
                    templateName, projectId);
            ExceptionUtils.throwWithLog(log, new RamReportTemplateAlreadyExistsException(templateName));
        }

        if (reportTemplate.isActive()) {
            disableActiveTemplatesForProject(reportTemplate);
        }
        return super.save(reportTemplate);
    }

    /**
     * Update report template.
     *
     * @param projectId      project identifier
     * @param reportTemplate updated report template content
     * @return updated report template
     */
    public ReportTemplate update(UUID projectId, ReportTemplate reportTemplate) {
        log.info("Update report template for project '{}' with new content '{}'", projectId, reportTemplate);

        if (reportTemplate.isActive()) {
            disableActiveTemplatesForProject(reportTemplate);
        }

        return super.save(reportTemplate);
    }

    private void disableActiveTemplatesForProject(ReportTemplate reportTemplate) {
        List<ReportTemplate> templates = repository.findAllByProjectId(reportTemplate.getProjectId());
        List<ReportTemplate> disabledTemplates = templates.stream()
                .filter(template -> template.isActive()
                        && !template.equals(reportTemplate))
                .peek(template -> template.setActive(false))
                .collect(Collectors.toList());

        if (log.isDebugEnabled()) {
            log.debug("Disabling active templates"
                    + StringUtils.join(templates, ",")
                    + " for project = " + reportTemplate.getProjectId());
        }

        saveAll(disabledTemplates);
    }

    /**
     * Get report template widget template content.
     *
     * @param widget widget type
     * @return template content
     * @throws IOException if cannot find template resource file
     */
    public String getReportTemplateWidgetTemplate(WidgetType widget) throws IOException {
        log.info("Load report template '{}' widget content", widget);
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        String templateFilePath = widget.getTemplateFilePath();
        Resource resource = resourceLoader.getResource(templateFilePath);

        if (!resource.exists()) {
            ExceptionUtils.throwWithLog(log, new RamReportTemplateWidgetContentNotFoundException(widget));
        }

        Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
        String content = FileCopyUtils.copyToString(reader);
        reader.close();
        log.debug("Found template content: {}", content);

        return content;
    }
}
