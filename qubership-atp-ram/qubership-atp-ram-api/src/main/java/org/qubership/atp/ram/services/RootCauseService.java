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
import static org.qubership.atp.ram.utils.StreamUtils.extractIds;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.modelmapper.ModelMapper;
import org.qubership.atp.auth.springbootstarter.utils.ExceptionUtils;
import org.qubership.atp.ram.exceptions.rootcauses.RamRootCauseAlreadyExistsException;
import org.qubership.atp.ram.exceptions.rootcauses.RamRootCauseIllegalAccessException;
import org.qubership.atp.ram.exceptions.rootcauses.RamRootCauseIllegalTypeException;
import org.qubership.atp.ram.model.request.RootCauseUpsertRequest;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.RootCauseTreeNode;
import org.qubership.atp.ram.models.RootCauseType;
import org.qubership.atp.ram.models.TreeNode;
import org.qubership.atp.ram.repositories.RootCauseRepository;
import org.qubership.atp.ram.utils.SecurityUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RootCauseService extends CrudService<RootCause> {
    private final RootCauseRepository repository;
    private final TestRunService testRunService;
    private final ModelMapper modelMapper;

    /**
     * RootCauseService constructor.
     *
     * @param repository     root cause repository
     * @param testRunService test run service
     * @param modelMapper    model mapper
     */
    public RootCauseService(RootCauseRepository repository,
                            @Lazy TestRunService testRunService,
                            ModelMapper modelMapper) {
        this.repository = repository;
        this.testRunService = testRunService;
        this.modelMapper = modelMapper;
    }

    @Override
    protected MongoRepository<RootCause, UUID> repository() {
        return repository;
    }

    @Cacheable("rootcauses")
    public List<RootCause> getAllRootCauses() {
        log.info("Get all root causes");
        return repository.findAll();
    }

    @Cacheable("rootcauses")
    public RootCause getById(UUID id) {
        log.info("Get root cause by id '{}'", id);
        return get(id);
    }

    /**
     * Gets root cause by id or returns null.
     * @param id root cause id
     * @return root cause
     */
    public RootCause getByIdOrNull(UUID id) {
        return repository().findById(id).orElse(null);
    }

    /**
     * Create root cause.
     *
     * @param request creation request
     * @return created root cause
     */
    @CacheEvict(value = "rootcauses", allEntries = true)
    public RootCause create(@Valid RootCauseUpsertRequest request) {
        log.info("Create root cause with content: {}", request);
        RootCause savedRootCause = modelMapper.map(request, RootCause.class);

        validateRootCauseForUserRights(savedRootCause);
        validateRootCauseNameUniqueness(savedRootCause);

        return repository.save(savedRootCause);
    }

    /**
     * Update root cause.
     *
     * @param id      root cause identifier
     * @param request root cause update request
     * @return updated request
     */
    @CacheEvict(value = "rootcauses", allEntries = true)
    public RootCause update(UUID id, @Valid RootCauseUpsertRequest request) {
        log.info("Update root cause '{}' with content: {}", id, request);

        RootCause existedRootCause = get(id);
        modelMapper.map(request, existedRootCause);

        validateRootCauseForUserRights(existedRootCause);
        validateRootCauseNameUniqueness(existedRootCause);

        return repository.save(existedRootCause);
    }

    private void validateRootCauseForUserRights(RootCause rootCause) {
        boolean isGlobalRootCause = RootCauseType.GLOBAL.equals(rootCause.getType());
        boolean nonAdmin = !SecurityUtils.isCurrentUserAdmin();
        if (isGlobalRootCause && nonAdmin) {
            ExceptionUtils.throwWithLog(log, new RamRootCauseIllegalAccessException());
        }
    }

    private void validateRootCauseNameUniqueness(RootCause savedRootCause) {
        final RootCauseType type = savedRootCause.getType();
        final String name = savedRootCause.getName();
        final UUID projectId = savedRootCause.getProjectId();
        final UUID id = savedRootCause.getUuid();

        RootCause existedRootCause;

        switch (type) {
            case CUSTOM:
                existedRootCause = repository.findByNameAndProjectId(name, projectId);
                break;

            case GLOBAL:
                existedRootCause = repository.findByNameAndType(name, RootCauseType.GLOBAL);
                break;

            default:
                log.error("Found illegal root cause type: {}", type);
                throw new RamRootCauseIllegalTypeException(type);
        }

        if (nonNull(existedRootCause)) {
            final boolean isSameNames = name.equals(existedRootCause.getName());

            final boolean isNotUpdatedRootCause = nonNull(id) && !id.equals(existedRootCause.getUuid());
            if (isSameNames && (isNull(id) || isNotUpdatedRootCause)) {
                log.error("Global root cause with provided name '{}' already exists", name);
                throw new RamRootCauseAlreadyExistsException();
            }
        }
    }

    /**
     * Delete root cause by id.
     *
     * @param id root cause identifier
     */
    @CacheEvict(value = "rootcauses", allEntries = true)
    public void deleteById(UUID id) {
        log.info("Delete root cause by id '{}'", id);
        RootCause rootCause = get(id);

        deleteRootCauseHierarchy(rootCause);
    }

    /**
     * Delete all parent root cause sub nodes.
     *
     * @param rootCause deleted parent root cause
     */
    public void deleteRootCauseHierarchy(RootCause rootCause) {
        final UUID rootCauseId = rootCause.getUuid();
        log.info("Start hierarchical deletion for parent root cause with id '{}'", rootCauseId);

        List<RootCause> children = getChildrenRootCauses(rootCause, null);

        if (!isEmpty(children)) {
            children.forEach(this::deleteRootCauseHierarchy);
        }

        log.debug("Delete root cause with id '{}'", rootCauseId);
        testRunService.unsetRootCauseForLinkedTestRuns(rootCauseId);
        repository.delete(rootCause);
    }

    @Cacheable("rootcauses")
    public List<RootCause> getByIds(Collection<UUID> ids) {
        log.info("Get root causes by ids [{}]", ids);
        return repository.findByUuidIn(ids);
    }

    /**
     * Get root cause tree by project id.
     *
     * @param projectId project identifier
     * @param filterDisabled exclude disabled root cause nodes
     * @return result tree
     */
    @Cacheable("rootcauses")
    public List<RootCauseTreeNode> getRootCauseTree(UUID projectId, boolean filterDisabled) {
        log.info("Get root cause tree for project '{}'", projectId);
        List<RootCause> globalRootCauses = getAllGlobalTopLevelRootCauses();
        List<RootCause> projectCustomRootCauses = getAllTopLevelCustomProjectRootCauses(projectId);

        List<RootCause> allTopLevelRootCauses = new ArrayList<>();
        allTopLevelRootCauses.addAll(globalRootCauses);
        allTopLevelRootCauses.addAll(projectCustomRootCauses);

        return getTreeByTopLevelRootCauses(allTopLevelRootCauses, projectId, filterDisabled);
    }

    /**
     * Get root cause name by id.
     *
     * @param id root cause identifier
     * @return name
     */
    public String getRootCauseNameById(UUID id) {
        if (nonNull(id)) {
            return repository.findNameByUuid(id).getName();
        }
        return "";
    }

    private List<RootCauseTreeNode> getTreeByTopLevelRootCauses(List<RootCause> topLevelRootCauses, UUID projectId,
                                                                boolean filterDisabled) {
        return topLevelRootCauses.stream()
                .filter(rootCause -> !filterDisabled || !rootCause.isDisabled())
                .map(rootCause -> getTreeNode(rootCause, projectId, filterDisabled))
                .collect(Collectors.toList());
    }

    private RootCauseTreeNode getTreeNode(RootCause rootCause, UUID projectId, boolean filterDisabled) {
        log.debug("Get tree node for root cause: {}", rootCause);
        RootCauseTreeNode parentNode = new RootCauseTreeNode(rootCause);

        List<RootCause> children = getChildrenRootCauses(rootCause, projectId);

        if (!isEmpty(children)) {
            List<TreeNode<RootCause>> childrenNodes = children.stream()
                    .filter(childRootCause -> !filterDisabled || !childRootCause.isDisabled())
                    .map(childRootCause -> getTreeNode(childRootCause, projectId, filterDisabled))
                    .collect(Collectors.toList());

            parentNode.setChildren(childrenNodes);
        }

        return parentNode;
    }

    /**
     * Collects children root causes by parent root cause and project id.
     * @param parentRootCause parent root cause
     * @param projectId project id
     * @return list of child root causes
     */
    public List<RootCause> getChildrenRootCauses(RootCause parentRootCause, UUID projectId) {
        log.debug("Get children root causes for parent root cause '{}'", parentRootCause);
        final UUID parentRootCauseId = parentRootCause.getUuid();
        final RootCauseType rootCauseType = parentRootCause.getType();

        switch (rootCauseType) {
            case CUSTOM:
                return repository.findAllByParentIdAndProjectId(parentRootCauseId, projectId);

            case GLOBAL:
                return repository.findAllByParentId(parentRootCauseId);

            default:
                return Collections.emptyList();
        }
    }

    /**
     * Checks if root cause already exists in target project.
     * @param rootCause root cause to check
     * @param targetProjectId target project id
     * @return true if exists
     */
    public boolean isNameUsed(RootCause rootCause, UUID targetProjectId) {
        List<RootCause> fromBase;
        if (rootCause.getProjectId() == null) {
            fromBase = repository.findAllByNameAndParentId(rootCause.getName(), rootCause.getParentId());
        } else {
            if (targetProjectId.equals(rootCause.getProjectId())) {
                fromBase = repository.findAllByProjectIdAndNameAndParentId(rootCause.getProjectId(),
                        rootCause.getName(), rootCause.getParentId());
            } else {
                fromBase = repository.findAllByNameAndParentId(rootCause.getName(), rootCause.getParentId());
            }
        }
        return !isEmpty(fromBase)
                && fromBase.stream().noneMatch(base -> base.getUuid().equals(rootCause.getUuid()));
    }

    private List<RootCause> getAllTopLevelCustomProjectRootCauses(UUID projectId) {
        log.debug("Getting all project custom top level root causes");

        final List<RootCause> rootCauses =
                repository.findAllByParentIdIsNullAndProjectIdAndType(projectId, RootCauseType.CUSTOM);
        log.debug("Found root causes: [{}]", extractIds(rootCauses));

        return rootCauses;
    }

    private List<RootCause> getAllGlobalTopLevelRootCauses() {
        log.debug("Getting all global root causes");

        final List<RootCause> rootCauses = repository.findAllByParentIdIsNullAndType(RootCauseType.GLOBAL);
        log.debug("Found root causes: [{}]", extractIds(rootCauses));

        return rootCauses;
    }


    /**
     * Disable root cause by specified identifier.
     *
     * @param id root cause identifier
     */
    @CacheEvict(value = "rootcauses", allEntries = true)
    public void disable(UUID id) {
        updateDisableStatus(id, true);
        log.debug("Root cause with id '{}' has been disabled", id);
    }

    /**
     * Enable root cause by specified identifier.
     *
     * @param id root cause identifier
     */
    @CacheEvict(value = "rootcauses", allEntries = true)
    public void enable(UUID id) {
        log.info("Enable root cause with id '{}'", id);
        updateDisableStatus(id, false);
        log.debug("Root cause with id '{}' has been disabled", id);
    }

    /**
     * Update root cause disabled status.
     *
     * @param id root cause identifier
     * @param status new value of the status
     */
    private void updateDisableStatus(UUID id, boolean status) {
        RootCause rootCause = get(id);

        rootCause.setDisabled(status);

        repository.save(rootCause);
    }

    /**
     * Get root cause list by parent id.
     *
     * @param parentId parent root cause identifier
     * @return name
     */
    public List<RootCause> getRootCausesByParentId(UUID parentId) {
        if (parentId == null) {
            return Collections.emptyList();
        }
        log.debug("Getting root causes by parentId {}", parentId);
        List<RootCause> foundRootCauses = repository.findAllByParentId(parentId);
        log.debug("Found root causes: [{}]", extractIds(foundRootCauses));
        return foundRootCauses;
    }

    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }
}
