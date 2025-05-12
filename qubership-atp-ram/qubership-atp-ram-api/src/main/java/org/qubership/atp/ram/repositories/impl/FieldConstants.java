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

package org.qubership.atp.ram.repositories.impl;

/*
* Please, insert new constants in alphabetical order for better and faster search!
*/
public interface FieldConstants {

    String OPERATOR = "$";
    String ID = "id";
    String _ID = "_id";
    String $ID = OPERATOR + _ID;
    String $_ID = OPERATOR + _ID;

    String ANALYZED_BY_QA = "analyzedByQa";

    String CHILDREN = "children";
    String $CHILDREN = OPERATOR + CHILDREN;
    String CHILDREN_DEPTH = "children.depth";
    String CHILDREN_0 = "children.0";
    String CREATED_DATE = "createdDate";
    String CREATED_DATE_STAMP = "createdDateStamp";

    String DURATION = "duration";
    String $DURATION = OPERATOR + DURATION;

    String ENVIRONMENT = "environment";
    String ENVIRONMENT_ID = "environmentId";
    String ENTITIES = "entities";
    String EXECUTION_REQUEST = "executionRequest";
    String $EXECUTION_REQUEST = OPERATOR + EXECUTION_REQUEST;
    String EXECUTION_REQUESTS = "executionRequests";
    String EXECUTION_REQUEST_ID = "executionRequestId";
    String $EXECUTION_REQUEST_ID = OPERATOR + EXECUTION_REQUEST_ID;
    String EXECUTION_REQUEST_NAME = "executionRequestName";
    String EXECUTION_STATUS = "executionStatus";
    String EXECUTOR = "executor";
    String EXECUTOR_ID = "executorId";
    String EXECUTOR_NAME = "executorName";
    String ISSUE = "issue";
    String $ISSUE = OPERATOR + "issue";
    String FILTERED_BY_LABELS = "filteredByLabels";
    String FILTERED_BY_LABELS_IDS = "filteredByLabelsIds";
    String FAILED_RATE = "failedRate";
    String FAIL_TEST_RUNS_COUNT_FIELD = "failedTestRunsCount";
    String FAIL_PATTERN = "failPattern";
    String FAIL_PATTERN_NAME_FIELD = "failPattern.name";
    String FAIL_REASON = "failReason";
    String FAIL_REASON_NAME_FIELD = "failReason.name";
    String FILE_NAME = "fileName";
    String FILE_TYPE = "fileMetadata.type";
    String FINISH_DATE = "finishDate";
    String $FINISH_DATE = OPERATOR + FINISH_DATE;
    String JIRA_TICKETS = "jiraTickets";
    String JIRA_DEFECTS = "jiraDefects";
    String LAST_UPDATED = "lastUpdated";
    String LEVEL = "level";
    String BROWSER_CONSOLE_LOGS_TABLE = "browserConsoleLogsTable";
    String $BROWSER_CONSOLE_LOGS_TABLE = OPERATOR + BROWSER_CONSOLE_LOGS_TABLE;
    String LOG_RECORD_ID = "logRecordId";
    String LOG_RECORD_FILE_METADATA = "fileMetadata.type";
    String LOG_RECORD_TEST_RUN_ID = "testRunId";

    String MESSAGE = "message";
    String METADATA = "metadata";
    String $METADATA = OPERATOR + METADATA;

    String NAME = "name";
    String UUID = "uuid";

    String PARENT = "parent";
    String PARENT_RECORD_ID = "parentRecordId";
    String $PARENT_LOG_RECORD_ID = OPERATOR + PARENT_RECORD_ID;
    String PARENT_TEST_RUN_ID = "parentTestRunId";
    String PASSED_RATE = "passedRate";
    String PREVIEW = "preview";
    String PROJECT_ID = "projectId";

    String QA_SYSTEM_INFO_LIST = "qaSystemInfoList";

    String START_DATE = "startDate";
    String END_DATE = "endDate";
    String $START_DATE = OPERATOR + START_DATE;

    String TA_SYSTEM_INFO_LIST = "taSystemInfoList";
    String TESTCASE_ID = "testCaseId";
    String TESTCASE_NAME = "testCaseName";
    String TESTING_STATUS = "testingStatus";
    String LOG_COLLECTOR_DATA = "logCollectorData";
    String $TESTING_STATUS = OPERATOR + TESTING_STATUS;
    String TEST_RUN_ID = "testRunId";
    String TEST_RUN = "testrun";
    String TESTRUNS = "testRuns";
    String $TEST_RUN = "$testrun";
    String TIMESTAMP = "timestamp";
    String TOTAL_COUNT = "totalCount";
    String TYPE = "type";

    String ROOT_CAUSE = "rootCause";
    String $ROOT_CAUSE = "$rootCause";
    String ROOT_CAUSE_ID = "rootCauseId";
    String RESOLVED = "resolved";
    String RESULTS = "results";

    String USER_ID = "userId";
    String URL = "url";

    String WARNING_RATE = "warningRate";

    String VALIDATION_LABELS = "validationLabels";
    String VALIDATION_TABLE = "validationTable";

    String VALIDATION_STEPS_LABELS = VALIDATION_TABLE + ".steps." + VALIDATION_LABELS;
}
