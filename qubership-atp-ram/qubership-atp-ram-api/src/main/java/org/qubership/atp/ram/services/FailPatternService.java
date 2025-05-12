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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.dto.response.FailPatternPageResponse;
import org.qubership.atp.ram.dto.response.FailPatternResponse;
import org.qubership.atp.ram.dto.response.PaginationResponse;
import org.qubership.atp.ram.model.FailPatternCheckRequest;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.models.FailPatternSearchRequest;
import org.qubership.atp.ram.models.JiraTicket;
import org.qubership.atp.ram.models.PaginationSearchRequest;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.repositories.CustomFailPatternRepository;
import org.qubership.atp.ram.repositories.FailPatternRepository;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FailPatternService extends CrudService<FailPattern> {

    private final ProjectsService projectsService;
    private final RootCauseService rootCauseService;
    private final FailPatternRepository repository;
    private final CustomFailPatternRepository customRepository;
    private final ModelMapper modelMapper;

    @Override
    protected MongoRepository<FailPattern, UUID> repository() {
        return repository;
    }

    /**
     * Update or insert Fail Pattern.
     */
    public FailPattern upsertFailPattern(FailPattern failPattern) {
        setJiraDefectCreationDates(failPattern);
        return save(failPattern);
    }

    private void setJiraDefectCreationDates(FailPattern failPattern) {
        List<JiraTicket> jiraDefects = failPattern.getJiraDefects();
        if (!isEmpty(jiraDefects)) {
            final Timestamp currentDate = Timestamp.valueOf(LocalDateTime.now());
            jiraDefects.stream()
                    .filter(jiraTicket -> isNull(jiraTicket.getCreatedDate()))
                    .forEach(jiraTicket -> jiraTicket.setCreatedDate(currentDate));
        }
    }

    /**
     * Returns fail patterns by projectUuid.
     */
    public List<FailPattern> getFailPatternsByIds(Collection<UUID> ids) {
        return repository.findByUuidIn(ids);
    }

    /**
     * Search fail patterns.
     */
    public FailPatternPageResponse search(FailPatternSearchRequest request, Pageable pageable) {
        log.info("Searching Fail Patterns with request '{}'", request);
        final PaginationResponse<FailPattern> response = customRepository.findAllFailPatterns(request, pageable);

        final List<FailPatternResponse> failPatternResponses = getResponses(response.getEntities());
        log.debug("Found Fail Patterns: {}", StreamUtils.extractIds(failPatternResponses));

        return new FailPatternPageResponse(response.getTotalCount(), failPatternResponses);
    }

    /**
     * Find a fail pattern.
     *
     * @param id fail pattern id
     * @return Fail Pattern
     */
    public FailPatternResponse getById(UUID id) {
        return getResponse(repository.findByUuid(id));
    }

    /**
     * Deletes fail pattern by failPatternId from fail pattern mongo collection.
     * @param failPatternId failPatternId
     */
    public void deleteByUuid(UUID failPatternId) {
        log.info("deleting Fail Pattern {}", failPatternId);
        repository.deleteByUuid(failPatternId);
    }

    /**
     * Find Fail Patterns in a project, sort and limit results.
     *
     * @param projectId  for find ER-s
     * @param startIndex number of start element
     * @param endIndex   number of finish element
     * @return sorted Fail Patterns in a project
     */
    public FailPatternPageResponse findPageByProjectIdAndSort(
            UUID projectId, int startIndex, int endIndex) {
        log.info("Finding Fail Patterns in project {} from {} to {}", projectId, startIndex, endIndex);
        int countOnPage = endIndex - startIndex;
        int numOfPage = startIndex / countOnPage;
        PageRequest request = PageRequest.of(numOfPage, countOnPage);
        List<FailPattern> failPatterns = repository.findAllByProjectId(projectId, request);
        List<FailPatternResponse> failPatternResponses = getResponses(failPatterns);
        return new FailPatternPageResponse((int) repository.countByProjectId(projectId), failPatternResponses);
    }

    /**
     * Build FailPatternResponse dto's from fail patterns.
     *
     * @param failPatterns fail patterns
     * @return list of fail pattern dto's
     */
    public List<FailPatternResponse> getResponses(List<FailPattern> failPatterns) {
        final Set<UUID> failPatternProjectIds = StreamUtils.extractIds(failPatterns, FailPattern::getProjectId);
        final Set<UUID> failPatternFailReasonIds = StreamUtils.extractIds(failPatterns, FailPattern::getFailReasonId);

        long durationOfExecute = System.currentTimeMillis();

        final List<Project> projects = projectsService.getProjectsByIds(failPatternProjectIds);
        final Map<UUID, String> projectMap = StreamUtils.toIdNameEntityMap(projects);

        final List<RootCause> rootCauses = rootCauseService.getByIds(failPatternFailReasonIds);
        final Map<UUID, String> rootCauseMap = StreamUtils.toIdNameEntityMap(rootCauses);;

        List<FailPatternResponse> failPatternResponses = failPatterns.stream()
                .map(failPattern -> {
                    final String projectName = projectMap.get(failPattern.getProjectId());
                    final String rootCauseName = rootCauseMap.get(failPattern.getFailReasonId());

                    return getResponse(failPattern, projectName, rootCauseName);
                })
                .collect(Collectors.toList());

        long maxDurationOfExecute = 1000;
        if (System.currentTimeMillis() - durationOfExecute > maxDurationOfExecute) {
            log.warn("getResponses: long execute collect of fail patterns. Map of project: {}", projectMap.keySet());
        }

        return failPatternResponses;
    }

    /**
     * Build FailPattern dto from FailPattern.
     *
     * @param failPattern FailPattern
     * @return FailPattern dto
     */
    public FailPatternResponse getResponse(FailPattern failPattern) {
        Project project = projectsService.getProjectById(failPattern.getProjectId());
        String rootCauseName = failPattern.getFailReasonId() != null
                ? rootCauseService.get(failPattern.getFailReasonId()).getName()
                : null;
        return getResponse(failPattern, project.getName(), rootCauseName);
    }

    private FailPatternResponse getResponse(FailPattern failPattern, String projectName, String rootCauseName) {
        FailPatternResponse failPatternResponse = modelMapper.map(failPattern, FailPatternResponse.class);
        if (failPattern.getProjectId() != null) {
            failPatternResponse.getProject().setName(projectName);
        }
        if (failPattern.getFailReasonId() != null) {
            failPatternResponse.getFailReason().setName(rootCauseName);
        }
        return failPatternResponse;
    }

    /**
     * Find Fail Patterns in a project.
     *
     * @param projectId for find ER-s
     * @return Fail Patterns in a project
     */
    public List<FailPattern> findPatternByProjectId(
            UUID projectId) {
        log.info("Finding Fail Patterns in project {}", projectId);
        return repository.findAllByProjectId(projectId);
    }

    /**
     * Check correctness of fail pattern.
     *
     * @param failPatternCheckRequest FailPatternCheckRequest
     * @return http status of check operation
     */
    public HttpStatus check(FailPatternCheckRequest failPatternCheckRequest) {
        Pattern compiledFailedPattern;
        HttpStatus httpStatus;
        try {
            compiledFailedPattern = Pattern.compile(failPatternCheckRequest.getRule());
        } catch (PatternSyntaxException e) {
            log.error("Could not parse pattern with rule {}, exception {}",
                    failPatternCheckRequest.getRule(), e.getMessage());
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            return httpStatus;
        }

        try {
            if (compiledFailedPattern != null
                    && compiledFailedPattern.matcher(failPatternCheckRequest.getMessage()).find()) {
                httpStatus = HttpStatus.OK;
            } else {
                httpStatus = HttpStatus.NOT_FOUND;
            }
        } catch (StackOverflowError e) {
            log.error("StackOverflowError for rule {}, exception {}",
                    failPatternCheckRequest.getRule(), e.getMessage());
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return httpStatus;
    }

    public UUID getProjectIdByFailPatternId(UUID failPatternId) {
        FailPattern failPattern = repository.findProjectByUuid(failPatternId);
        return failPattern.getProjectId();
    }

    /**
     * Find all fail patterns by name with pagination.
     *
     * @param request pagination search request
     * @return page of fail patterns
     */
    public Page<FailPattern> getAllNamesByNameWithPagination(PaginationSearchRequest request) {
        log.info("Finding Fail Patterns by request '{}'", request);
        final Set<UUID> projectIds = request.getProjects();
        final String name = request.getName();
        final String sort = nonNull(request.getSort()) ? request.getSort() : "name";
        final Sort.Direction direction =  nonNull(request.getDirection()) ? request.getDirection() : Sort.Direction.ASC;

        final Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), direction, sort);
        log.debug("Pageable: {}", pageable);

        if (isEmpty(projectIds)) {
            log.debug("Finding Fail Patterns by name '{}'", name);
            return repository.findAllNamesByNameContainsIgnoreCase(name, pageable);
        }

        log.debug("Finding Fail Patterns by name '{}' in projects '{}'", name, projectIds);
        return repository.findAllNamesByProjectIdInAndNameContainsIgnoreCase(projectIds, name, pageable);
    }

    /**
     * Find all fail pattern issues by name with pagination.
     *
     * @param request pagination search request
     * @return page of issues
     */
    public PaginationResponse getAllIssuesWithPagination(PaginationSearchRequest request) {
        log.info("Finding issues by request '{}'", request);
        return customRepository.getAllIssuesWithPagination(request);
    }

    /**
     * Find all fail reasons by name with pagination.
     *
     * @param request pagination search request
     * @return page of fail reasons
     */
    public PaginationResponse getAllFailReasonsWithPagination(PaginationSearchRequest request) {
        log.info("Finding fail reasons by request '{}'", request);
        return customRepository.getAllFailReasonsWithPagination(request);
    }
}
