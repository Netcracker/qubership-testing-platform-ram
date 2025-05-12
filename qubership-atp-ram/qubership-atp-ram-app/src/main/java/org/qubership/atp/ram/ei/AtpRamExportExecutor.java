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

package org.qubership.atp.ram.ei;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.qubership.atp.ei.node.ExportExecutor;
import org.qubership.atp.ei.node.constants.Constant;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.exceptions.ExportException;
import org.qubership.atp.ei.node.services.ObjectSaverToDiskService;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.RootCauseType;
import org.qubership.atp.ram.repositories.FailPatternRepository;
import org.qubership.atp.ram.repositories.RootCauseRepository;
import org.qubership.atp.ram.services.RootCauseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AtpRamExportExecutor implements ExportExecutor {

    @Value("${spring.application.name}")
    private String implementationName;

    private final RootCauseService rootCauseService;
    private final RootCauseRepository rootCauseRepository;
    private final FailPatternRepository failPatternRepository;
    private final ObjectSaverToDiskService objectSaverToDiskService;

    @Override
    public void exportToFolder(ExportImportData exportData, Path path) throws ExportException {
        log.info("Start export. Request {}", exportData);
        exportData.getExportScope().getEntities().getOrDefault(Constant.ENTITY_PROJECTS, new HashSet<>())
                .forEach(projectId -> {
                    Set<String> failReasonsEntity = exportData.getExportScope().getEntities()
                            .getOrDefault(ServiceScopeEntities.ENTITY_RAM_FAIL_REASONS.getValue(), new HashSet<>());
                    if (!failReasonsEntity.isEmpty()) {
                        Set<RootCause> rootCauseList = new HashSet<>();
                        rootCauseList.addAll(rootCauseRepository.findAllByParentIdIsNullAndType(RootCauseType.GLOBAL));
                        rootCauseList.addAll(rootCauseRepository.findAllByParentIdIsNullAndProjectIdAndType(
                                UUID.fromString(projectId), RootCauseType.CUSTOM));

                        List<List<RootCause>> rootCauseChildren = new ArrayList<>();
                        rootCauseList.forEach(rootCause -> collectChildRootCauses(rootCauseChildren, rootCause,
                                UUID.fromString(projectId)));

                        rootCauseList.addAll(rootCauseChildren
                                .stream()
                                .flatMap(List::stream)
                                .collect(Collectors.toList()));

                        Map<UUID, RootCause> rootCauses = rootCauseList.stream()
                                .distinct()
                                .collect(Collectors.toMap(RootCause::getUuid, Function.identity()));
                        rootCauses.forEach((rootCauseId, rootCause) ->
                                objectSaverToDiskService.exportAtpEntity(rootCauseId, rootCause, path));
                    }
                    Set<String> failPatternsEntity = exportData.getExportScope().getEntities()
                            .getOrDefault(ServiceScopeEntities.ENTITY_RAM_FAIL_PATTERNS.getValue(), new HashSet<>());
                    if (!failPatternsEntity.isEmpty()) {
                        Map<UUID, FailPattern> failPatterns = failPatternRepository
                                .findAllByProjectId(UUID.fromString(projectId))
                                .stream()
                                .collect(Collectors.toMap(FailPattern::getUuid, Function.identity()));
                        failPatterns.forEach((failPatternId, failPattern) ->
                                objectSaverToDiskService.exportAtpEntity(failPatternId, failPattern, path));
                    }
                });
        log.info("End export. Request {}", exportData);
    }

    @Override
    public String getExportImplementationName() {
        return implementationName;
    }

    /**
     * Collects child root causes by parent root cause and project id.
     * @param rootCauseChildren list of lists with child root causes
     * @param parentRootCause parent root cause
     * @param projectId project id
     */
    private void collectChildRootCauses(List<List<RootCause>> rootCauseChildren, RootCause parentRootCause,
                                        UUID projectId) {
        List<RootCause> children = rootCauseService.getChildrenRootCauses(parentRootCause, projectId);
        if (CollectionUtils.isEmpty(children)) {
            return;
        }
        rootCauseChildren.add(children);
        children.forEach(rootCause -> collectChildRootCauses(rootCauseChildren, rootCause, projectId));
    }
}
