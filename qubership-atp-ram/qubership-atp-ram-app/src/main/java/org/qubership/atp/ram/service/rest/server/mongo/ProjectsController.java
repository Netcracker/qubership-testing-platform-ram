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

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.services.ProjectsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@CrossOrigin
@RequestMapping("/api/projects")
@RestController()
@RequiredArgsConstructor
public class ProjectsController /*implements ProjectsControllerApi*/ {

    private final ProjectsService service;

    /**
     * Returns list of all Projects.
     */
    @Deprecated
    @GetMapping
    @PreAuthorize("@entityAccess.isAdmin()")
    public List<Project> getAll() {
        return service.getAll();
    }

    /**
     * Returns project by projectUuid.
     */
    @GetMapping(value = "/{projectId}")
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid,'READ')")
    public Project getProjectByUuid(@PathVariable("projectId") UUID projectUuid) {
        return service.getProjectById(projectUuid);
    }

    /**
     * Save project.
     *
     * @deprecated For creating use TestRunLoggingController#findOrCreateWithParents while logging of steps
     */
    @Deprecated
    @PutMapping(value = "/save", produces = TEXT_PLAIN_VALUE)
    @PreAuthorize("@entityAccess.checkAccess(#project.getUuid(),'UPDATE')")
    public UUID saveProject(@RequestBody Project project) {
        service.save(project);
        return project.getUuid();
    }

    /**
     * Create project.
     *
     * @deprecated For creating use TestRunLoggingController#findOrCreateWithParents while logging of steps
     */
    @Deprecated
    @PostMapping(value = "/create")
    @PreAuthorize("@entityAccess.checkAccess(#project.getUuid(),'CREATE')")
    public ResponseEntity createProject(@RequestBody Project project) {
        return new ResponseEntity<>(service.save(project), HttpStatus.CREATED);
    }

    @GetMapping(value = "/{projectId}/name", produces = TEXT_PLAIN_VALUE)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid,'READ')")
    public String getProjectName(@PathVariable("projectId") UUID projectUuid) {
        return service.getProjectName(projectUuid);
    }
}
