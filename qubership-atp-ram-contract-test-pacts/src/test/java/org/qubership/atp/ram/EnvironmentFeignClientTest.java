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

package org.qubership.atp.ram;

import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.ram.client.EnvironmentFeignClient;
import org.qubership.atp.ram.clients.api.dto.environments.environment.BaseSearchRequestDto;
import org.qubership.atp.ram.clients.api.dto.environments.environment.EnvironmentDto;
import org.qubership.atp.ram.clients.api.dto.environments.environment.EnvironmentFullVer1ViewDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;

@RunWith(SpringRunner.class)

@EnableFeignClients(clients = {EnvironmentFeignClient.class})
@ContextConfiguration(classes = {EnvironmentFeignClientTest.TestApp.class})
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, FeignConfiguration.class,
        FeignAutoConfiguration.class})
@TestPropertySource(
        properties = {"feign.atp.environments.name=atp-environments", "feign.atp.environments.route=",
                "feign.atp.environments.url=http://localhost:8888", "feign.httpclient.enabled=false"})
public class EnvironmentFeignClientTest {

    @Rule
    public PactProviderRule mockProvider = new PactProviderRule("atp-environments", "localhost", 8888, this);
    @Autowired
    EnvironmentFeignClient environmentFeignClient;

    @Test
    @PactVerification()
    public void allPass() {
        UUID id = UUID.fromString("7c9dafe9-2cd1-4ffc-ae54-45867f2b9701");
        BaseSearchRequestDto searchRequestDto = new BaseSearchRequestDto();
        searchRequestDto.setIds(asList(id));
        searchRequestDto.setNames(asList("names"));
        searchRequestDto.setProjectId(id);

        ResponseEntity<List<EnvironmentDto>> result1 =
                environmentFeignClient.findBySearchRequest(searchRequestDto, false);
        Assert.assertEquals(result1.getStatusCode().value(), 200);
        Assert.assertTrue(result1.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<EnvironmentFullVer1ViewDto> result2 = environmentFeignClient.getEnvironment(id, true);
        Assert.assertEquals(result2.getStatusCode().value(), 200);
        Assert.assertTrue(result2.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<String> result3 = environmentFeignClient.getEnvironmentNameById(id);
        Assert.assertEquals(result3.getStatusCode().value(), 200);
        Assert.assertTrue(result3.getHeaders().get("Content-Type").contains("text/plain"));
    }

    @Pact(consumer = "atp-ram")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers1 = new HashMap<>();
        headers1.put("Content-Type", "application/json");

        Map<String, String> headers2 = new HashMap<>();
        headers2.put("Content-Type", "text/plain");

        DslPart environmentResObj = new PactDslJsonBody()
                .integerType("created")
                .uuid("createdBy")
                .stringType("description")
                .stringType("graylogName")
                .uuid("id")
                .integerType("modified")
                .uuid("modifiedBy")
                .stringType("name")
                .uuid("projectId")
                .array("systems").object().closeArray();
        DslPart environmentListRes = new PactDslJsonArray().template(environmentResObj);
        String searchRequest =
                "{\"ids\":[\"7c9dafe9-2cd1-4ffc-ae54-45867f2b9701\"], \"names\":[\"names\"], \"projectId\":\"7c9dafe9-2cd1-4ffc-ae54-45867f2b9701\"}";

        DslPart environmentFullVer1ViewRes = new PactDslJsonBody()
                .integerType("created")
                .uuid("createdBy")
                .stringType("description")
                .stringType("graylogName")
                .uuid("id")
                .integerType("modified")
                .uuid("modifiedBy")
                .stringType("name")
                .uuid("projectId")
                .array("systems").object().closeArray();

        PactDslResponse response = builder
                .given("all ok")
                .uponReceiving("POST /api/environments/search OK")
                .path("/api/environments/search")
                .query("full=false")
                .method("POST")
                .body(searchRequest)
                .willRespondWith()
                .status(200)
                .headers(headers1)
                .body(environmentListRes)

                .given("all ok")
                .uponReceiving("GET /api/environments/{environmentId} OK")
                .path("/api/environments/7c9dafe9-2cd1-4ffc-ae54-45867f2b9701")
                .query("full=true")
                .method("GET")
                .willRespondWith()
                .headers(headers1)
                .body(environmentFullVer1ViewRes)
                .status(200)

                .given("all ok")
                .uponReceiving("GET /api/environments/{environmentId}/name OK")
                .path("/api/environments/7c9dafe9-2cd1-4ffc-ae54-45867f2b9701/name")
                .method("GET")
                .willRespondWith()
                .headers(headers2)
                .body("name")
                .status(200);

        return response.toPact();
    }


    @Configuration
    public static class TestApp {

    }
}
