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

package org.qubership.atp.ram.testdata;

import java.sql.Timestamp;
import java.util.Queue;
import java.util.UUID;

import javax.annotation.Nullable;

import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.models.TestPlan;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.spring.ServiceProvider;

import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;

public class TestDataBuilder extends ServiceProvider {

    private final LogRecordBuilder LOG_RECORD_BUILDER = new LogRecordBuilder();
    private Project project;
    private TestPlan testPlan;
    private ExecutionRequest executionRequest;
    private TestRun testRun;

    public TestDataBuilder project(String name) {
        project = new Project();
        project.setName(name);
        projectsService.save(project);
        return this;
    }

    public TestDataBuilder testPlan(String name) {
        Preconditions.checkNotNull(project, "Project isn't created, create project at first");
        testPlan = new TestPlan();
        testPlan.setName(name);
        testPlan.setProjectId(this.project.getUuid());
        testPlansService.save(testPlan);
        return this;
    }

    public TestDataBuilder er(String name) {
        Preconditions.checkNotNull(testPlan, "TestPlan isn't created, create test plan at first.");
        executionRequest = new ExecutionRequest();
        executionRequest.setTestPlanId(testPlan.getUuid());
        executionRequest.setName(name);
        executionRequestService.save(executionRequest);
        return this;
    }

    public TestDataBuilder testRun(String name, TestingStatuses status) {
        testRun = new TestRun();
        testRun.setName(name);
        testRun.setExecutionRequestId(executionRequest.getUuid());
        testRun.updateTestingStatus(status);
        testRunService.save(testRun);
        return this;
    }

    public Project getProject() {
        return project;
    }

    public TestPlan getTestPlan() {
        return testPlan;
    }

    public ExecutionRequest getExecutionRequest() {
        return executionRequest;
    }

    public TestRun getTestRun() {
        return testRun;
    }

    public LogRecordBuilder logRecordBuilder() {
        return LOG_RECORD_BUILDER;
    }

    public class LogRecordBuilder {

        private Queue<LogRecord> sectionQueue = Queues.newLinkedBlockingQueue();
        private LogRecord logRecord;

        public LogRecordBuilder logRecord(String name, TestingStatuses status) {
            LogRecord logRecord = new LogRecord();
            logRecord.setName(name);
            logRecord.setTestingStatus(status);
            logRecord.setTestRunId(getTestRun().getUuid());
            logRecord.setParentRecordId(getParentRecordUuid());
            logRecord.setMessage("Message for: " + name);
            logRecord.setStartDate(new Timestamp(System.currentTimeMillis() - Short.MAX_VALUE));
            logRecord.setEndDate(new Timestamp(System.currentTimeMillis()));
            this.logRecord = logRecord;
            logRecordService.save(logRecord);
            return this;
        }

        @Nullable
        private UUID getParentRecordUuid() {
            LogRecord peek = sectionQueue.peek();
            if (peek != null) {
                return peek.getUuid();
            }
            return null;
        }

        public LogRecordBuilder openSection(String name, TestingStatuses status) {
            LogRecordBuilder logRecord = logRecord(name, status);
            LogRecord record = logRecord.getLogRecord();
            record.setSection(true);
            logRecordService.save(record);
            this.sectionQueue.add(record);
            return this;
        }

        public LogRecordBuilder closeSection() {
            this.sectionQueue.remove();
            return this;
        }

        public LogRecord getLogRecord() {
            return this.logRecord;
        }

        public LogRecordBuilder cleanupMemory() {
            return this;
        }

        public void build() {
            this.sectionQueue.clear();
        }
    }
}
