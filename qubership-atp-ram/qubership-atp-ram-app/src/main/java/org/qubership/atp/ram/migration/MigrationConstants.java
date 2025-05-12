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

package org.qubership.atp.ram.migration;

public interface MigrationConstants {
    String CHANGE_LOGS_SCAN_PACKAGE = "org.qubership.atp.ram.migration.changelogs";
    String _ID = "_id";

    String CREATED_DATE_FIELD_NAME = "createdDate";
    String CREATED_DATE_INDEX_NAME = "created_date";
    String CREATED_DATE_INDEX_NAME_1 = "createdDate_1";
    String EXPIRE_DATE_INDEX_FIELD = "expireAfterSeconds";
    String NAME_INDEX_FIELD = "name";


    String LOG_RECORDS_COLLECTION_NAME = "logrecord";
    String LOG_RECORD_MESSAGE_PARAMETERS_COLLECTION_NAME = "logrecordMessageParameters";
    String LOG_RECORD_SCRIPT_REPORT_COLLECTION_NAME = "logrecordScriptReport";


    String LOG_RECORD_CONTEXT_VARIABLES_COLLECTION_NAME = "logrecordContextVariables";
    String LOG_RECORD_STEP_CONTEXT_VARIABLES_COLLECTION_NAME = "logrecordStepContextVariables";
    String LOG_RECORD_BROWSER_CONSOLE_LOGS_COLLECTION_NAME = "logrecordBrowserConsoleLogs";

}
