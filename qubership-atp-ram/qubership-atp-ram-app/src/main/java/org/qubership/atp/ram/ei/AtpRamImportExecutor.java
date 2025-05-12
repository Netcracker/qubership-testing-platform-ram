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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.qubership.atp.ei.node.ImportExecutor;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ValidationResult;
import org.qubership.atp.ei.node.dto.validation.UserMessage;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.qubership.atp.ram.exceptions.internal.RamImportFileLoadException;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.models.RamObject;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.repositories.FailPatternRepository;
import org.qubership.atp.ram.repositories.RootCauseRepository;
import org.qubership.atp.ram.repositories.impl.FieldConstants;
import org.qubership.atp.ram.services.RootCauseService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AtpRamImportExecutor implements ImportExecutor {

    private final RootCauseRepository rootCauseRepository;
    private final RootCauseService rootCauseService;
    private final FailPatternRepository failPatternRepository;
    private final ObjectLoaderFromDiskService objectLoaderFromDiskService;

    @Override
    @CacheEvict(value = "rootcauses", allEntries = true)
    public void importData(ExportImportData importData, Path path) {
        log.info("Start import. Data: {}, WorkDir: {}", importData, path);
        importFailReasons(importData, path);
        importFailPatterns(importData, path);
        log.info("End of import");
    }

    /**
     * Imports fail reasons.
     *
     * @param importData import data
     * @param workDir    working directory
     */
    public void importFailReasons(ExportImportData importData, Path workDir) {
        Map<UUID, Path> rootCauseFiles =
                objectLoaderFromDiskService.getListOfObjects(workDir, RootCause.class);
        Map<UUID, UUID> replacementMap = importData.getReplacementMap();
        log.debug("importRootCauses list: {}", rootCauseFiles);
        boolean isReplacement = importData.isInterProjectImport() || importData.isCreateNewProject();
        rootCauseFiles.forEach((rootCauseId, filePath) -> {
            log.debug("importRootCauses starts import: {}.", rootCauseId);
            RootCause rootCauseObject = load(filePath, replacementMap, isReplacement, RootCause.class);
            if (rootCauseObject == null) {
                final String path = filePath.toString();
                log.error("Failed to upload file using path: {}", filePath);
                throw new RamImportFileLoadException(path);
            }
            log.debug("Imports root cause:{}", rootCauseObject);
            RootCause existingRootCause = rootCauseService.getByIdOrNull(rootCauseObject.getUuid());
            if (existingRootCause == null) {
                log.debug("existing root cause is null, id: {}", rootCauseId);
                Map<UUID, String> newObjectNamesMap = importData.getNewObjectNamesMap();
                if (newObjectNamesMap.containsKey(rootCauseId)) {
                    RootCause rootCause = new RootCause();
                    rootCause.setUuid(rootCauseObject.getUuid());
                    rootCause.setParentId(rootCauseObject.getParentId());
                    rootCause.setProjectId(rootCauseObject.getProjectId());
                    rootCause.setName(newObjectNamesMap.getOrDefault(rootCauseId, "New Root Cause"));
                    rootCause.setType(rootCauseObject.getType());
                    rootCause.setDefault(rootCauseObject.isDefault());
                    rootCause.setDisabled(rootCauseObject.isDisabled());
                    rootCauseObject = rootCause;
                }
            } else {
                log.debug("updating root cause with id: {}", rootCauseId);
            }
            checkAndCorrectRootCauseName(rootCauseObject, importData.getProjectId());
            rootCauseRepository.save(rootCauseObject);
        });
    }

    /**
     * Imports fail patterns.
     *
     * @param importData import data
     * @param workDir    working directory
     */
    public void importFailPatterns(ExportImportData importData, Path workDir) {
        Map<UUID, Path> failPatternFiles =
                objectLoaderFromDiskService.getListOfObjects(workDir, FailPattern.class);
        Map<UUID, UUID> replacementMap = importData.getReplacementMap();
        log.debug("importFailPatterns list: {}", failPatternFiles);
        boolean isReplacement = importData.isInterProjectImport() || importData.isCreateNewProject();
        failPatternFiles.forEach((failPatternId, filePath) -> {
            log.debug("importFailPatterns starts import: {}.", failPatternId);
            FailPattern failPatternObject = load(filePath, replacementMap, isReplacement, FailPattern.class);
            log.debug("Imports fail pattern:{}", failPatternObject);
            if (failPatternObject == null) {
                final String path = filePath.toString();
                log.error("Failed to upload file using path: {}", filePath);
                throw new RamImportFileLoadException(path);
            }
            failPatternObject.setProjectId(importData.getProjectId());
            failPatternObject.setFailReasonId(replacementMap.getOrDefault(failPatternObject.getFailReasonId(),
                    failPatternObject.getFailReasonId()));
            failPatternRepository.save(failPatternObject);
        });
    }

    @Override
    public ValidationResult preValidateData(ExportImportData importData, Path path) {
        return null;
    }

    @Override
    public ValidationResult validateData(ExportImportData importData, Path path) throws Exception {
        log.info("start validateData(importData: {}, workDir: {})", importData, path);
        Map<UUID, UUID> replacementMap = new HashMap<>(importData.getReplacementMap());
        List<UserMessage> details = new ArrayList<>();
        Set<String> messages = new HashSet<>();
        if (importData.isCreateNewProject()) {
            Set<UUID> ids = objectLoaderFromDiskService.getListOfObjects(path, RootCause.class).keySet();
            ids.addAll(objectLoaderFromDiskService.getListOfObjects(path, FailPattern.class).keySet());
            ids.forEach(id -> replacementMap.put(id, UUID.randomUUID()));
            messages.addAll(checkDuplicateNameForRootCauses(path, false, replacementMap,
                    importData.getProjectId()));
        } else if (importData.isInterProjectImport()) {
            replacementMap.putAll(getSourceTargetMap(path, replacementMap));
            replacementMap.entrySet().forEach(entry -> {
                if (entry.getValue() == null) {
                    entry.setValue(UUID.randomUUID());
                }
            });
            messages.addAll(checkDuplicateNameForRootCauses(path, true, replacementMap,
                    importData.getProjectId()));
        } else {
            messages.addAll(checkDuplicateNameForRootCauses(path, false, replacementMap,
                    importData.getProjectId()));
        }
        if (CollectionUtils.isNotEmpty(messages)) {
            details.addAll(messages.stream().map(UserMessage::new).collect(Collectors.toList()));
        }
        return new ValidationResult(details, replacementMap);
    }

    /**
     * Gets source target replacement map for root causes and fail patterns.
     *
     * @param workDir        working directory
     * @param replacementMap replacement map
     * @return map of replacements
     */
    public Map<UUID, UUID> getSourceTargetMap(final Path workDir, final Map<UUID, UUID> replacementMap) {
        log.debug("Get source target replacement map for root causes and fail patterns");
        Map<UUID, UUID> result = new HashMap<>();
        Map<UUID, Path> failReasonsToImport = objectLoaderFromDiskService.getListOfObjects(workDir, RootCause.class);
        failReasonsToImport.forEach((uuid, filePath) -> {
            RootCause object = objectLoaderFromDiskService.loadFileAsObjectWithReplacementMap(filePath, RootCause.class,
                    replacementMap);
            RootCause existingObject = rootCauseRepository.findByProjectIdAndUuid(object.getProjectId(), uuid);
            processReplacementMapWithFoundObjects(object.getProjectId(), uuid, existingObject, result,
                    FieldConstants.FAIL_REASON);
        });
        Map<UUID, Path> failPatternsToImport = objectLoaderFromDiskService.getListOfObjects(workDir, FailPattern.class);
        failPatternsToImport.forEach((uuid, filePath) -> {
            FailPattern object = objectLoaderFromDiskService.loadFileAsObjectWithReplacementMap(
                    filePath, FailPattern.class, replacementMap);
            FailPattern existingObject = failPatternRepository.findByProjectIdAndUuid(object.getProjectId(), uuid);
            processReplacementMapWithFoundObjects(object.getProjectId(), uuid, existingObject, result,
                    FieldConstants.FAIL_PATTERN);
        });
        return result;
    }

    /**
     * Fills replacement map for RootCauses and FailPatterns.
     *
     * @param projectId      projectId
     * @param uuid           uuid
     * @param existingObject existing object
     * @param result         map of replacements
     * @param objectName     failReason or failPattern
     */
    private <T extends RamObject> void processReplacementMapWithFoundObjects(
            UUID projectId, UUID uuid, T existingObject, Map<UUID, UUID> result, String objectName) {
        if (existingObject == null) {
            log.debug("{} by projectId: [{}] and sourceId: [{}] not found", objectName, projectId, uuid);
            log.debug("Put {}: null to replacementMap", uuid);
            result.put(uuid, null);
        } else {
            log.debug("{} by projectId: [{}] and sourceId: [{}] found", objectName, projectId, uuid);
            log.debug("Put {}: {} to replacementMap", uuid, existingObject.getUuid());
            result.put(uuid, existingObject.getUuid());
        }
    }

    /**
     * Loads object from disk by path with replacement map.
     *
     * @param filePath       file path
     * @param replacementMap replacement map
     * @param isReplacement  isReplacement
     * @param objectClass    RootCause.class or FailPattern.class
     * @param <T>            RootCause or FailPattern
     * @return object of RootCause or FailPattern
     */
    private <T> T load(Path filePath, Map<UUID, UUID> replacementMap, boolean isReplacement, Class<T> objectClass) {
        if (isReplacement) {
            log.debug("Load object by path [{}] with replacementMap: {}", filePath, replacementMap);
            return objectLoaderFromDiskService.loadFileAsObjectWithReplacementMap(filePath, objectClass,
                    replacementMap, true, false);
        } else {
            log.debug("Load object by path [{}] without replacementMap", filePath);
            return objectLoaderFromDiskService.loadFileAsObject(filePath, objectClass);
        }
    }

    /**
     * Check duplicate name for new root causes set.
     *
     * @param workDir              the work dir
     * @param isInterProjectImport the is inter project import
     * @param repMap               the rep map
     * @param targetProjectId      target project id
     * @return the set
     */
    private Set<String> checkDuplicateNameForRootCauses(Path workDir, boolean isInterProjectImport,
                                                        Map<UUID, UUID> repMap, UUID targetProjectId) {
        Set<String> result = new HashSet<>();
        Map<UUID, Path> objectsToImport = objectLoaderFromDiskService.getListOfObjects(workDir, RootCause.class);
        objectsToImport.forEach((id, path) -> {
            RootCause object;
            if (isInterProjectImport) {
                object = objectLoaderFromDiskService.loadFileAsObjectWithReplacementMap(path, RootCause.class, repMap);
            } else {
                object = objectLoaderFromDiskService.loadFileAsObject(path, RootCause.class);
            }
            log.debug("importing object {}", object);
            if (object == null) {
                log.error("Cannot load file by path {}", path.toString());
                result.add("Some import file cannot be loaded. Please find details in log files.");
                return;
            }
            checkDuplicateNameForNewRootCauses(object, targetProjectId, result);
        });
        return result;
    }

    private void checkDuplicateNameForNewRootCauses(RootCause rootCause, UUID targetProjectId, Set<String> result) {
        if (rootCauseService.isNameUsed(rootCause, targetProjectId)) {
            String message =
                    String.format("The %s with name '%s' already exists. New one will be rename to '%s Copy'",
                            rootCause.getClass().getSimpleName(), rootCause.getName(), rootCause.getName());
            log.info(message);
            result.add(message);
        }
    }

    /**
     * Checks and corrects root cause name.
     *
     * @param rootCause the root cause
     */
    private void checkAndCorrectRootCauseName(RootCause rootCause, UUID targetProjectId) {
        int i = 0;
        String newName;
        String initName = rootCause.getName();
        while (rootCauseService.isNameUsed(rootCause, targetProjectId)) {
            if (i == 0) {
                initName = rootCause.getName() + " Copy";
                newName = initName;
            } else {
                newName = initName + " _" + i;
            }
            rootCause.setName(newName);
            ++i;
        }
    }
}
