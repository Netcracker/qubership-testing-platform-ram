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

package org.qubership.atp.ram.utils;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;


public class JsonHelperTest {

    @Test
    public void getUuidValue_SetNullKey_ShouldReturnNull() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("key", "key");
        Assertions.assertNull(JsonHelper.getUuidValue(jsonObject, "key1"),"Value by invalid key is NULL");
    }

    @Test
    public void getUuidValue_SetUuid_ShouldReturnUuid() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("key", "1ccf9445-095e-4022-bc02-041d4fbc5773");
        UUID expUuid = UUID.fromString("1ccf9445-095e-4022-bc02-041d4fbc5773");
        Assertions.assertEquals( expUuid, JsonHelper.getUuidValue(jsonObject, "key"), "Value by valid key is UUID");
    }
}
