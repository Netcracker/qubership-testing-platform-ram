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

package org.qubership.atp.ram.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;



@Data
@AllArgsConstructor
public class CompareTestRunsDetailsRow {
    String name;
    String rowType;
    List<CompareTestRunsDetailsCell> cellList;

    public CompareTestRunsDetailsRow() {
        cellList = new ArrayList<>();
    }

    public void addCell(CompareTestRunsDetailsCell cell) {
        cellList.add(cell);
    }

    /**
     * Setting row type by string.
     */
    public void setRowType(String type) {
        switch (type.toLowerCase()) {
            case "compound":
                this.rowType =  CompareTestRunsRowType.COMPOUND;
                break;
            case "test_run":
                this.rowType =  CompareTestRunsRowType.TEST_RUN;
                break;
            default:
                this.rowType = CompareTestRunsRowType.ACTION;
        }
    }

    public static class CompareTestRunsRowType {
        public static String TEST_RUN = "TEST_RUN";
        public static String COMPOUND = "COMPOUND";
        public static String ACTION = "ACTION";
    }
}
