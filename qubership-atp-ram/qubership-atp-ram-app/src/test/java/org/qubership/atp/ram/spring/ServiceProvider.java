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

package org.qubership.atp.ram.spring;

import org.qubership.atp.ram.service.mail.MailService;
import org.qubership.atp.ram.services.AkbRecordsService;
import org.qubership.atp.ram.services.DefectsService;
import org.qubership.atp.ram.services.ExecutionRequestService;
import org.qubership.atp.ram.services.LabelsService;
import org.qubership.atp.ram.services.LogRecordService;
import org.qubership.atp.ram.services.ProjectsService;
import org.qubership.atp.ram.services.TestPlansService;
import org.qubership.atp.ram.services.TestRunService;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ServiceProvider {
    @Autowired
    protected ProjectsService projectsService;
    @Autowired
    protected TestPlansService testPlansService;
    @Autowired
    protected ExecutionRequestService executionRequestService;
    @Autowired
    protected TestRunService testRunService;
    @Autowired
    protected LogRecordService logRecordService;
    @Autowired
    protected LabelsService labelsService;
    @Autowired
    protected AkbRecordsService akbRecordsService;
    @Autowired
    protected MailService mailService;
    @Autowired
    protected DefectsService defectsService;
}
