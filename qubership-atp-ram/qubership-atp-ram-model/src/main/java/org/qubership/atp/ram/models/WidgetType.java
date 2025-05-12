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

package org.qubership.atp.ram.models;

import lombok.Getter;

@Getter
public enum WidgetType {
    SUMMARY("classpath:data/widget-templates/ram-accordion-table.template.ftl"),
    SERVER_SUMMARY("classpath:data/widget-templates/ram-server-summary.template.ftl"),
    EXECUTION_SUMMARY("classpath:data/widget-templates/ram-execution-summary.template.ftl"),
    TEST_CASES("classpath:data/widget-templates/ram-accordion-table.template.ftl"),
    TOP_ISSUES("classpath:data/widget-templates/ram-top-issues.template.ftl"),
    ENVIRONMENTS_INFO("classpath:data/widget-templates/ram-environments-info.template.ftl"),
    ROOT_CAUSES_STATISTIC("classpath:data/widget-templates/ram-root-causes-statistics.template.ftl");

    private String templateFilePath;

    WidgetType(String templateFilePath) {
        this.templateFilePath = templateFilePath;
    }
}
