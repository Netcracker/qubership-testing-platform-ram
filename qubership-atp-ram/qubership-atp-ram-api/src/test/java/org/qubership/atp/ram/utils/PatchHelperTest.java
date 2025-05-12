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

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import lombok.Data;

public class PatchHelperTest {

    private static final PatchHelper patchHelper = new PatchHelper();
    private static final String STRING_PROPERTY_TO_UPDATE = "String property";
    private static final int INT_PROPERTY_TO_UPDATE = Integer.MAX_VALUE;
    private static final String INT_PROPERTY_NAME = "intProperty";
    private static final List<String> LIST_PROPERTY_TO_UPDATE = asList(STRING_PROPERTY_TO_UPDATE);
    private static final Map<String, String> MAP_PROPERTY_TO_UPDATE = new HashMap<String, String>() {
        {
            put(STRING_PROPERTY_TO_UPDATE, STRING_PROPERTY_TO_UPDATE);
        }
    };

    @Test
    public void partialUpdate_propertyIsUpdatedInTarget() {
        TestPojo source = new TestPojo();
        TestPojo target = new TestPojo();
        source.setStringProperty(STRING_PROPERTY_TO_UPDATE);
        patchHelper.partialUpdate(source, target);
        assertEquals(STRING_PROPERTY_TO_UPDATE, target.getStringProperty(), "Property name should be updated in target");
    }

    @Test
    public void partialUpdate_propertyIsUnsetIfNullIsProvidedAndNonePropertiesAreIgnored() {
        TestPojo source = new TestPojo();
        TestPojo target = new TestPojo();
        target.setStringProperty(STRING_PROPERTY_TO_UPDATE);
        patchHelper.partialUpdate(source, target);
        assertNull(target.getStringProperty(), "Property name should be set to null");
    }

    @Test
    public void partialUpdate_propertyIsNotUpdatedIfNullFieldsShouldBeIgnoreDuringPatching() {
        TestPojo source = new TestPojo();
        TestPojo target = new TestPojo();
        target.setStringProperty(STRING_PROPERTY_TO_UPDATE);
        patchHelper.partialUpdate(source, target, PatchHelper.nullProperties);
        assertEquals(STRING_PROPERTY_TO_UPDATE, target.getStringProperty(), "Property name should be set");
    }

    @Test
    public void partialUpdate_emptyListsShouldBeIgnoredIfThisFilterIsSpecified() {
        TestPojo source = new TestPojo();
        TestPojo target = new TestPojo();
        target.setCollectionProperty(LIST_PROPERTY_TO_UPDATE);
        patchHelper.partialUpdate(source, target, PatchHelper.emptyCollectionsProperties);
        assertEquals(LIST_PROPERTY_TO_UPDATE, target.getCollectionProperty(),
                "Collection property should not be updated");
    }

    @Test
    public void partialUpdate_emptyMapsShouldBeIgnoredIfThisFilterIsSpecified() {
        TestPojo source = new TestPojo();
        TestPojo target = new TestPojo();
        target.setMapProperty(MAP_PROPERTY_TO_UPDATE);
        patchHelper.partialUpdate(source, target, PatchHelper.emptyMapsPropertiesFilter);
        assertEquals(MAP_PROPERTY_TO_UPDATE, target.getMapProperty(),
                "Map property should not be updated");
    }

    @Test
    public void partialUpdate_providedPropertiesShouldBeIgnored() {
        TestPojo source = new TestPojo();
        TestPojo target = new TestPojo();
        target.setIntProperty(INT_PROPERTY_TO_UPDATE);
        patchHelper.partialUpdate(source, target, patchHelper.getConcretePropertiesFilter(asList(INT_PROPERTY_NAME)));
        assertEquals(INT_PROPERTY_TO_UPDATE, target.getIntProperty(),
                "Int property should not be updated");
    }

    @Data
    static class TestPojo {

        private String stringProperty;
        private int intProperty;
        private Collection<String> collectionProperty;
        private Map<String, String> mapProperty;
    }
}
