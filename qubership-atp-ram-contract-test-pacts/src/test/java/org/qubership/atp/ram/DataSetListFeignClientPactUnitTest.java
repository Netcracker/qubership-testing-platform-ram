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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.dataset.clients.dto.DatasetResponseDto;
import org.qubership.atp.ram.client.DataSetListFeignClient;
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
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.gson.Gson;

@EnableFeignClients(clients = {DataSetListFeignClient.class})
@ExtendWith(PactConsumerTestExt.class)
@SpringJUnitConfig(classes = {DataSetListFeignClientPactUnitTest.TestApp.class})
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, FeignConfiguration.class,
        FeignAutoConfiguration.class})
@TestPropertySource(
        properties = {"feign.atp.datasets.name=atp-datasets", "feign.atp.datasets.route=",
                "feign.atp.datasets.url=http://localhost:8888", "feign.httpclient.enabled=false"})
@PactTestFor(providerName = "atp-datasets", port = "8888", pactVersion = PactSpecVersion.V3)
public class DataSetListFeignClientPactUnitTest {
    @Configuration
    public static class TestApp {

    }
    @Autowired
    DataSetListFeignClient dataSetListFeignClient;

    @Test
    @PactTestFor(pactMethod = "createPact")
    public void allPass() {

        ResponseEntity<List<DatasetResponseDto>> expectedResult =
                dataSetListFeignClient.getDataSetsWithNameAndDataSetList(getListUuid());
        Assertions.assertEquals(200, expectedResult.getStatusCode().value());
        Assertions.assertTrue(Objects.requireNonNull(expectedResult.getHeaders().get("Content-Type"))
                .contains("application/json"));
    }

    @Pact(consumer = "atp-ram")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        DslPart result = new PactDslJsonBody()
                .uuid("dataSetId")
                .uuid("dataSetListId")
                .stringType("dataSetListName")
                .stringType("dataSetName")
                ;
        DslPart listResult = new PactDslJsonArray().template(result);

        PactDslResponse response = builder
                .given("all ok")
                .uponReceiving("POST /dsl/ds/all OK")
                .path("/dsl/ds/all")
                .method("POST")
                .body(new Gson().toJson(getListUuid()))
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(listResult);

        return response.toPact();
    }

    public List<UUID> getListUuid() {
        List<UUID> list = new ArrayList<>();
        UUID uuid = UUID.fromString("c2737427-05e4-4c17-8032-455539deaa01");
        UUID uuid2 = UUID.fromString("c2737427-05e4-4c17-8032-455539deaa02");
        list.add(uuid);
        list.add(uuid2);
        return list;
    }
    public UUID getUuid() {
        return UUID.fromString("c2737427-05e4-4c17-8032-455539deaa01");
    }
}
