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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.ram.enums.SystemStatus;
import org.qubership.atp.ram.model.GridFsFileData;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.SsmMetricReports;
import org.qubership.atp.ram.models.SystemInfo;
import org.qubership.atp.ram.models.ToolsInfo;
import org.qubership.atp.ram.repositories.EnvironmentsInfoRepository;
import org.qubership.atp.ram.repositories.ToolsInfoRepository;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnvironmentsInfoService extends CrudService<EnvironmentsInfo> {

    private final EnvironmentsInfoRepository repository;
    private final ToolsInfoRepository toolsInfoRepository;
    private final GridFsService gridFsService;

    @Lazy
    private final MongoTemplate mongoTemplate;

    @Override
    protected MongoRepository<EnvironmentsInfo, UUID> repository() {
        return repository;
    }

    public EnvironmentsInfo findByUuid(UUID uuid) {
        return this.repository.findByUuid(uuid);
    }

    public List<EnvironmentsInfo> findByRequestIds(Collection<UUID> requestIds) {
        return this.repository.findByExecutionRequestIdIn(requestIds);
    }

    /**
     * Returns environments info for ExecutionRequestTestResult by provided list of execution request uuid.
     */
    public Map<UUID, EnvironmentsInfo> getDataForErTestResultByExecutionRequestIds(Set<UUID> erIds) {
        List<EnvironmentsInfo> environmentsInfos = repository.findByExecutionRequestIdInForErTestResult(erIds);

        return StreamUtils.toIdEntityMap(environmentsInfos,
                EnvironmentsInfo::getExecutionRequestId);
    }

    /**
     * Returns all environments info by provided execution request uuid.
     */
    public EnvironmentsInfo findByExecutionRequestId(UUID executionRequestId) throws AtpEntityNotFoundException {
        EnvironmentsInfo envInfo = repository.findByExecutionRequestId(executionRequestId);
        if (isNull(envInfo)) {
            log.error("Failed to find Environment Info by execution request id: {}", executionRequestId);
            throw new AtpEntityNotFoundException("Environment Info", "execution request id", executionRequestId);
        }
        UUID toolsInfoId = envInfo.getToolsInfoUuid();

        if (nonNull(toolsInfoId)) {
            log.debug("Getting tools info by id: {}", toolsInfoId);
            ToolsInfo toolsInfo = toolsInfoRepository.findByUuid(toolsInfoId);
            log.debug("Founded tools info: {}", toolsInfo);
            envInfo.setToolsInfo(toolsInfo);
        }
        envInfo.setStatus(calculateStatus(envInfo).name());
        return envInfo;
    }

    public GridFsFileData getReportById(UUID reportId) throws FileNotFoundException {
        return gridFsService.getReportById(reportId);
    }

    /**
     * Save mandatory checks report.
     *
     * @param executionRequestId execution request identifier
     * @param fileInputStream    file input stream
     * @param fileName           file name
     * @return report identifier
     */
    public UUID saveMandatoryChecksReport(UUID executionRequestId, InputStream fileInputStream, String fileName) {
        UUID reportId = UUID.randomUUID();
        gridFsService.saveMandatoryChecksReport(reportId, executionRequestId, fileInputStream, fileName);

        return reportId;
    }

    /**
     * Upload SSM metrics report file into GridFS storage.
     *
     * @param fileName    file name
     * @param type        file type
     * @param contentType file content type
     * @param inputStream input stream
     * @param logRecordId log record identifier
     * @param erId        execution request identifier
     * @return uploaded report file identifier
     */
    public UUID saveSsmMetricsReport(String fileName, String type, String contentType, InputStream inputStream,
                                     UUID erId, UUID logRecordId) {
        log.debug("Uploading SSM metrics report file. Params: fileName='{}', type='{}', contentType='{}', "
                + "execution request id='{}', log record if='{}'", fileName, type, contentType, erId, logRecordId);
        return gridFsService.saveSsmMetricReport(fileName, type, contentType, inputStream, erId, logRecordId);
    }

    /**
     * Update SSM metric reports data.
     *
     * @param executionRequestId execution request identifier
     * @param data               SSM metric reports data
     */
    public void updateSsmMetricReportsData(UUID executionRequestId, SsmMetricReports data) {
        log.info("Updating environments info SSM metric reports data for ER with id '{}'", executionRequestId);
        log.info("Updates: {}", data);

        EnvironmentsInfo environmentsInfo = findByExecutionRequestId(executionRequestId);
        log.debug("Found environments info with id '{}'", environmentsInfo.getUuid());

        SsmMetricReports ssmMetricReports = environmentsInfo.getSsmMetricReports();
        log.debug("SSM metric reports data: {}", ssmMetricReports);

        if (isNull(ssmMetricReports)) {
            ssmMetricReports = new SsmMetricReports();
        }

        final UUID microservicesReportId = data.getMicroservicesReportId();
        if (nonNull(microservicesReportId)) {
            ssmMetricReports.setMicroservicesReportId(microservicesReportId);
        }

        final UUID problemContextReportId = data.getProblemContextReportId();
        if (nonNull(problemContextReportId)) {
            ssmMetricReports.setProblemContextReportId(problemContextReportId);
        }

        environmentsInfo.setSsmMetricReports(ssmMetricReports);

        log.debug("Updated SSM metric reports data: {}", ssmMetricReports);
        save(environmentsInfo);

        log.info("Environments info has been successfully updated");
    }

    private SystemStatus calculateStatus(EnvironmentsInfo environmentsInfo) {
        List<SystemInfo> allSystems = new ArrayList<>(environmentsInfo.getTaSystemInfoList());
        allSystems.addAll(environmentsInfo.getQaSystemInfoList());
        return calculateStatus(allSystems);
    }

    SystemStatus calculateStatus(List<SystemInfo> systemInfos) {
        if (systemInfos.stream().anyMatch(systemInfo -> checkStatus(systemInfo, SystemStatus.FAIL))) {
            return SystemStatus.FAIL;
        }
        if (systemInfos.stream().anyMatch(systemInfo -> checkStatus(systemInfo, SystemStatus.WARN))) {
            return SystemStatus.WARN;
        }
        return SystemStatus.PASS;
    }

    private boolean checkStatus(SystemInfo info, SystemStatus systemStatus) {
        return info.getStatus() == systemStatus;
    }

    /**
     * Find QA TA systems by execution request identifier.
     * @param executionRequestId execution request identifier
     * @return found systems
     */
    public EnvironmentsInfo findQaTaSystemsByExecutionRequestId(UUID executionRequestId) {
        EnvironmentsInfo environmentsInfo = repository.findQaTaSystemsByExecutionRequestId(executionRequestId);
        if (isNull(environmentsInfo)) {
            log.error("Failed to find Environment Info by execution request id: {}", executionRequestId);
            throw new AtpEntityNotFoundException("Environment Info", "execution request id", executionRequestId);
        }
        return environmentsInfo;
    }

    /**
     * Deleted expired Env info.
     */
    public void deleteAllEnvironmentsInfoByExecutionRequestId(List<UUID> executionRequestIds) {
        repository.deleteAllByExecutionRequestIdIn(executionRequestIds);
    }

    /**
     * Deleted toolsInfo by executions requests ids.
     */
    public void deleteAllToolsByExecutionRequestId(List<UUID> executionRequestIds) {
        toolsInfoRepository.deleteAllByExecutionRequestIdIn(executionRequestIds);
    }
}
