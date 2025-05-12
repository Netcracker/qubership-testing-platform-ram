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

package org.qubership.atp.ram.tsg.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.qubership.atp.auth.springbootstarter.utils.ExceptionUtils;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.exceptions.internal.RamTsgUrlIllegalArgumentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TsgConfiguration {

    @Value("${tsg.receiver.url}")
    private String tsgReceiverUrl;

    private StatusConfig statuses;

    /**
     * Returns current tsg receiver url.
     */
    public String getTsgReceiverUrl() {
        return tsgReceiverUrl;
    }

    /**
     * Set new TsgReceiver URL.
     */
    public void setTsgReceiverUrl(String tsgReceiverUrl) {
        if (Strings.isNullOrEmpty(tsgReceiverUrl)) {
            ExceptionUtils.throwWithLog(log, new RamTsgUrlIllegalArgumentException());
        }
        this.tsgReceiverUrl = tsgReceiverUrl;
    }

    /**
     * Returns Statuses configuration.
     */
    public StatusConfig getStatuses() {
        if (statuses == null) {
            StatusConfig config = new StatusConfig();

            List<ExecutionStatuses> executionStatuses = new ArrayList<>();
            Collections.addAll(executionStatuses,
                    ExecutionStatuses.FINISHED, ExecutionStatuses.TERMINATED_BY_TIMEOUT, ExecutionStatuses.TERMINATED);
            config.setExecutionStatuses(executionStatuses);

            List<TestingStatuses> testingStatuses = new ArrayList<>();
            testingStatuses.add(TestingStatuses.FAILED);
            testingStatuses.add(TestingStatuses.STOPPED);
            testingStatuses.add(TestingStatuses.WARNING);
            config.setTestingStatuses(testingStatuses);

            statuses = config;
        }
        return statuses;
    }

    /**
     * Updates Statuses configuration.
     */
    public void setStatuses(StatusConfig statuses) {
        this.statuses = statuses;
    }
}
