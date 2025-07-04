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

package org.qubership.atp.ram.service.template.impl;

import java.util.List;

import org.qubership.atp.ram.dto.response.RootCausesStatisticResponse;
import org.qubership.atp.ram.models.Project;
import org.qubership.atp.ram.utils.TimeUtils;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RootCausesStatisticResponseAdapter {

    private RootCausesStatisticResponse rootCausesStatisticResponse;
    private Project project;

    /**
     * Converts  StartDate value to string and format it as "dd.MM.yyyy, HH:mm:ss"
     * @return datetime value as formatted string
     */
    public String getStartDate() {
        String dateTimeFormat = String.format("%s %s", project.getDateFormat(), project.getTimeFormat());
        String timeZone = project.getTimeZone();

        return TimeUtils.formatDateTime(rootCausesStatisticResponse.getStartDate(), dateTimeFormat, timeZone);
    }

    public String getExecutionRequestName() {
        return rootCausesStatisticResponse.getExecutionRequestName();
    }

    public List<RootCausesStatisticResponse.RootCausesGroup> getRootCausesGroups() {
        return rootCausesStatisticResponse.getRootCausesGroups();
    }
}
