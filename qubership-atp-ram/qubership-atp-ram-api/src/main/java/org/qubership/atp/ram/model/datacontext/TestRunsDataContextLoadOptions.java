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

package org.qubership.atp.ram.model.datacontext;

import lombok.Getter;

@Getter
public class TestRunsDataContextLoadOptions {
    private boolean includeRunMap;
    private boolean includeRunValidationLogRecordsMap;
    private boolean includeRunFailedLogRecordsMap;
    private boolean includeRunTestCasesMap;
    private boolean includeRootCausesMap;
    private boolean includeTestRunDslNamesMap;

    public TestRunsDataContextLoadOptions includeTestRunMap() {
        this.includeRunMap = true;
        return this;
    }

    public TestRunsDataContextLoadOptions includeRunValidationLogRecordsMap(boolean condition) {
        this.includeRunValidationLogRecordsMap = condition;
        return this;
    }

    public TestRunsDataContextLoadOptions includeRunValidationLogRecordsMap() {
        this.includeRunValidationLogRecordsMap = true;
        return this;
    }

    public TestRunsDataContextLoadOptions includeRunFailedLogRecordsMap() {
        this.includeRunFailedLogRecordsMap = true;
        return this;
    }

    public TestRunsDataContextLoadOptions includeRunTestCasesMap() {
        this.includeRunTestCasesMap = true;
        return this;
    }

    public TestRunsDataContextLoadOptions includeRootCausesMap() {
        this.includeRootCausesMap = true;
        return this;
    }

    public TestRunsDataContextLoadOptions includeTestRunDslNamesMap() {
        this.includeTestRunDslNamesMap = true;
        return this;
    }
}
