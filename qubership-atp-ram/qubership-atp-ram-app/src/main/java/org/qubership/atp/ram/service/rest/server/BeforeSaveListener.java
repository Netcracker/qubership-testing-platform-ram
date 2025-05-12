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

package org.qubership.atp.ram.service.rest.server;

import java.util.UUID;

import org.qubership.atp.ram.models.RamObject;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

@Component
public class BeforeSaveListener extends AbstractMongoEventListener<RamObject> {

    /**
     * Generates uuid for {@link RamObject} before saving.
     *
     * @param event save event
     */
    @Override
    public void onBeforeConvert(BeforeConvertEvent<RamObject> event) {
        final RamObject source = event.getSource();
        if (source.getUuid() == null) {
            source.setUuid(UUID.randomUUID());
        }
    }
}
