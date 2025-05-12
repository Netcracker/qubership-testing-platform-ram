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

package org.qubership.atp.ram.config;

public interface ApiPath {

    String API = "api";
    String EXECUTOR = "executor";
    String LOG_RECORDS = "logrecords";
    String TEST_RUNS = "testruns";
    String EXECUTION_REQUESTS = "executionrequests";
    String RAM = "ram";
    String REPORTS = "reports";
    String TEST_RUN_ID = "testRunId";

    String STOP = "stop";
    String STOP_TEST_RUNS = "stopTestRuns";
    String UPLOAD = "upload";
    String STREAM = "stream";
    String CREATE = "create";
    String PATCH = "patch";
    String FIND_OR_CREATE = "findOrCreate";
    String UPDATE_OR_CREATE = "updateOrCreate";
    String LOG_RECORD_WITH_CHILDREN = "logRecordWithChildren";
    String DELETE = "delete";
    String SAVE = "save";
    String SAVE_FAILURE_REASON = "saveFailureReason";
    String SAVE_ROOT_CASE = "saveRootCause";
    String UPDATE_FAILURE_REASON = "updFailureReason";
    String UPD_EXECUTION_STATUS = "updExecutionStatus";
    String START = "start";
    String FINISH = "finish";
    String UPD_TESTING_STATUS = "updTestingStatus";
    String UPD_BROWSER_NAMES = "updBrowserNames";
    String UPD_TESTING_STATUS_HARD = "updTestingStatusHard";
    String TERMINATE = "terminate";
    String STOP_RESUME = "stopresume";
    String STATUS_TO_TERMINATED = "setStatusToTerminated";

    String UUID = "uuid";
    String TEST_CASE = "testcase";
    String FAILURE_REASON = "failureReason";
    String ROOT_CAUSE_ID = "rootCauseId";
    String TOP_LEVEL_LOG_RECORDS = "topLevelLogRecords";
    String TEST_CASES = "testCasesList";
    String EXECUTION_STATUS = "executionStatus";
    String TESTING_STATUS = "testingStatus";
    String BROWSER_NAMES = "browserNames";
    String EXECUTION_REQUEST_ID = "executionRequestId";
    String PARENT = "parent";
    String STATISTIC = "statistic";
    String REPORT_LABELS_PARAMS = "reportLabelParams";
    String NAME = "name";
    String NO_MATCH = "nomatch";

    String CONFIG_INFO_PATH = "/configinfo";
    String HARD_PATH = "/hard";
    String SERVER_SUMMARY_PATH = "/serverSummary";
    String ROOT_CAUSES_STATISTIC_PATH = "/rootCausesStatistic";
    String EXECUTION_SUMMARY_PATH = "/executionSummary";
    String TEST_CASES_PATH = "/testCases";
    String UPDATE_PATH = "/update";
    String EXPORT_PATH = "/export";
    String CSV_PATH = "/csv";

    String TEST_CASE_MANAGEMENT_PATH = "/test-case-management";
    String FAIL_PATTERNS_PATH = "/fail-patterns";

    String UUID_PATH_VARIABLE = "{" + UUID + "}";
    String FAILURE_REASON_PATH_VARIABLE = "{" + FAILURE_REASON + "}";
    String ROOT_CAUSE_ID_PATH_VARIABLE = "{" + ROOT_CAUSE_ID + "}";
    String EXECUTION_STATUS_PATH_VARIABLE = "{" + EXECUTION_STATUS + "}";
    String TESTING_STATUS_PATH_VARIABLE = "{" + TESTING_STATUS + "}";
    String EXECUTION_REQUEST_ID_PATH_VARIABLE = "{" + EXECUTION_REQUEST_ID + "}";
    String NAME_PATH_VARIABLE = "{" + NAME + "}";
    String TEST_RUN_ID_VARIABLE = "{" + TEST_RUN_ID + "}";

    String API_PATH = "/" + API;
    String EXECUTOR_PATH = "/" + EXECUTOR;
    String LOG_RECORDS_PATH = "/" + LOG_RECORDS;
    String TEST_RUNS_PATH = "/" + TEST_RUNS;
    String EXECUTION_REQUESTS_PATH = "/" + EXECUTION_REQUESTS;
    String LOG_RECORD_WITH_CHILDREN_PATH = '/' + LOG_RECORD_WITH_CHILDREN;
    String REPORTS_PATH = "/" + REPORTS;

