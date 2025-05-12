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

package org.qubership.atp.ram;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.testdata.TestDataBuilder;

public class TestDataGeneration extends TestDataBuilder {

    @Test
    @Disabled("Use only for generation local test data.")
    public void testGenerateStructure() {
        project("TD Project")
                .testPlan("TD TestPlan")
                .er("TD ExecutionRequest")
                .testRun("TD TestRun", TestingStatuses.PASSED)
                .logRecordBuilder()
                .openSection("FirstSection", TestingStatuses.PASSED)
                .logRecord("Login", TestingStatuses.PASSED)
                .logRecord("Validation Login", TestingStatuses.PASSED)
                .closeSection()
                .openSection("SecondSection", TestingStatuses.FAILED)
                .logRecord("Logout", TestingStatuses.PASSED)
                .logRecord("Login", TestingStatuses.PASSED)
                .openSection("Validations", TestingStatuses.FAILED)
                .openSection("Check logout", TestingStatuses.FAILED)
                .logRecord("Verify [LogOff] btn", TestingStatuses.FAILED)
                .closeSection()
                .logRecord("Failed Case", TestingStatuses.FAILED)
                .build();
    }
}
