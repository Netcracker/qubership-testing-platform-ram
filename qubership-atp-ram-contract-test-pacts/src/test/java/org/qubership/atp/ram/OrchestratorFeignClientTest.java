/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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

package org.qubership.atp.ram;

import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.ExternalResourceSupport;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.orchestrator.clients.dto.TerminateRequestDto;
import org.qubership.atp.ram.client.OrchestratorFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import lombok.extern.slf4j.Slf4j;

@EnableFeignClients(clients = {OrchestratorFeignClient.class})
@ExtendWith(ExternalResourceSupport.class)
@SpringJUnitConfig(classes = {OrchestratorFeignClientTest.TestApp.class})
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, FeignConfiguration.class,
        FeignAutoConfiguration.class})
@TestPropertySource(
        properties = {"feign.atp.orchestrator.name=atp-orchestrator", "feign.atp.orchestrator.route=",
                "feign.atp.orchestrator.url=http://localhost:8888", "feign.httpclient.enabled=false"})
@Slf4j
public class OrchestratorFeignClientTest {

    @Rule
    public PactProviderRule mockProvider = new PactProviderRule("atp-orchestrator", "localhost", 8888, this);
    @Autowired
    OrchestratorFeignClient orchestratorFeignClient;

    @Test
    @PactVerification()
    public void allPass() {
        UUID id = UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304");
        List<UUID> uuids = List.of(id);
        TerminateRequestDto terminateRequestDto = new TerminateRequestDto();
        terminateRequestDto.setExecutionRequestIds(uuids);
        terminateRequestDto.setIsGracefully(false);


        ResponseEntity<String> result1 = orchestratorFeignClient.terminateProcess(terminateRequestDto);
        Assertions.assertEquals(200, result1.getStatusCode().value());
        Assertions.assertTrue(Objects.requireNonNull(result1.getHeaders().get("Content-Type")).contains("text/plain"));

        ResponseEntity<String> result2 = orchestratorFeignClient.stopProcess(uuids);
        Assertions.assertEquals(200, result2.getStatusCode().value());
        Assertions.assertTrue(Objects.requireNonNull(result2.getHeaders().get("Content-Type")).contains("text/plain"));

        ResponseEntity<String> result3 = orchestratorFeignClient.resumeProcess(uuids);
        Assertions.assertEquals(200, result3.getStatusCode().value());
        Assertions.assertTrue(Objects.requireNonNull(result3.getHeaders().get("Content-Type")).contains("text/plain"));

        ResponseEntity<String> result4 = orchestratorFeignClient.restartProcess(uuids);
        Assertions.assertEquals(200, result4.getStatusCode().value());
        Assertions.assertTrue(Objects.requireNonNull(result4.getHeaders().get("Content-Type")).contains("text/plain"));

        ResponseEntity<String> result5 = orchestratorFeignClient.terminateTestRunProcess(uuids);
        Assertions.assertEquals(200, result5.getStatusCode().value());
        Assertions.assertTrue(Objects.requireNonNull(result5.getHeaders().get("Content-Type")).contains("text/plain"));

        ResponseEntity<String> result6 = orchestratorFeignClient.stopTestRunProcess(id, uuids);
        Assertions.assertEquals(200, result6.getStatusCode().value());
        Assertions.assertTrue(Objects.requireNonNull(result6.getHeaders().get("Content-Type")).contains("text/plain"));

        ResponseEntity<String> result7 = orchestratorFeignClient.resumeTestRunProcess(id, uuids);
        Assertions.assertEquals(200, result7.getStatusCode().value());
        Assertions.assertTrue(Objects.requireNonNull(result7.getHeaders().get("Content-Type")).contains("text/plain"));

        ResponseEntity<String> result8 = orchestratorFeignClient.restartTestRunProcess(id, uuids);
        Assertions.assertEquals(200, result8.getStatusCode().value());
        Assertions.assertTrue(Objects.requireNonNull(result8.getHeaders().get("Content-Type")).contains("text/plain"));

        ResponseEntity<UUID> result9 = orchestratorFeignClient.rerunTestRunsProcess(id, "authorization", uuids);
        Assertions.assertEquals(200, result9.getStatusCode().value());
        Assertions.assertTrue(Objects.requireNonNull(result9.getHeaders().get("Content-Type"))
                .contains("application/json"));

        ResponseEntity<UUID> result10 = orchestratorFeignClient.getRunnerProcessIdByExecutionRequestId(id);
        Assertions.assertEquals(200, result10.getStatusCode().value());
        Assertions.assertTrue(Objects.requireNonNull(result10.getHeaders().get("Content-Type"))
                .contains("application/json"));
    }

    @Pact(consumer = "atp-ram")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        Map<String, String> headers2 = new HashMap<>();
        headers2.put("Content-Type", "text/plain");

        DslPart uuids = PactDslJsonArray
                .arrayEachLike(PactDslJsonRootValue.uuid("e2490de5-5bd3-43d5-b7c4-526e33f71304"));