    String TOP_LEVEL_LOG_RECORDS_PATH = "/" + TOP_LEVEL_LOG_RECORDS;
    String TEST_CASES_NAMES_PATH = "/" + TEST_CASES;
    String PARENT_PATH = "/" + PARENT;

    String UUID_PATH = "/" + UUID_PATH_VARIABLE;
    String NAME_PATH = "/" + NAME_PATH_VARIABLE;
    String TEST_CASE_PATH = "/" + TEST_CASE;
    String FAILURE_REASON_PATH = "/" + FAILURE_REASON_PATH_VARIABLE;
    String ROOT_CAUSE_ID_PATH = "/" + ROOT_CAUSE_ID_PATH_VARIABLE;
    String EXECUTION_STATUS_PATH = "/" + EXECUTION_STATUS_PATH_VARIABLE;
    String TESTING_STATUS_PATH = "/" + TESTING_STATUS_PATH_VARIABLE;
    String EXECUTION_REQUEST_ID_PATH = "/" + EXECUTION_REQUEST_ID_PATH_VARIABLE;
    String TEST_RUN_ID_PATH = "/" + TEST_RUN_ID_VARIABLE;
    String NO_MATCH_PATH = "/" + NO_MATCH;

    String STOP_PATH = "/" + STOP;
    String STOP_TEST_RUNS_PATH = "/" + STOP_TEST_RUNS;
    String UPLOAD_PATH = "/" + UPLOAD;
    String STREAM_PATH = "/" + STREAM;
    String CREATE_PATH = "/" + CREATE;
    String PATCH_PATH = "/" + PATCH;
    String FIND_OR_CREATE_PATH = "/" + FIND_OR_CREATE;
    String UPDATE_OR_CREATE_PATH = "/" + UPDATE_OR_CREATE;
    String DELETE_PATH = "/" + DELETE;
    String SAVE_PATH = "/" + SAVE;
    String SAVE_FAILURE_REASON_PATH = "/" + SAVE_FAILURE_REASON;
    String SAVE_ROOT_CASE_PATH = "/" + SAVE_ROOT_CASE;
    String UPDATE_FAILURE_REASON_PATH = "/" + UPDATE_FAILURE_REASON;
    String UPD_EXECUTION_STATUS_PATH = "/" + UPD_EXECUTION_STATUS;
    String START_PATH = "/" + START;
    String FINISH_PATH = "/" + FINISH;
    String UPD_TESTING_STATUS_PATH = "/" + UPD_TESTING_STATUS;
    String UPD_BROWSER_NAMES_PATH = "/" + UPD_BROWSER_NAMES;
    String UPD_TESTING_STATUS_HARD_PATH = "/" + UPD_TESTING_STATUS_HARD;
    String TERMINATE_PATH = "/" + TERMINATE;
    String STOP_RESUME_PATH = "/" + STOP_RESUME;
    String STATUS_TO_TERMINATED_PATH = "/" + STATUS_TO_TERMINATED;
    String STATISTIC_PATH = "/" + STATISTIC;
    String REPORT_LABELS_PARAMS_PATH = "/" + REPORT_LABELS_PARAMS;
    String BULK_PATH = "/bulk";
    String DELAYED_PATH = "/delayed";
    String UPDATE_FINISH_DATE = "/updFinishDate";

    String PROJECT_PATH = "/project";
    String REPORT_EXECUTION_REQUESTS_PATH = '/' + RAM + "/execution-request";

    String ANALYZED_PATH = "/analyzed";
    String SEARCH_PATH = "/search";
    String TESTING_STATUSES_PATH = "/testingStatuses";
    String FAILURE_REASONS_PATH = "/failureReasons";
    String PROPAGATE_JIRA_TICKETS_PATH = "/jiraTickets";
    String JIRA_INFO_PATH = "/infoForJira";
    String JIRA_REFRESH_INFO_PATH = "/infoForRefreshing";

}
