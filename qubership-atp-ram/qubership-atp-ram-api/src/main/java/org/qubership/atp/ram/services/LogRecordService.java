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
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpIllegalNullableArgumentException;
import org.qubership.atp.auth.springbootstarter.utils.ExceptionUtils;
import org.qubership.atp.ram.RamConstants;
import org.qubership.atp.ram.dto.request.LogRecordQuantityMatchPatternRequest;
import org.qubership.atp.ram.dto.request.UpdateLogRecordContextVariablesRequest;
import org.qubership.atp.ram.dto.request.UpdateLogRecordExecutionStatusRequest;
import org.qubership.atp.ram.dto.request.UpdateLogRecordMessageParametersRequest;
import org.qubership.atp.ram.dto.response.ContextVariablesResponse;
import org.qubership.atp.ram.dto.response.LocationInEditorResponse;
import org.qubership.atp.ram.dto.response.LogRecordPreviewResponse;
import org.qubership.atp.ram.dto.response.LogRecordQuantityResponse;
import org.qubership.atp.ram.dto.response.LogRecordResponse;
import org.qubership.atp.ram.dto.response.LogRecordScreenshotResponse;
import org.qubership.atp.ram.dto.response.LogRecordShort;
import org.qubership.atp.ram.dto.response.MessageParameter;
import org.qubership.atp.ram.dto.response.PaginationResponse;
import org.qubership.atp.ram.dto.response.ScreenshotResponse;
import org.qubership.atp.ram.dto.response.logrecord.Content;
import org.qubership.atp.ram.dto.response.logrecord.LogRecordContentResponse;
import org.qubership.atp.ram.entities.AkbContext;
import org.qubership.atp.ram.entities.ErrorMappingItem;
import org.qubership.atp.ram.enums.ContextVariablesActiveTab;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.MaskCondition;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.exceptions.logrecords.RamIllegalLogRecordsFetchSourceException;
import org.qubership.atp.ram.mapper.Mapper;
import org.qubership.atp.ram.model.LogRecordFilteringRequest;
import org.qubership.atp.ram.model.LogRecordWithChildrenResponse;
import org.qubership.atp.ram.model.LogRecordWithParentListResponse;
import org.qubership.atp.ram.model.SubstepScreenshotResponse;
import org.qubership.atp.ram.models.AkbRecord;
import org.qubership.atp.ram.models.BrowserConsoleLogsTable;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.Issue;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.LogRecordContextVariable;
import org.qubership.atp.ram.models.LogRecordContextVariableObject;
import org.qubership.atp.ram.models.LogRecordMessageParameters;
import org.qubership.atp.ram.models.LogRecordStepContextVariable;
import org.qubership.atp.ram.models.MetaInfo;
import org.qubership.atp.ram.models.SsmMetricReports;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.logrecords.BvLogRecord;
import org.qubership.atp.ram.models.logrecords.CompoundLogRecord;
import org.qubership.atp.ram.models.logrecords.UiLogRecord;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.qubership.atp.ram.repositories.AkbRecordsRepository;
import org.qubership.atp.ram.repositories.CustomLogRecordRepository;
import org.qubership.atp.ram.repositories.ExecutionRequestRepository;
import org.qubership.atp.ram.repositories.IssueRepository;
import org.qubership.atp.ram.repositories.LogRecordContextVariableCommonRepository;
import org.qubership.atp.ram.repositories.LogRecordContextVariableRepository;
import org.qubership.atp.ram.repositories.LogRecordMessageParametersRepository;
import org.qubership.atp.ram.repositories.LogRecordRepository;
import org.qubership.atp.ram.repositories.LogRecordStepContextVariableRepository;
import org.qubership.atp.ram.repositories.TestRunRepository;
import org.qubership.atp.ram.utils.PathsGenerator;
import org.qubership.atp.ram.utils.SourceShot;
import org.qubership.atp.ram.utils.StepPath;
import org.qubership.atp.ram.utils.StreamUtils;
import org.qubership.atp.ram.utils.TimeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogRecordService extends CrudService<LogRecord> {

    static final Map<TestingStatuses, TestingStatuses> invertStatusMap = new HashMap();

    private static final String TEST_RUN = "testrun";
    private static final String EXECUTION_REQUEST = "executionrequest";
    private static final String BASE_64_PREFIX = "data:image/png;base64,";

    @Value("${browser.monitoring.link}")
    private String browserMonitoringLinkTemplate;

    private final Mapper<LogRecord, LogRecordShort> logRecordMapper;
    private final LogRecordRepository repository;
    private final LogRecordContextVariableRepository logRecordContextRepository;
    private final LogRecordStepContextVariableRepository logRecordStepContextRepository;
    private final LogRecordMessageParametersRepository logRecordMessageParametersRepository;
    private final TestRunRepository testRunRepository;
    private final GridFsService gridFsService;
    private final AkbRecordsRepository akbRecordsRepository;
    private final ExecutionRequestRepository executionRequestRepository;
    private final ModelMapper modelMapper;
    private final CustomLogRecordRepository customLogRecordRepository;
    private final IssueRepository issueRepository;
    private final CatalogueService catalogueService;
    private final LogRecordContextVariableService contextVariableService;
    private final BrowserConsoleLogService browserConsoleLogService;

    /**
     * Init invertStatusMap.
     */
    @PostConstruct
    public void init() {
        invertStatusMap.put(TestingStatuses.FAILED, TestingStatuses.PASSED);
        invertStatusMap.put(TestingStatuses.PASSED, TestingStatuses.FAILED);
        invertStatusMap.put(TestingStatuses.WARNING, TestingStatuses.FAILED);
    }

    @Override
    protected MongoRepository<LogRecord, UUID> repository() {
        return repository;
    }

    @Override
    public List<LogRecord> getAll() {
        return repository.findAll();
    }

    public List<LogRecord> getByIds(List<UUID> logRecordIds) {
        return (List<LogRecord>) repository.findAllById(logRecordIds);
    }

    /**
     * Gets log record children.
     *
     * @param id the id
     * @return the log record children
     */
    public Stream<LogRecord> getLogRecordChildren(UUID id) {
        return repository.findAllByParentRecordIdIsOrderByCreatedDateStampAsc(id);
    }

    /**
     * Get step context variables of logrecord.
     *
     * @param id if of logrecord.
     * @return List of step context variables.
     */
    public List<ContextVariable> getStepContextVariablesOfLogRecord(UUID id) {
        LogRecordStepContextVariable stepContext = logRecordStepContextRepository.getById(id);
        return isNull(stepContext) ? Collections.EMPTY_LIST : stepContext.getContextVariables();
    }

    /**
     * Get context variables by logRecordIds.
     * @param logRecordIds logRecordIds
     * @return list of context variables collected from logRecords
     */
    public List<ContextVariable> getContextVariablesByIds(List<UUID> logRecordIds) {
        List<LogRecordContextVariable> logRecordContextVariables = logRecordContextRepository.findAllByIdIn(
                logRecordIds);
        if (logRecordContextVariables == null) {
            return Collections.emptyList();
        }
        return logRecordContextVariables
                .stream()
                .flatMap(logRecordContextVariable -> logRecordContextVariable.getContextVariables().stream())
                .collect(Collectors.toList());
    }

    private List<ContextVariable> getContextVariables(UUID id, LogRecordContextVariableCommonRepository repository) {

        LogRecordContextVariableObject logRecordContextVariables = repository.getById(id);
        if (logRecordContextVariables == null) {
            return Collections.emptyList();
        }

        return logRecordContextVariables.getContextVariables();
    }

    private Object getContextVariables(UUID id, Integer page, Integer size,
                                       List<ContextVariablesActiveTab> activeTabs,
                                       LogRecordContextVariableCommonRepository repository) {
        List<ContextVariable> contextVariables = getContextVariables(id, repository);

        if (isEmpty(contextVariables) || isNull(page) || isNull(size) || isEmpty(activeTabs)) {
            return Collections.emptyList();
        } else {
            Collections.sort(contextVariables);
        }

        return contextVariableService.getPagedContextVariables(contextVariables, page, size, activeTabs);
    }

    /**
     * Get context variables of logrecord.
     *
     * @param id   if of logrecord
     * @param page number of page for pagination
     * @param size size of 1 page for pagination
     * @return List of paginated context variables and total caount of context variables.
     */
    public Object getContextVariables(UUID id, Integer page, Integer size,
                                      List<ContextVariablesActiveTab> activeTabs) {
        return getContextVariables(id, page, size, activeTabs, logRecordContextRepository);
    }

    /**
     * Get All context variables of logrecord.
     *
     * @param id   if of logrecord
     * @return List of paginated context variables and total caount of context variables.
     */
    public List<ContextVariable> getAllContextVariables(UUID id) {
        return getContextVariables(id, logRecordContextRepository);
    }

    /**
     * Gets step context variables.
     *
     * @param id         the id
     * @param page       the page
     * @param size       the size
     * @param activeTabs the active tabs
     * @return the step context variables
     */
    public Object getStepContextVariables(UUID id, Integer page, Integer size,
                                          List<ContextVariablesActiveTab> activeTabs) {
        return getContextVariables(id, page, size, activeTabs, logRecordStepContextRepository);
    }

    private ContextVariablesResponse filterContextVariables(UUID id,
                                                            List<String> parameters,
                                                            String beforeValue,
                                                            String afterValue,
                                                            LogRecordContextVariableCommonRepository repository) {
        List<ContextVariable> contextVariables = getContextVariables(id, repository);
        if (isEmpty(contextVariables)) {
            return new ContextVariablesResponse();
        }
        return contextVariableService.filterAndSplitContextVariables(contextVariables, parameters, beforeValue,
                afterValue);
    }


    /**
     * Filter context variable of logrecord by specified parameters.
     *
     * @param id          if of logrecord
     * @param parameters  list of context variables names to filter by
     * @param beforeValue beforeValue to filter by using "contains" strategy
     * @param afterValue  afterValue to filter by using "contains" strategy
     * @return list of context variables that have passed all predicates.
     */
    public ContextVariablesResponse filterContextVariables(UUID id,
                                                           List<String> parameters,
                                                           String beforeValue,
                                                           String afterValue) {
        return filterContextVariables(id, parameters, beforeValue, afterValue, logRecordContextRepository);
    }

    /**
     * Filter step context variables context variables response.
     *
     * @param id          the id
     * @param parameters  the parameters
     * @param beforeValue the before value
     * @param afterValue  the after value
     * @return the context variables response
     */
    public ContextVariablesResponse filterStepContextVariables(UUID id,
                                                               List<String> parameters,
                                                               String beforeValue,
                                                               String afterValue) {
        return filterContextVariables(id, parameters, beforeValue, afterValue, logRecordStepContextRepository);
    }

    public void delete(UUID uuid) {
        repository.deleteByUuid(uuid);
    }

    public List<LogRecord> getOrderedChildrenLogRecordsForParentLogRecord(UUID uuid) {
        return repository.findAllByParentRecordIdOrderByStartDateAsc(uuid);
    }

    public LogRecord create() {
        return repository.save(new LogRecord());
    }

    @Override
    public LogRecord save(LogRecord logRecord) {
        LogRecord logRecordUpd = repository.save(logRecord);
        updateAllParentsStatuses(logRecordUpd);
        return logRecordUpd;
    }

    /**
     * Find {@link LogRecord} by uuid or throw NPE if LR doesn't exist.
     *
     * @param logRecordId for find {@link LogRecord}
     * @return return log record by uuid or throw NPE
     */
    public LogRecord findById(UUID logRecordId) {
        LogRecord logRecord = repository.findByUuid(logRecordId);
        Preconditions.checkNotNull(logRecord, "Cannot find log record by ID " + logRecordId);

        Stream<LogRecord> children = this.getLogRecordChildren(logRecordId);

        logRecord.setChildren(children
                .map(LogRecord.Child::new)
                .collect(Collectors.toList()));

        logRecord.setContextVariablesPresent(isContextVariablesPresent(logRecordId));
        logRecord.setBrowserConsoleLogsPresent(browserConsoleLogService.isBrowserConsoleLogsPresent(logRecordId));
        logRecord.setMessageParametersPresent(isMessageParametersPresent(logRecordId));

        if (logRecord instanceof UiLogRecord) {
            setBrowserMonitoringLink((UiLogRecord) logRecord);
        }
        return logRecord;
    }

    private void setBrowserMonitoringLink(UiLogRecord uiLogRecord) {
        if (Strings.isNullOrEmpty(browserMonitoringLinkTemplate) || isNull(uiLogRecord.getBrowserName())
                || isNull(uiLogRecord.getStartDate()) || isNull(uiLogRecord.getEndDate())) {
            log.warn("Can't generate browser monitoring link for logRecord '{}' due to empty template"
                            + " or logRecord params: template={}, BrowserName={}, StartDate={}, EndDate={}",
                    uiLogRecord.getUuid(), browserMonitoringLinkTemplate, uiLogRecord.getBrowserName(),
                    uiLogRecord.getStartDate(), uiLogRecord.getEndDate());
        } else {
            uiLogRecord.setBrowserMonitoringLink(browserMonitoringLinkTemplate
                    .replace(RamConstants.BROWSER_POD, uiLogRecord.getBrowserName())
                    .replace(RamConstants.FROM_TIMESTAMP, Long.toString(uiLogRecord.getStartDate().getTime()))
                    .replace(RamConstants.TO_TIMESTAMP, Long.toString(uiLogRecord.getEndDate().getTime())));
        }
    }

    public LogRecord findLastInProgressLogRecordByTestRunId(UUID testRunId) {
        return repository.findFirstByTestRunIdAndExecutionStatusOrderByCreatedDateStampDesc(testRunId,
                ExecutionStatuses.IN_PROGRESS);
    }

    public LogRecord findLastInProgressOrcLogRecordByTestRunId(UUID testRunId) {
        return customLogRecordRepository.findLastOrcLogRecordByTestRunAndExecutionStatus(testRunId,
                ExecutionStatuses.IN_PROGRESS);
    }

    private boolean isContextVariablesPresent(UUID logRecordId) {
        return logRecordContextRepository.existsById(logRecordId)
                || logRecordStepContextRepository.existsById(logRecordId);
    }

    private boolean isMessageParametersPresent(UUID logRecordId) {
        return logRecordMessageParametersRepository.existsById(logRecordId);
    }

    /**
     * Find by logRecordUuid from request or create new.
     */
    @Deprecated
    private LogRecord findByRequestOrCreate(LogRecord request) {
        UUID logRecordId = request.getUuid();
        LogRecord logRecord = repository.findByUuid(logRecordId);
        if (isNull(logRecord)) {
            log.trace("Log Record: {} not found.", logRecordId);
            logRecord = createLogRecordByRequest(request);
        }
        return logRecord;
    }

    private void updateAllParentsStatuses(@NotNull LogRecord logRecord) {
        UUID parentId = logRecord.getParentRecordId();
        if (isNull(parentId)) {
            TestRun testRun = testRunRepository.findByUuid(logRecord.getTestRunId());
            testRun.updateTestingStatus(logRecord.getTestingStatus());
            testRunRepository.save(testRun);
        } else {
            LogRecord parent = repository.findByUuid(parentId);
            if (nonNull(parent)) {
                parent.setTestingStatus(logRecord.getTestingStatus());
                save(parent);
            }
        }
    }

    /**
     * Find BrowserConsoleLogs by LogRecordUUID with pagination.
     */
    public PaginationResponse<BrowserConsoleLogsTable> getBrowserConsoleLogsTable(
            UUID logRecordUuid, Pageable pageable) {
        return browserConsoleLogService.getBrowserConsoleLogsTable(logRecordUuid, pageable);
    }

    /**
     * Create BrowserConsoleLog for LogRecord.
     *
     * @param logRecordUuid LogRecordId
     * @param logs logs to save
     */
    public void createBrowserConsoleLog(UUID logRecordUuid, List<BrowserConsoleLogsTable> logs) {
        browserConsoleLogService.createBrowserConsoleLog(logRecordUuid, logs);
    }

    /**
     * Add existing AkbRecord to LogRecord.
     */
    public String addLogRecordAkb(UUID logRecordId, UUID akbRecordId) {
        if (isNull(logRecordId)) {
            log.error("Found illegal nullable log record id for the validated method parameter");
            throw new AtpIllegalNullableArgumentException("log record id", "method parameter");
        }
        if (isNull(akbRecordId)) {
            log.error("Found illegal nullable akb record id for the validated method parameter");
            throw new AtpIllegalNullableArgumentException("akb record id", "method parameter");
        }
        LogRecord logRecord = get(logRecordId);
        AkbRecord akbRecord = akbRecordsRepository.findByUuid(akbRecordId);
        if (isNull(akbRecord)) {
            log.warn("AKB wasn't added to Log Record. There is no AKB {}.", akbRecordId);
            return "There is no AKB with uuid: " + akbRecordId;
        } else {
            repository.save(logRecord);
            log.trace("AKB: {} added to Log Record: {}.", logRecordId, akbRecordId);
            return "AKB with uuid: " + akbRecordId + " was successfully added to LogRecord with uuid: "
                    + logRecordId;
        }
    }

    /**
     * Save number of screenshots fot TestRuns from current ER.
     *
     * @param executionRequestUuid Execution Request UUID
     * @deprecated use LogRecordLoggingController instead of this.
     */
    public void saveCountScreenshots(UUID executionRequestUuid, List<TestRun> testRuns) {
        int numberOfScreen = 0;
        Map<UUID, String> trIdAndNumberOfScreens = new HashMap<>();
        for (TestRun testRun : testRuns) {
            UUID testRunId = testRun.getUuid();
            try {
                List<LogRecord> logRecords = getAllLogRecordsUuidByTestRunId(testRunId);
                if (!logRecords.isEmpty()) {
                    numberOfScreen = gridFsService.getCountScreen(logRecords);
                }
                trIdAndNumberOfScreens.put(testRunId, String.valueOf(numberOfScreen));
                testRun.setNumberOfScreens(numberOfScreen);
            } catch (Exception e) {
                log.error("Error in calculating screenshots count for Test Run {}.", testRunId, e);
            }
        }
        testRunRepository.saveAll(testRuns);
        log.trace("Number of screens for ER {}: {}", executionRequestUuid, trIdAndNumberOfScreens);
    }

    /**
     * Analyzes all LogRecords in TestRun.
     */
    public void analyze(TestRun testRun) {
        List<LogRecord> logRecords = getAllNotSectionLogRecordsByTestRunId(testRun.getUuid());
        List<AkbRecord> akbRecords = akbRecordsRepository.findAll();
        for (LogRecord logRecord : logRecords) {
            for (AkbRecord akbRecord : akbRecords) {
                analyzeAkbRecord(akbRecord, logRecord, testRun);
            }
        }
    }

    /**
     * Create {@link LogRecord} by request from adapter.
     *
     * @param request with data for {@link LogRecord}
     * @return {@link LogRecord} object
     */
    @Deprecated
    public LogRecord createLogRecordByRequest(LogRecord request) {
        UUID logRecordId = request.getUuid();
        log.trace("Creating Log Record: {}", logRecordId);
        LogRecord record = request;
        Timestamp startDate = new Timestamp(System.currentTimeMillis());
        Timestamp finishDate = new Timestamp(System.currentTimeMillis());
        try {
            startDate = record.getStartDate();
        } catch (Exception e) {
            log.error("Cannot parse StartDate in request!", e);
        }
        record.setStartDate(startDate);
        record.setEndDate(finishDate);
        record.setDuration(TimeUtils.getDuration(startDate, finishDate));
        record.setCreatedDate(new Timestamp(System.currentTimeMillis()));

        return save(record);
    }

    private void analyzeAkbRecord(AkbRecord akbRecord, LogRecord logRecord, TestRun testRun) {
        if (!checkAkbByContext(akbRecord, testRun)) {
            log.debug("Limit by AKB context, Akb Record {}, Log Record: {}", akbRecord.getUuid(),
                    logRecord.getUuid());
            return;
        }

        String message = logRecord.getMessage();
        String name = logRecord.getName();

        String messageRegularExpression = akbRecord.getMessageRegularExpression();
        String nameRegularExpression = akbRecord.getNameRegularExpression();
        MaskCondition maskCondition = akbRecord.getMaskCondition();

        boolean isOrCondition = maskCondition == null || MaskCondition.OR.equals(maskCondition);
        boolean condition = isSuccessfulAnalyzeByAkb(name, nameRegularExpression);
        if (isOrCondition) {
            condition |= isSuccessfulAnalyzeByAkb(message, messageRegularExpression);
        } else {
            condition &= isSuccessfulAnalyzeByAkb(message, messageRegularExpression);
        }
        if (condition) {
            updateObjectsAfterAnalyze(testRun, akbRecord, logRecord);
        }
    }

    private boolean checkAkbByContext(AkbRecord akbRecord, TestRun testRun) {
        AkbContext context = akbRecord.getAkbContext();
        if (context == null) {
            return true;
        }
        List<String> tcNameByContext = context.getTestCasesName();
        List<UUID> tpUuidByContext = context.getTestPlansId();

        String tcNameByTestRun = testRun.getTestCaseName();
        UUID tpUuidByTestRun = executionRequestRepository
                .findByUuid(testRun.getExecutionRequestId()).getTestPlanId();

        boolean res = true;
        if (!tcNameByContext.isEmpty()) {
            res = tcNameByContext.contains(tcNameByTestRun);
        }
        if (!tpUuidByContext.isEmpty()) {
            res &= tpUuidByContext.contains(tpUuidByTestRun);
        }
        return res;
    }

    private boolean isSuccessfulAnalyzeByAkb(String valueForAnalyze, String regularExpression) {
        String regExp;
        if (null != (regExp = regularExpression)) {
            Pattern p = Pattern.compile(regExp);
            Matcher m = p.matcher(valueForAnalyze);
            return m.find();
        }
        return false;
    }

    private void updateObjectsAfterAnalyze(TestRun testRun, AkbRecord akbRecord, LogRecord lr) {
        UUID rootCauseType = akbRecord.getRootCauseId();
        if (rootCauseType != null) {
            testRun.setRootCauseId(rootCauseType);
            testRunRepository.save(testRun);
        }
        addLogRecordAkb(lr.getUuid(), akbRecord.getUuid());
    }

    // todo DROP SERVICES VARIABLES FROM METHODS PARAMETERS

    /**
     * Loads list of {@link ErrorMappingItem} from specified sources.
     *
     * @param executionRequestService {@link ExecutionRequestService}
     * @param source                  TestRun or ExecutionRequest.
     * @param parentUuid              UUID of TestRun or ExecutionRequest
     * @return list of {@link ErrorMappingItem}
     */
    public List<ErrorMappingItem> getErrorMapping(ExecutionRequestService executionRequestService,
                                                  String source, UUID parentUuid) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(source),
                "LogRecord source is not specified. Use TestRun or ExecutionRequest");
        Preconditions.checkArgument(nonNull(parentUuid),
                "ParentUuid of LogRecords can't be empty");
        List<LogRecord> logRecords = getLogRecords(executionRequestService, source, parentUuid);
        return logRecords.stream().map(ErrorMappingItem::new).collect(Collectors.toList());
    }

    private List<LogRecord> getLogRecords(ExecutionRequestService executionRequestService, String source,
                                          UUID parentId) {
        List<LogRecord> logRecords = Lists.newLinkedList();
        switch (source.toLowerCase()) {
            case TEST_RUN:
                logRecords.addAll(findLogRecordsWithSpecificFieldsByTestRunIdOrderByStartDateAsc(parentId));
                break;
            case EXECUTION_REQUEST:
                List<TestRun> runs = executionRequestService.getAllTestRuns(parentId);
                runs.stream()
                        .map(testRun ->
                                findLogRecordsWithSpecificFieldsByTestRunIdOrderByStartDateAsc(testRun.getUuid()))
                        .collect(Collectors.toList())
                        .forEach(logRecords::addAll);
                break;
            default:
                ExceptionUtils.throwWithLog(log, new RamIllegalLogRecordsFetchSourceException());
        }
        return logRecords;
    }

    /**
     * Gets project id by parent id.
     *
     * @param parentId parent id
     * @param source source type
     * @return project id
     */
    public UUID getProjectIdByParentId(UUID parentId, String source) {
        switch (source.toLowerCase()) {
            case TEST_RUN:
                return testRunRepository.findProjectIdByTestRunId(parentId);
            case EXECUTION_REQUEST:
                return executionRequestRepository.findProjectIdByUuid(parentId).getProjectId();
            default:
                throw new IllegalArgumentException(
                        "Invalid LogRecord source is specified. Use 'TestRun' or 'ExecutionRequest'");
        }
    }

    public StepPath getLogRecordPath(UUID uuid) {
        PathsGenerator pathsGenerator = new PathsGenerator(this);
        return pathsGenerator.generatePathToFoundLogRecord(uuid);
    }

    /**
     * Add existing AkbRecord to LogRecord.
     */
    public LogRecord addLogRecordAkbRecords(UUID logRecordUuid, List<UUID> akbRecordsUuid) {
        Preconditions.checkNotNull(logRecordUuid, "LogRecordUuid cannot be null");
        Preconditions.checkNotNull(akbRecordsUuid, "AKB Records cannot be null");

        LogRecord logRecord = repository.findByUuid(logRecordUuid);
        List<AkbRecord> newAkbRecords = akbRecordsUuid.stream()
                .map(akbRecordsRepository::findByUuid)
                .collect(Collectors.toList());
        log.debug("Akb Records: {} was added for Log Record: {}", akbRecordsUuid.toString(), logRecordUuid);
        return save(logRecord);
    }

    /**
     * Returns List of LogRecord for TestRun and ParentLogRecord.
     */
    public List<LogRecord> findByTestRunIdAndParentUuid(UUID testRunId, UUID parentId,
                                                        LogRecordFilteringRequest filteringRequest) {
        if (filteringRequest != null) {
            List<String> statuses = filteringRequest.getStatuses();
            List<String> types = filteringRequest.getTypes();
            boolean showNotAnalyzedItemsOnly = filteringRequest.isShowNotAnalyzedItemsOnly();

            if (!isEmpty(statuses) || !isEmpty(types)) {
                return customLogRecordRepository.getTopLogRecordsByFilterLookup(
                        testRunId, statuses, types, showNotAnalyzedItemsOnly);
            }
        }

        return repository
                .findLogRecordsForTreeByTestRunIdAndParentRecordIdOrderByCreatedDateStampAsc(testRunId, parentId)
                .stream()
                .filter(logRecord -> filterLogRecordByRequest(logRecord, filteringRequest))
                .collect(Collectors.toList());
    }

    public List<LogRecord> findTopLogRecordsOnTestRun(UUID testRunId) {
        return repository
                .findLogRecordsByTestRunIdAndParentRecordIdOrderByCreatedDateStampAsc(testRunId, null);
    }

    /**
     * Filter LogRecordNode by filteringRequest.
     *
     * @param logRecord        LogRecord
     * @param filteringRequest Request to perform filtering based on it`s parameters
     * @return true if logRecordNode meets the requirements otherwise false
     */

    private boolean filterLogRecordByRequest(LogRecord logRecord, LogRecordFilteringRequest filteringRequest) {
        if (filteringRequest == null) {
            return true;
        }
        return (filteringRequest.getStatuses() == null || filteringRequest.getStatuses().stream()
                .anyMatch(status ->
                        status.compareToIgnoreCase(logRecord.getTestingStatus().name()) == 0))
                && (filteringRequest.getTypes() == null || filteringRequest.getTypes().stream()
                .anyMatch(type ->
                        type.compareToIgnoreCase(logRecord.getType().name()) == 0))
                && (!filteringRequest.isShowNotAnalyzedItemsOnly() || logRecord.getRootCause() == null);
    }

    /**
     * Returns List of full LogRecords for TestRun.
     */
    public List<LogRecord> findAllByTestRunIdOrderByStartDateAsc(UUID testRunUuid,
                                                                 LogRecordFilteringRequest filteringRequest) {
        List<LogRecord> records = repository.findAllByTestRunIdOrderByStartDateAsc(testRunUuid);

        return records.stream()
                .filter(logRecord -> filterLogRecordByRequest(logRecord, filteringRequest))
                .collect(Collectors.toList());
    }

    /**
     * Returns List of LogRecords with id, status, type and preview  for TestRun.
     */
    public List<LogRecord> findLogRecordsWithPreviewByTestRunIdOrderByStartDateAsc(
            UUID testRunUuid,
            LogRecordFilteringRequest filteringRequest) {
        Stream<LogRecord> records = repository.findLogRecordsWithPreviewByTestRunIdOrderByStartDateAsc(testRunUuid);

        return records
                .filter(logRecord -> filterLogRecordByRequest(logRecord, filteringRequest))
                .collect(Collectors.toList());
    }

    /**
     * Returns List of LogRecords with id, status, type, name, message, parent id, is section, is compound, testRun id
     * and duration for TestRun.
     */
    public List<LogRecord> findLogRecordsWithSpecificFieldsByTestRunIdOrderByStartDateAsc(UUID testRunId) {
        return repository.findLogRecordsWithSpecificFieldsByTestRunIdOrderByStartDateAsc(testRunId);
    }

    /**
     * Returns names of LogRecords (@Link LogRecord) for TestRun.
     */
    public List<LogRecord> findLogRecordNamesByTestRunId(UUID testRunUuid) {
        return repository.findAllNameByTestRunId(testRunUuid);
    }

    /**
     * Returns number of children for LogRecord.
     */
    public long getChildrenCount(LogRecord lr) {
        return repository.countAllByParentRecordIdIs(lr.getUuid());
    }

    public UUID getProjectIdByLogRecordId(UUID id) {
        return customLogRecordRepository.getProjectIdByLogRecordId(id);
    }

    /**
     * Find or create log record.
     *
     * @param request creation request
     * @return result
     * @deprecated use LogRecordLoggingController instead of this.
     */
    @Deprecated
    public LogRecord findOrCreate(LogRecord request) {
        log.debug("Start of search (or creating - if the Log Record was not found) by request:\n{}", request);
        LogRecord logRecord = findByRequestOrCreate(request);
        TestRun testRun = findByRequest(request);

        if (ExecutionStatuses.TERMINATED.equals(testRun.getExecutionStatus())) {
            log.warn("TestRun [{}] is already terminated", testRun.getUuid());
        } else {
            testRun.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
            testRunRepository.save(testRun);

            ExecutionRequest executionRequest =
                    executionRequestRepository.findByUuid(testRun.getExecutionRequestId());
            executionRequest.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
            executionRequestRepository.save(executionRequest);

            log.trace("Set In Progress status for TR {} and ER {}.", testRun.getUuid(),
                    testRun.getExecutionRequestId());
        }

        return logRecord;
    }

    /**
     * Find by testRunId from logrecord or find by testRunId from request.
     */
    @Nonnull
    @Deprecated
    public TestRun findByRequest(LogRecord request) {
        LogRecord logRecord = findById(request.getUuid());
        if (isNull(logRecord) || isNull(logRecord.getTestRunId())) {
            return testRunRepository.findByUuid(logRecord.getTestRunId());
        } else {
            return testRunRepository.findByUuid(logRecord.getTestRunId());
        }
    }

    public List<LogRecord> getAllLogRecordsUuidByTestRunId(UUID testRunId) {
        return repository.findAllUuidByTestRunId(testRunId);
    }

    public List<LogRecord> getAllSectionNotCompoundLogRecordsByTestRunId(UUID testRunId) {
        return repository.findAllByTestRunIdAndIsSectionAndIsCompaund(testRunId, true, false);
    }

    public List<LogRecord> getAllNotSectionLogRecordsByTestRunId(UUID testRunId) {
        return repository.findAllByTestRunIdAndIsSection(testRunId, false);
    }

    public List<LogRecord> getAllFailedLogRecordsByTestRunId(UUID testRunId) {
        return repository.findAllByTestRunIdAndTestingStatus(testRunId, TestingStatuses.FAILED);
    }

    public List<LogRecord> getUuidAndMessageFailedLogRecordsByTestRunId(UUID testRunId) {
        return repository.findUuidAndMessageByTestRunIdAndTestingStatus(testRunId, TestingStatuses.FAILED);
    }

    public List<LogRecord> getAllTestingStatusLogRecordsByTestRunId(UUID testRunId) {
        return repository.findAllTestingStatusByTestRunId(testRunId);
    }

    public List<LogRecord> getAllFailedLogRecordsByTestRunIds(Collection<UUID> testRunsIds) {
        return repository.findAllByTestRunIdInAndTestingStatus(testRunsIds, TestingStatuses.FAILED)
                .collect(Collectors.toList());
    }

    public List<LogRecord> getAllNotStartedLogRecordsByTestRunId(UUID testRunId) {
        return repository.findAllByTestRunIdAndTestingStatus(testRunId, TestingStatuses.NOT_STARTED);
    }

    /**
     * Get list of child LR or current LR.
     *
     * @param id of current LR
     * @return list of LR-s
     */
    public List<LogRecord> geChildLogRecordsOrParent(UUID id) {
        List<LogRecord> result = getOrderedChildrenLogRecordsForParentLogRecord(id);
        if (isEmpty(result)) {
            LogRecord parent = findById(id);
            if (parent == null) {
                return Collections.emptyList();
            } else {
                return Collections.singletonList(parent);
            }
        }
        return result;
    }

    public List<LogRecord> getAllMatchesLogRecordsByTestRunId(UUID testRunId, String searchValue) {
        return repository.findAllByTestRunIdAndNameContains(testRunId, searchValue);
    }

    public Stream<LogRecord> getAllMatchesLogRecordsByTestRunIdCaseInsensitive(UUID testRunId, String searchValue) {
        return repository.findAllByTestRunIdAndNameRegex(testRunId, "(?i)\\Q" + searchValue + "\\E");
    }

    /**
     * Find parent LR by UUID and his children, if exists.
     *
     * @param uuid for find LR
     * @return parent LR and his children if exists
     */
    public LogRecordResponse getLogRecordByIdWithChildren(UUID uuid) {
        LogRecordShort parent = logRecordMapper.entityToDto(findById(uuid));
        List<LogRecordShort> children =
                repository.findAllByParentRecordIdIsOrderByCreatedDateStampAsc(uuid)
                        .map(logRecordMapper::entityToDto)
                        .collect(Collectors.toList());
        return new LogRecordResponse(parent, children);
    }

    /**
     * Gets location in editor by id of log record.
     *
     * @param id the id
     * @return the location in editor
     */
    public LocationInEditorResponse getLocationInEditor(UUID id) {
        List<Integer> lines = new LinkedList<>();
        LogRecord logRecord = repository.findByUuid(id);
        LogRecord topLog = logRecord;
        Map<UUID, String> hashSums = new HashMap<>();
        while (logRecord != null) {
            MetaInfo metaFoCurrent = logRecord.getMetaInfo();
            if (metaFoCurrent != null) {
                Integer lineForCurrent = metaFoCurrent.getLine();
                if (lineForCurrent != null) {
                    lines.add(0, lineForCurrent); // put in first position
                }
                UUID currentScenarioId = metaFoCurrent.getScenarioId();
                String currentHashSum = metaFoCurrent.getScenarioHashSum();
                if (currentScenarioId != null && StringUtils.isNotEmpty(currentHashSum)) {
                    hashSums.put(currentScenarioId, currentHashSum);
                }
            }
            topLog = logRecord;
            logRecord = repository.findByUuid(logRecord.getParentRecordId());
        }

        UUID testRunId = topLog.getTestRunId();
        TestRun testRun = testRunRepository.findByUuid(testRunId);
        UUID testCaseId = testRun.getTestCaseId();

        UUID scenarioId = catalogueService.getScenarioIdByTestCaseId(testCaseId);
        MetaInfo meta = testRun.getMetaInfo();
        if (meta != null && StringUtils.isNotEmpty(meta.getScenarioHashSum())) {
            hashSums.put(scenarioId, meta.getScenarioHashSum());
        }

        boolean isOutOfDate = !catalogueService.checkHashSumForScenario(hashSums).isValid();
        String line = StringUtils.join(lines, "_");
        return new LocationInEditorResponse(scenarioId, line, isOutOfDate);
    }

    /**
     * Get general info for logrecord.
     */
    public LogRecordContentResponse getLogRecordContent(UUID id) {
        List<Content> children =
                repository.findAllByParentRecordIdIsOrderByCreatedDateStampAsc(id).map(child ->
                        modelMapper.map(child, Content.class)
                ).collect(Collectors.toList());
        return new LogRecordContentResponse(children);
    }

    /**
     * Get all test run BV log records.
     *
     * @param testRunId test rin identifier
     * @return log records
     */
    public List<BvLogRecord> getAllTestRunBvLogRecordsWithValidationSteps(UUID testRunId) {
        return repository.findAllByTestRunIdAndType(testRunId, TypeAction.BV)
                .stream()
                .filter(logRecord -> nonNull(logRecord.getValidationTable())
                        && nonNull(logRecord.getValidationTable().getSteps())
                        && !logRecord.getValidationTable().getSteps().isEmpty())
                .map(logRecord -> (BvLogRecord) logRecord)
                .collect(Collectors.toList());
    }

    /**
     * Find LR-s with fields 'uuid' and 'name' by testing status and test run id.
     *
     * @param testingStatuses status of found LR-s
     * @param testRunId       uuid TR for found LR-s
     * @return list of LR-s with fields 'uuid' and 'name'
     */
    public List<LogRecord> getAllByTestingStatusAndTestRunId(TestingStatuses testingStatuses, UUID testRunId) {
        return repository.findAllByTestingStatusAndTestRunId(testingStatuses, testRunId);
    }

    public List<LogRecord> findLogRecordsWithValidationParamsByTestRunIds(Collection<UUID> testRunIds) {
        return customLogRecordRepository.findLogRecordsByTestRunIdsAndValidationWithHint(testRunIds);
    }

    public List<LogRecord> findFailedLogRecordsWithMetaInfoByTestRunIds(Collection<UUID> testRunIds) {
        return repository.findLogRecordsWithMetaInfoByTestRunIdInAndTestingStatus(testRunIds, TestingStatuses.FAILED);
    }

    public Stream<LogRecord> findLogRecordsWithValidationParamsAndFailureByTestRunIds(Collection<UUID> testRunIds) {
        return repository.findLogRecordsWithValidationParamsAndFailureByTestRunIds(testRunIds);
    }

    /**
     * Finds child Log Records by name.
     *
     * @param id          of parent Log Record
     * @param searchValue part of the child Log Record name to search
     * @return list of child Log Record
     */
    public List<LogRecord> getLogRecordChildrenByName(UUID id, String searchValue) {
        return repository.findAllByParentRecordIdAndNameContains(id, searchValue);
    }

    /**
     * Get all log record children previews.
     *
     * @param logRecordId log record id
     * @return list of previews
     */
    public LogRecordScreenshotResponse getAllLogRecordScreenshotPreviews(UUID logRecordId) {
        log.info("Get log record '{}' children screenshow previews", logRecordId);
        LogRecord rootLogRecord = get(logRecordId);

        LogRecordScreenshotResponse response = new LogRecordScreenshotResponse();

        List<LogRecordPreviewResponse> childrenPreviews =
                getAllHierarchicalChildrenLogRecords(logRecordId)
                        .stream()
                        .filter(logRecord -> TypeAction.UI.equals(logRecord.getType())
                                && !Strings.isNullOrEmpty(logRecord.getPreview()))
                        .map(logRecord -> new LogRecordPreviewResponse(
                                rootLogRecord.getTestRunId(), logRecord.getUuid(),
                                logRecord.getPreview(), logRecord.getTestingStatus()))
                        .collect(Collectors.toList());

        log.debug("Found children log records with id's: {}",
                StreamUtils.extractIds(childrenPreviews, LogRecordPreviewResponse::getLogRecordId));
        response.setChildrenPreviews(childrenPreviews);

        if (childrenPreviews.isEmpty()) {
            setScreenshotContentToResponse(logRecordId, response);
        }

        return response;
    }

    /**
     * Update log record execution status.
     *
     * @param logRecordId log record id
     * @param request     update request
     */
    public void updateExecutionStatus(UUID logRecordId, UpdateLogRecordExecutionStatusRequest request) {
        log.info("Update execution status for log record '{}' with request data: {}", logRecordId, request);
        LogRecord logRecord = get(logRecordId);
        if (!Strings.isNullOrEmpty(request.getName())) {
            logRecord.setName(request.getName());
        }
        logRecord.setExecutionStatus(request.getExecutionStatus());
        if (nonNull(request.getStartDate())) {
            logRecord.setStartDate(request.getStartDate());
        }
        logRecord.setEndDate(request.getEndDate());
        logRecord.setDuration(request.getDuration());

        log.debug("Update log record '{}' with data: {}", logRecordId, logRecord);
        repository.save(logRecord);
    }

    private void updateContextVariables(UUID logRecordId, UpdateLogRecordContextVariablesRequest request,
                                        LogRecordContextVariableCommonRepository repository) {
        log.info("Update context variables for log record '{}'", logRecordId);
        LogRecordContextVariableObject context = repository.getById(logRecordId);
        if (context == null) {
            context = new LogRecordContextVariable();
        }
        context.setContextVariables(request.getContextVariables());
        log.debug("Update log record '{}' with data: {}", logRecordId, context);
        repository.save(context);
    }

    /**
     * Update log record context variables.
     *
     * @param logRecordId log record id
     * @param request     update request
     */
    public void updateContextVariables(UUID logRecordId, UpdateLogRecordContextVariablesRequest request) {
        updateContextVariables(logRecordId, request, logRecordContextRepository);
    }

    /**
     * Update log record step context variables.
     *
     * @param logRecordId log record id
     * @param request     update request
     */
    public void updateStepContextVariables(UUID logRecordId, UpdateLogRecordContextVariablesRequest request) {
        updateContextVariables(logRecordId, request, logRecordStepContextRepository);
    }

    /**
     * Update log record message parameters.
     *
     * @param logRecordId log record id
     * @param request     update request
     */
    public void updateMessageParameters(UUID logRecordId, UpdateLogRecordMessageParametersRequest request) {
        log.info("Update message parameters for log record '{}'", logRecordId);
        LogRecordMessageParameters context = logRecordMessageParametersRepository.getById(logRecordId);
        if (context == null) {
            context = new LogRecordMessageParameters();
        }
        context.setMessageParameters(request.getMessageParameters());
        context.setCreatedDate(request.getCreatedDate());
        log.debug("Update log record '{}' with data: {}", logRecordId, context);
        logRecordMessageParametersRepository.save(context);
    }

    /**
     * Get all hierarchical children log records for parent log record.
     *
     * @param parentLogRecordId paren log record id
     * @return children log records
     */
    public List<LogRecord> getAllHierarchicalChildrenLogRecords(UUID parentLogRecordId) {
        log.info("Get all hierarchical children log records for parent: {}", parentLogRecordId);
        List<LogRecord> logRecords = new ArrayList<>();

        List<LogRecord> topLevelChildren = getLogRecordChildren(parentLogRecordId).collect(Collectors.toList());
        log.debug("Top level children: {}", StreamUtils.extractIds(topLevelChildren));
        if (!isEmpty(topLevelChildren)) {
            topLevelChildren.forEach(childLogRecord -> getAllHierarchicalUILogRecords(logRecords, childLogRecord));
        }
        log.debug("Result: {}", StreamUtils.extractIds(logRecords));

        return logRecords;
    }

    /**
     * Get hierarchical UI log records.
     *
     * @param list      list container
     * @param logRecord parent log record
     */
    public void getAllHierarchicalUILogRecords(List<LogRecord> list, LogRecord logRecord) {
        if (TypeAction.UI.equals(logRecord.getType()) && logRecord.getPreview() != null) {
            log.debug("Add log record: {}", logRecord.getUuid());
            list.add(logRecord);
        }

        List<LogRecord> children = getLogRecordChildren(logRecord.getUuid()).collect(Collectors.toList());
        log.debug("Children: {}", StreamUtils.extractIds(children));
        if (!isEmpty(children)) {
            children.forEach(childLogRecord -> getAllHierarchicalUILogRecords(list, childLogRecord));
        }
    }

    /**
     * Find all log records (uuid, testRunId, testing status, execution status) with by lastLoaded and testRunIds
     * search params.
     *
     * @param lastLoaded last loaded date
     * @param testRunIds test run ids
     * @return result
     */
    List<LogRecord> findAllByLastUpdatedAfterAndTestRunIdIn(Date lastLoaded, List<UUID> testRunIds) {
        log.info("Find all log records with last update date after '{}' and with test run id in: {}",
                lastLoaded, testRunIds);

        List<LogRecord> result = repository.findAllByLastUpdatedAfterAndTestRunIdIn(lastLoaded, testRunIds);
        log.debug("Founded log records: {}", StreamUtils.extractIds(result));

        return result;
    }

    /**
     * Revert testing status for log record.
     * Note if you need correct test run status as well
     * you have to call method testRunService.revertTestingStatusForLogRecord
     * instead of this
     *
     * @param logRecordId the log record id
     * @return the log record
     */
    public LogRecord revertTestingStatusForLogRecord(UUID logRecordId) {
        LogRecord logRecord = findById(logRecordId);
        TestingStatuses testingStatus = logRecord.getTestingStatus();

        TestingStatuses newTestingStatus = null;
        if (testingStatus != null) {
            newTestingStatus = invertStatusMap.get(testingStatus);
        }
        log.info("current status = {}, new status = {} for LR [{}]", testingStatus, newTestingStatus, logRecordId);
        if (newTestingStatus != null) {
            logRecord.setTestingStatusHard(newTestingStatus);
            logRecord = save(logRecord);
            log.debug("logRecord = {}", logRecord);

            if (logRecord.getParentRecordId() != null) {
                LogRecord parentLog = findById(logRecord.getParentRecordId());
                if (parentLog instanceof CompoundLogRecord) {
                    forceUpdateCompoundTestingStatusByChild(parentLog);
                }
            }
        }

        return logRecord;
    }

    private void forceUpdateCompoundTestingStatusByChild(LogRecord parentLog) {
        log.debug("start forceUpdateCompoundTestingStatusByChild(parentLog: {})", parentLog.getUuid());
        List<LogRecord.Child> child = parentLog.getChildren();
        TestingStatuses finalTestingStatus = TestingStatuses.UNKNOWN;
        for (LogRecord.Child logRecord : child) {
            finalTestingStatus =
                    TestingStatuses.compareAndGetPriority(logRecord.getTestingStatus(), finalTestingStatus);
        }
        log.debug("forceUpdateCompoundTestingStatusByChild: finalTestingStatus = {} for log record {}",
                finalTestingStatus, parentLog.getUuid());
        parentLog.setTestingStatusHard(finalTestingStatus);
        save(parentLog);
        log.debug("forceUpdateCompoundTestingStatusByChild: parentLog.getParentRecordId() = {} for log record {}",
                parentLog.getParentRecordId(),
                parentLog.getUuid());
        if (parentLog.getParentRecordId() != null) {
            LogRecord parentLog0 = findById(parentLog.getParentRecordId());
            if (parentLog0 instanceof CompoundLogRecord) {
                forceUpdateCompoundTestingStatusByChild(parentLog0);
            }
        }
    }

    public LogRecord findLogRecordForTreeByUuid(UUID logRecordId) {
        return repository.findLogRecordForTreeByUuid(logRecordId);
    }

    /**
     * Method returns quantity of all Log Records which match to current and new patterns.
     * Returning value contains quantity of all Log Records which match to current and new patterns.
     *
     * @param request contains parameters for calculation
     * @return object of type {@link LogRecordQuantityResponse}
     */
    public LogRecordQuantityResponse getQuantityLogRecordsWhichMatchToPatterns(
            LogRecordQuantityMatchPatternRequest request) {
        final UUID failPatternId = request.getFailPatternId();
        final UUID executionRequestId = request.getExecutionRequestId();

        log.info("Start getting quantity log records which match to patterns. "
                        + "[executionRequestId = {}, FailPatternId = {}, NewPattern = {}]",
                executionRequestId, failPatternId, request.getNewPattern());

        Pattern pattern = Pattern.compile(request.getNewPattern());

        List<LogRecord> logRecords = testRunRepository.findAllByExecutionRequestId(executionRequestId)
                .stream()
                .flatMap(testRun -> getAllFailedLogRecordsByTestRunId(testRun.getUuid()).stream())
                .collect(Collectors.toList());

        long countAfterUpdateRule = logRecords.stream()
                .filter(logRecord -> !StringUtils.isEmpty(logRecord.getMessage())
                        && pattern.matcher(logRecord.getMessage()).find())
                .count();

        long countBeforeUpdateRule;
        if (isNull(failPatternId)) {
            countBeforeUpdateRule = countAfterUpdateRule;
        } else {
            List<Issue> failurePatternBeforeIssues =
                    issueRepository.findByFailPatternIdAndExecutionRequestId(failPatternId, executionRequestId);

            countBeforeUpdateRule = failurePatternBeforeIssues.stream()
                    .filter(issue -> !isEmpty(issue.getLogRecordIds()))
                    .mapToInt(issue -> issue.getLogRecordIds().size())
                    .sum();
        }

        log.debug("Finish getting quantity log records which match to patterns. "
                        + "[countBeforeUpdateRule = {}, countAfterUpdateRule = {}]",
                countBeforeUpdateRule, countAfterUpdateRule);

        return new LogRecordQuantityResponse(countBeforeUpdateRule, countAfterUpdateRule);
    }

    /**
     * Return list of LR with fields: validationLabels, validationTable, testingStatus.
     *
     * @param testRunId for search of log records
     * @return list of log records for current test run
     */
    public List<LogRecord> findLogRecordsWithValidationParamsAndStatusByTrId(UUID testRunId) {
        return repository.findLogRecordsWithValidationParamsByTestRunId(testRunId);
    }

    /**
     * Get log record message parameters.
     *
     * @param id log record identifier
     * @return message parameters
     */
    public List<MessageParameter> getLogRecordMessageParameters(UUID id) {
        // TODO: after a couple months need to create ticket to remove this condition
        LogRecordMessageParameters logRecordMessageParameters = logRecordMessageParametersRepository.getById(id);
        if (logRecordMessageParameters != null && !logRecordMessageParameters.getMessageParameters().isEmpty()) {
            return logRecordMessageParameters.getMessageParameters();
        }

        return get(id).getMessageParameters();
    }

    private List<String> searchContextVariableParameters(UUID logRecordId, String name,
                                                         LogRecordContextVariableCommonRepository repository) {
        log.info("Search context variable parameters for log record '{}' by name: {}", logRecordId, name);
        LogRecordContextVariableObject context = repository.getById(logRecordId);
        if (context == null) {
            return Collections.emptyList();
        }

        List<ContextVariable> contextVariables = context.getContextVariables();
        if (isEmpty(contextVariables)) {
            return Collections.emptyList();
        }

        List<String> foundParameters = contextVariables.stream()
                .filter(contextVariable -> nonNull(contextVariable.getName())
                        && StringUtils.containsIgnoreCase(contextVariable.getName(), name))
                .map(ContextVariable::getName)
                .sorted()
                .collect(Collectors.toList());
        log.debug("Found parameters: {}", foundParameters);

        return foundParameters;
    }

    /**
     * Search context variable parameters.
     *
     * @param logRecordId log record identifier
     * @param name        search name
     * @return result parameter names list
     */
    public List<String> searchContextVariableParameters(UUID logRecordId, String name) {
        return searchContextVariableParameters(logRecordId, name, logRecordContextRepository);
    }

    /**
     * Search step context variable parameters list.
     *
     * @param logRecordId the log record id
     * @param name        the name
     * @return the list
     */
    public List<String> searchStepContextVariableParameters(UUID logRecordId, String name) {
        return searchContextVariableParameters(logRecordId, name, logRecordContextRepository);
    }

    /**
     * Get first parent and children LR.
     *
     * @param testRunId       TR id
     * @param testingStatuses status
     * @return parent and children LR
     */
    public LogRecordWithChildrenResponse getLogRecordWithChildrenByTrIdAndStatus(UUID testRunId,
                                                                                 TestingStatuses testingStatuses) {
        return customLogRecordRepository.getLogRecordParentAndChildrenByTestingStatusAndTestRunId(testRunId,
                testingStatuses);
    }

    /**
     * Get count LR-s for TR.
     *
     * @param testRunsId for count LR-s
     * @return count LR-s
     */
    public Long countLrsByTestRunsId(Set<UUID> testRunsId) {
        return repository.countAllByTestRunIdIn(testRunsId);
    }

    /**
     * Get list of substeps response.
     *
     * @param logRecordIds for get screenshot
     * @return list of {@link SubstepScreenshotResponse}
     */
    public List<SubstepScreenshotResponse> getSubstepScreenshots(List<UUID> logRecordIds) {
        List<SubstepScreenshotResponse> substepScreenshotResponses = new ArrayList<>();

        logRecordIds.forEach(uuid -> {
            SubstepScreenshotResponse response = new SubstepScreenshotResponse();
            response.setId(uuid);
            setScreenshotContentToResponse(uuid, response);
            substepScreenshotResponses.add(response);
        });

        return substepScreenshotResponses;
    }

    public List<LogRecordWithParentListResponse> findLogRecordsWithParentsByPreviewExists(UUID testRunId) {
        return customLogRecordRepository.findLogRecordsWithParentsByPreviewExists(testRunId);
    }

    /**
     * Set screenshot to {@link  LogRecordScreenshotResponse}.
     *
     * @param logRecordId for get screenshot
     * @param response for update source
     */
    private void setScreenshotContentToResponse(UUID logRecordId, ScreenshotResponse response) {
        SourceShot screenShot = gridFsService.getScreenShot(logRecordId);
        if (screenShot != null && StringUtils.isNotEmpty(screenShot.getContent())) {
            String content = screenShot.getContent().replaceAll(BASE_64_PREFIX, StringUtils.EMPTY);
            response.setScreenshot(content);
            response.setScreenshotSource(screenShot.getSnapshotSource());
        }
    }

    public List<LogRecord> getAllLogRecordsByTestRunIds(List<UUID> testRunIds) {
        return repository.findAllByTestRunIdIn(testRunIds);
    }

    /**
     * Update SSM metric reports data.
     *
     * @param logRecordId log record identifier
     * @param data SSM metric reports data
     */
    public void updateSsmMetricReportsData(UUID logRecordId, SsmMetricReports data) {
        log.info("Updating log record SSM metric reports data for ER with id '{}'", logRecordId);
        log.info("Updates: {}", data);

        LogRecord logRecord = get(logRecordId);
        log.debug("Found log record with id '{}'", logRecord.getUuid());

        SsmMetricReports ssmMetricReports = logRecord.getSsmMetricReports();
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

        logRecord.setSsmMetricReports(ssmMetricReports);

        log.debug("Updated SSM metric reports data: {}", ssmMetricReports);
        save(logRecord);

        log.info("Log record has been successfully updated");
    }

    public Long countAllFailedLrByTestRunIds(Collection<UUID> testRunsIds) {
        return repository.countAllByTestRunIdInAndTestingStatus(testRunsIds, TestingStatuses.FAILED);
    }

    public Long countAllPassedLrByTestRunIds(Collection<UUID> testRunsIds) {
        return repository.countAllByTestRunIdInAndTestingStatus(testRunsIds, TestingStatuses.PASSED);
    }

    /**
     * Get failed LR-s by TR-s id and exclude some LR-s by ID-s.
     *
     * @param testRunsIds for find LR-s
     * @param logRecordsIdForExclude for exclude LR-s by id
     * @return stream of {@link LogRecord}
     */
    public Stream<LogRecord> getAllFailedLogRecordsByTestRunIdsStream(Collection<UUID> testRunsIds,
                                                                      Collection<UUID> logRecordsIdForExclude) {
        return repository.findAllByUuidNotInAndTestRunIdInAndTestingStatus(logRecordsIdForExclude, testRunsIds,
                TestingStatuses.FAILED);
    }
}
