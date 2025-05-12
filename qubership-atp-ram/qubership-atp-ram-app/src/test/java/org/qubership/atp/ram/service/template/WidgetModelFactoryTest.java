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

package org.qubership.atp.ram.service.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.models.WidgetType;
import org.qubership.atp.ram.service.mail.ReportParams;

public class WidgetModelFactoryTest {

    WidgetModelFactory widgetModelFactory;

    @BeforeEach
    public void setUp() {
        widgetModelFactory = new WidgetModelFactory(generateListOfBuilders());
    }

    private List<WidgetModelBuilder> generateListOfBuilders() {
        return new ArrayList<WidgetModelBuilder>() {{
            add(generateBuilder(WidgetType.EXECUTION_SUMMARY));
            add(generateBuilder(WidgetType.ENVIRONMENTS_INFO));
            add(generateBuilder(WidgetType.SUMMARY));
        }};
    }

    private WidgetModelBuilder generateBuilder(WidgetType widgetType) {
        return new WidgetModelBuilder() {
            @Override
            public Map<String, Object> getModel(ReportParams reportParams) {
                throw new UnsupportedOperationException();
            }

            @Override
            public WidgetType getType() {
                return widgetType;
            }
        };
    }

    @Test
    public void onWidgetModelFacory_getModelBuilderByWidgetType_ModelBuilderOfCorrectWidgetTypeReturned() {
        Optional<WidgetModelBuilder> modelBuilder = widgetModelFactory.getModelBuilder(WidgetType.EXECUTION_SUMMARY);

        Assertions.assertTrue(modelBuilder.isPresent());
        Assertions.assertEquals(WidgetType.EXECUTION_SUMMARY, modelBuilder.get().getType());
    }

    @Test
    public void onWidgetModelFacory_getNotImplementedModelBuilderByWidgetType_EmptyOptionalModelBuilderReturned() {
        Optional<WidgetModelBuilder> modelBuilder = widgetModelFactory.getModelBuilder(WidgetType.ROOT_CAUSES_STATISTIC);
        Assertions.assertFalse(modelBuilder.isPresent());
    }
}
