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

package org.qubership.atp.ram.tsg.controller;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.services.ProjectsService;
import org.qubership.atp.ram.tsg.model.TsgConfiguration;
import org.qubership.atp.ram.tsg.service.TsgProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonObject;

@RequestMapping("/api/tsg-projects")
@RestController()
public class TsgProjectsController /*implements TsgProjectsControllerApi*/ {

    private static final Logger LOG = LoggerFactory.getLogger(TsgProjectsController.class);
    private final ProjectsService service;
    private final TsgProjectService tsgProjectService;
    private final TsgConfiguration tsgConfiguration;

    /**
     * Constructor.
     */
    @Autowired
    public TsgProjectsController(ProjectsService service,
                                 TsgProjectService tsgProjectService,
                                 TsgConfiguration tsgConfiguration) {
        this.service = service;
        this.tsgProjectService = tsgProjectService;
        this.tsgConfiguration = tsgConfiguration;
    }

    /**
     * Returns ProjectsController status.
     */
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    /**
     * Returns all tsgProjects.
     */
    @GetMapping()
    public List<Project> getAllTsgProjects() {
        return tsgProjectService.getAllTsgProjects();
    }

    /**
     * Update tsgProjectName parameter.
     */
    @PutMapping("/{projectUuid}")
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid,'UPDATE')")
    public Project setTsgProjectName(@PathVariable("projectUuid") UUID projectUuid, @RequestBody JsonObject
            tsgParameters) {
        return tsgProjectService.updateTsgParameters(projectUuid, tsgParameters);
    }

    /**
     * Updates Tsg Integration Parameters.
     */
    @PutMapping("/settings/put")
    public TsgConfiguration updateSettings(@RequestBody TsgConfiguration tsgConfiguration) {
        this.tsgConfiguration.setTsgReceiverUrl(tsgConfiguration.getTsgReceiverUrl());
        this.tsgConfiguration.setStatuses(tsgConfiguration.getStatuses());
        LOG.debug("TSG Configuration was changed: {}", this.tsgConfiguration);
        return this.tsgConfiguration;
    }

    /**
     * Returns current TsgConfiguration.
     */
    @GetMapping("/settings/get")
    public TsgConfiguration getSettings() {
        return this.tsgConfiguration;
    }
}

