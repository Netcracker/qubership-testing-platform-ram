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

package org.qubership.atp.ram.tsg.senders.fdr;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.ram.tsg.senders.Sender;
import org.qubership.atp.ram.tsg.service.TsgService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("fdrSender")
@ConditionalOnProperty(
        value = "kafka.fdr.enable",
        havingValue = "false",
        matchIfMissing = true
)
public class RestSender implements Sender<List<UUID>> {

    private final TsgService tsgService;

    public RestSender(TsgService tsgService) {
        this.tsgService = tsgService;
    }

    @Override
    public void send(List<UUID> testRunIds) {
        tsgService.sendFdrs(testRunIds);
    }
}
