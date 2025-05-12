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

package org.qubership.atp.ram.service.rest.dto;

public class SingleProperties {

    private String baseUrl;
    private String isSingleUiEnabled;

    public SingleProperties() {
    }

    public SingleProperties(String baseUrl, String isSingleUiEnabled) {
        this.baseUrl = baseUrl;
        this.isSingleUiEnabled = isSingleUiEnabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getIsSingleUiEnabled() {
        return isSingleUiEnabled;
    }

    public void setIsSingleUiEnabled(String isSingleUiEnabled) {
        this.isSingleUiEnabled = isSingleUiEnabled;
    }
}