        DslPart id = PactDslJsonRootValue.uuid("e2490de5-5bd3-43d5-b7c4-526e33f71304");

        PactDslResponse response = builder

                .given("all ok")
                .uponReceiving("POST /api/v1/processorchestrator/flow/er/process/terminate OK")
                .path("/api/v1/processorchestrator/flow/er/process/terminate")
                .method("POST")
                .headers(headers)
                .body("{\"executionRequestIds\":[\"e2490de5-5bd3-43d5-b7c4-526e33f71304\"],\"isGracefully\":false}")
                .willRespondWith()
                .status(200)
                .headers(headers2)
                .body("ERs have been terminated: [e2490de5-5bd3-43d5-b7c4-526e33f71304]")

                .given("all ok")
                .uponReceiving("POST /api/v1/processorchestrator/flow/er/process/stop OK")
                .path("/api/v1/processorchestrator/flow/er/process/stop")
                .method("POST")
                .headers(headers)
                .body(uuids)
                .willRespondWith()
                .status(200)
                .headers(headers2)
                .body("ERs have been suspended: [e2490de5-5bd3-43d5-b7c4-526e33f71304]")

                .given("all ok")
                .uponReceiving("POST /api/v1/processorchestrator/flow/er/process/resume OK")
                .path("/api/v1/processorchestrator/flow/er/process/resume")
                .method("POST")
                .headers(headers)
                .body(uuids)
                .willRespondWith()
                .status(200)
                .headers(headers2)
                .body("ERs have been resumed: [e2490de5-5bd3-43d5-b7c4-526e33f71304]")

                .given("all ok")
                .uponReceiving("POST /api/v1/processorchestrator/flow/er/process/restart OK")
                .path("/api/v1/processorchestrator/flow/er/process/restart")
                .method("POST")
                .headers(headers)
                .body(uuids)
                .willRespondWith()
                .status(200)
                .headers(headers2)
                .body("ERs have been restarted: [e2490de5-5bd3-43d5-b7c4-526e33f71304]")

                .given("all ok")
                .uponReceiving("POST /api/v1/processorchestrator/flow/tr/process/terminate OK")
                .path("/api/v1/processorchestrator/flow/tr/process/terminate")
                .method("POST")
                .headers(headers)
                .body(uuids)
                .willRespondWith()
                .status(200)
                .headers(headers2)
                .body("TRs have been terminated: [e2490de5-5bd3-43d5-b7c4-526e33f71304]")

                .given("all ok")
                .uponReceiving("POST /api/v1/processorchestrator/flow/tr/process/stop OK")
                .path("/api/v1/processorchestrator/flow/tr/process/stop")
                .matchQuery("executionRequestId", "e2490de5-5bd3-43d5-b7c4-526e33f71304")
                .method("POST")
                .headers(headers)
                .body(uuids)
                .willRespondWith()
                .status(200)
                .headers(headers2)
                .body("TRs have been suspended: [e2490de5-5bd3-43d5-b7c4-526e33f71304]")

                .given("all ok")
                .uponReceiving("POST /api/v1/processorchestrator/flow/tr/process/resume OK")
                .path("/api/v1/processorchestrator/flow/tr/process/resume")
                .matchQuery("executionRequestId", "e2490de5-5bd3-43d5-b7c4-526e33f71304")
                .method("POST")
                .headers(headers)
                .body(uuids)
                .willRespondWith()
                .status(200)
                .headers(headers2)
                .body("TRs have been resumed: [e2490de5-5bd3-43d5-b7c4-526e33f71304]")

                .given("all ok")
                .uponReceiving("POST /api/v1/processorchestrator/flow/tr/process/restart OK")
                .path("/api/v1/processorchestrator/flow/tr/process/restart")
                .matchQuery("executionRequestId", "e2490de5-5bd3-43d5-b7c4-526e33f71304")
                .method("POST")
                .headers(headers)
                .body(uuids)
                .willRespondWith()
                .status(200)
                .headers(headers2)
                .body("TRs have been restarted: [e2490de5-5bd3-43d5-b7c4-526e33f71304]")

                .given("all ok")
                .uponReceiving("POST /api/v1/processorchestrator/flow/er/process/restart/{uuid} OK")
                .path("/api/v1/processorchestrator/flow/er/process/restart/e2490de5-5bd3-43d5-b7c4-526e33f71304")
                .matchHeader("Authorization", "\\w*", "authorization")
                .method("POST")
                .headers(headers)
                .body(uuids)
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(id)

                .given("all ok")
                .uponReceiving("GET /api/v1/processorchestrator/flow/er/{executionRequestId}/process/id OK")
                .path("/api/v1/processorchestrator/flow/er/e2490de5-5bd3-43d5-b7c4-526e33f71304/process/id")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(id)

                ;

        return response.toPact();
    }

    @Configuration
    public static class TestApp {

    }
}
