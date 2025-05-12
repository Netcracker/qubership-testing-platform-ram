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

package org.qubership.atp.ram.logging.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

public class ListUtilsTest {

    @Test
    public void mergeTwoListsWithoutDuplicates_BothListsAreEmpty_ShouldReturnEmptyList() {
        List<String> res = ListUtils.mergeTwoListsWithoutDuplicates(Collections.emptyList(), Collections.emptyList());
        Assertions.assertTrue(CollectionUtils.isEmpty(res), "Merging lists should be empty");
    }

    @Test
    public void mergeTwoListsWithoutDuplicates_ListIsNotEmpty_ShouldReturnMergedLists() {
        List<String> list1 = Arrays.asList("TA", "TA1");
        List<String> list2 = Arrays.asList("TA", "QA", "Test");
        List<String> actualList = ListUtils.mergeTwoListsWithoutDuplicates(list1, list2);
        Assertions.assertEquals( 4, actualList.size(), "Size of results list = 4");

        List<String> expList = Arrays.asList("TA", "TA1", "QA", "Test");
        Assertions.assertEquals(expList, actualList, "Merged lists are valid");
    }
}
