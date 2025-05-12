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

package org.qubership.atp.ram.logging.constants;

public interface ApiPathLogging {
    String FILE_NAME_PARAM = "fileName";
    String CONTENT_TYPE_PARAM = "contentType";
    String SNAPSHOT_SOURCE_PARAM = "snapshotSource";
    String CONTENT_TYPE_FILE = "file";
    String CONTENT_TYPE_IMAGE = "image";
    String WARM_UP_FILE_NAME = "warm_up_";
    String EXECUTE_STEP_NAME = "execute_step_";

    String API_PATH = "/api";
    String LOGGING_PATH = "/logging";
    String LOG_RECORDS_PATH = "/logRecords";
    String TEST_RUNS_PATH = "/testRuns";
    String EXECUTION_REQUESTS_PATH = "/executionRequests";
    String TEST_PLANS_PATH = "/testPlans";
    String STREAM_PATH = "/stream";
    String WITH_PARENTS = "/withParents";
    String FIND_OR_CREATE_PATH = "/findOrCreate";
    String UPDATE_PATH = "/update";
    String UPDATE_EXECUTION_STATUS = "/updExecutionStatus";
    String STOP_PATH = "/stop";
    String UPLOAD_PATH = "/upload";

    String UUID = "uuid";
    String UUID_PATH_VARIABLE = "{" + UUID + "}";
    String UUID_PATH = "/" + UUID_PATH_VARIABLE;
    String START_DATE = "${START_DATE}";
    String END_DATE = "${END_DATE}";
    String TEST_RUN_ID = "testRunId";
    String EXECUTION_STATUS = "executionStatus";
    String EXECUTION_STATUS_PATH = "{" + EXECUTION_STATUS + "}";

    String MAPPER_FOR_LOGGING_BEAN_NAME = "mapperForLogging";
}
