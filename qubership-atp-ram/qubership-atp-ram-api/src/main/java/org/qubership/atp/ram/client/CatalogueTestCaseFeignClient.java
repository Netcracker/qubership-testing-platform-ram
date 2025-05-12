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

package org.qubership.atp.ram.client;

import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.ram.clients.api.TestCaseControllerApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${feign.atp.catalogue.name}", url = "${feign.atp.catalogue.url}",
        path = "${feign.atp.catalogue.route}", configuration = FeignConfiguration.class)
public interface CatalogueTestCaseFeignClient extends TestCaseControllerApi {


}
