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

import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.tsg.model.TsgFdr;
import org.qubership.atp.ram.tsg.senders.Sender;
import org.qubership.atp.ram.tsg.service.TsgService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonArray;

@RestController
@RequestMapping("/api/tsg-exchanger")
public class FdrController /*implements FdrControllerApi*/ {

    private static final Logger LOG = LoggerFactory.getLogger(FdrController.class);

    private final TsgService service;
    private final Sender<List<UUID>> fdrSender;

    @Autowired
    public FdrController(TsgService service, Sender<List<UUID>> fdrSender) {
        this.service = service;
        this.fdrSender = fdrSender;
    }

    /**
     * Ping.
     */
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("@entityAccess.checkAccess(@executionRequestService.getProjectIdByExecutionRequestId(#uuid),'READ')")
    public List<TsgFdr> getByErUuid(@PathVariable("uuid") UUID uuid) {
        LOG.info("Get FDR for ER: {}", uuid);
        return service.buildAllFdrsForEr(uuid);
    }

    @GetMapping("/{executionRequestUuid}/{testRunUuid}")
    @PreAuthorize("@entityAccess.checkAccess(@executionRequestService.getProjectIdByExecutionRequestId("
            + "#executionRequestUuid),'READ')")
    public TsgFdr getFdrForTestRun(@PathVariable("executionRequestUuid") UUID executionRequestUuid,
                                   @PathVariable("testRunUuid") UUID testRunUuid) {
        LOG.debug("Get FDRs for TR:{}, ER: {}", testRunUuid, executionRequestUuid);
        return service.buildFdr(testRunUuid);
    }

    @PostMapping("/fdr")
    @PreAuthorize("@entityAccess.checkAccess(@testRunService.getProjectIdByTestRunId("
            + "#uuidList.get(0)),'READ')")
    public void sendToTsg(@RequestBody List<UUID> uuidList) {
        LOG.info("Start sending FDRs to TSG.");
        fdrSender.send(uuidList);
    }

    @GetMapping("/fill")
    public void fillNotFinishedErs() {
        service.checkNotFinishedErs();
    }

    @GetMapping("/check")
    public void checkFinishedErs() {
        service.checkFinishedErs();
    }

    @GetMapping("/getErs")
    public List<ExecutionRequest> getErs() {
        return service.getAllRequestInQueue();
    }

    @GetMapping("/dailyInfo/{daysFilter}")
    public JsonArray getDailyInfo(@PathVariable("daysFilter") int daysFilter) {
        return service.getDailyInfo(daysFilter);
    }

    @GetMapping("/report")
    public String getReport(@RequestParam("days") int daysFilter) {
        return service.getHtmlReport(daysFilter);
    }
}

