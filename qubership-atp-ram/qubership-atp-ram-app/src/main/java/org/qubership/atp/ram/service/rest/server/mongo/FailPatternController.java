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

package org.qubership.atp.ram.service.rest.server.mongo;

import java.util.UUID;

import org.qubership.atp.ram.dto.response.FailPatternPageResponse;
import org.qubership.atp.ram.dto.response.FailPatternResponse;
import org.qubership.atp.ram.dto.response.PaginationResponse;
import org.qubership.atp.ram.model.FailPatternCheckRequest;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.models.FailPatternSearchRequest;
import org.qubership.atp.ram.models.PaginationSearchRequest;
import org.qubership.atp.ram.service.history.ConcurrentModificationService;
import org.qubership.atp.ram.services.FailPatternService;
import org.qubership.atp.ram.services.IssueService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/api/failpatterns")
@RestController()
@RequiredArgsConstructor
@Slf4j
public class FailPatternController /*implements FailPatternControllerApi*/ {

    private static final String X_PROJECT_ID = "X-Project-Id";

    private final FailPatternService failPatternService;
    private final IssueService issueService;
    private final ConcurrentModificationService concurrentModificationService;

    /**
     * Returns a fail pattern by id.
     */
    @GetMapping(value = "/{id}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).FAIL_PATTERN.getName(),"
            + "@failPatternService.getProjectIdByFailPatternId(#id),'READ')")
    public FailPatternResponse getById(@PathVariable("id") UUID id) {
        return failPatternService.getById(id);
    }

    /**
     * Search fail patterns.
     */
    @PostMapping("/search")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).FAIL_PATTERN.getName(),"
            + "#projectId,'READ')")
    public FailPatternPageResponse search(@RequestBody FailPatternSearchRequest request,
                                          @RequestHeader(X_PROJECT_ID) String projectId,
                                          Pageable pageable) {
        return failPatternService.search(request, pageable);
    }

    /**
     * Returns list all fail patterns in a project.
     */
    @GetMapping(value = "/project/{projectId}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).FAIL_PATTERN.getName(),"
            + "#projectId,'READ')")
    public FailPatternPageResponse getAllByProject(@PathVariable("projectId") UUID projectId,
                                                   @RequestParam("startIndex") int startIndex,
                                                   @RequestParam("endIndex") int endIndex) {
        return failPatternService.findPageByProjectIdAndSort(projectId, startIndex, endIndex);
    }

    /**
     * Create a new fail pattern.
     */
    @PostMapping()
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).FAIL_PATTERN.getName(),"
            + "#failPattern.getProjectId(),'CREATE')")
    public FailPattern create(@RequestBody FailPattern failPattern) {
        return failPatternService.upsertFailPattern(failPattern);
    }

    @DeleteMapping(value = "/{id}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).FAIL_PATTERN.getName(),"
            + "@failPatternService.get(#id).getProjectId(),'DELETE')")
    public void delete(@PathVariable("id") UUID id) {
        issueService.deleteFailPattern(id);
    }


    /**
     * Update existing fail pattern.
     */
    @PutMapping()
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).FAIL_PATTERN.getName(),"
            + "#failPattern.getProjectId(),'UPDATE')")
    public ResponseEntity<FailPattern> save(@RequestBody FailPattern failPattern) {
        HttpStatus status =
                concurrentModificationService.getConcurrentModificationHttpStatus(failPattern.getUuid(),
                failPattern.getModifiedWhen(), failPatternService);
        return ResponseEntity.status(status).body(failPatternService.upsertFailPattern(failPattern));
    }

    @PutMapping(value = "/check")
    public HttpStatus check(@RequestBody FailPatternCheckRequest failPatternCheckRequest) {
        return failPatternService.check(failPatternCheckRequest);
    }

    /**
     * Returns list fail pattern names with a search and pagination.
     */
    @PostMapping(value = "/find-name-pagination")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).FAIL_PATTERN.getName(),"
            + "#projectId,'READ')")
    public Page<FailPattern> getAllNamesWithPagination(@RequestBody PaginationSearchRequest request,
                                                       @RequestHeader(X_PROJECT_ID) String projectId) {
        log.info("Get all fail patterns by name with pagination");
        return failPatternService.getAllNamesByNameWithPagination(request);
    }

    /**
     * Returns list fail pattern issues with a search and pagination.
     */
    @PostMapping(value = "/issues/find-name-pagination")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).FAIL_PATTERN.getName(),"
            + "#projectId,'READ')")
    public PaginationResponse getAllIssuesWithPagination(@RequestBody PaginationSearchRequest request,
                                                         @RequestHeader(X_PROJECT_ID) String projectId) {
        log.info("Get all issues by name with pagination");
        return failPatternService.getAllIssuesWithPagination(request);
    }

    /**
     * Returns list fail reasons with a search and pagination.
     */
    @PostMapping(value = "/fail-reasons/find-name-pagination")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.ram.enums.UserManagementEntities).FAIL_PATTERN.getName(),"
            + "#projectId,'READ')")
    public PaginationResponse getAllFailReasonsWithPagination(@RequestBody PaginationSearchRequest request,
                                                              @RequestHeader(X_PROJECT_ID) String projectId) {
        log.info("Get all fail reasons by name with pagination");
        return failPatternService.getAllFailReasonsWithPagination(request);
    }
}
