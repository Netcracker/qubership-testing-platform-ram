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

package org.qubership.atp.ram.config;

import org.qubership.atp.ram.service.rest.dto.SingleProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SingleConfiguration {

    @Value("${base.url}")
    private String baseUrl;
    @Value("${atp.ram.singleui.enabled}")
    private String isSingleUiEnabled;

    @Autowired
    public SingleConfiguration() {
    }

    public SingleProperties getProperties() {
        return new SingleProperties(baseUrl, isSingleUiEnabled);
    }

}
