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

package org.qubership.atp.ram.services;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.qubership.atp.ram.dto.response.ContextVariablesResponse;
import org.qubership.atp.ram.enums.ContextVariablesActiveTab;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import lombok.AllArgsConstructor;
import lombok.Data;

@ExtendWith(SpringExtension.class)
public class LogRecordContextVariableServiceTest {

    private static final String PARAM_NAME     = "name",      PARAM_NAME_BEFORE_VALUE     = "before_name",   PARAM_NAME_AFTER_VALUE     = "after_name",
                                ANOTHER_PARAM  = "param1",    ANOTHER_PARAM_BEFORE_VALUE  = "before_p1",     ANOTHER_PARAM_AFTER_VALUE  = "after_p1",
                                ONE_MORE_PARAM = "param2",    ONE_MORE_PARAM_BEFORE_VALUE = "before_p2",     ONE_MORE_PARAM_AFTER_VALUE = "after_p2";

    private List<ContextVariable> contextVariables;
    private ContextVariable nameContextVariable;
    private ContextVariable anotherContextVariable;
    private ContextVariable oneMoreContextVariable;

    @InjectMocks
    private LogRecordContextVariableService service;

    @BeforeEach
    public void setUp() {
        nameContextVariable     = new ContextVariable(PARAM_NAME,     PARAM_NAME_BEFORE_VALUE,     PARAM_NAME_AFTER_VALUE);
        anotherContextVariable  = new ContextVariable(ANOTHER_PARAM,  ANOTHER_PARAM_BEFORE_VALUE,  ANOTHER_PARAM_AFTER_VALUE);
        oneMoreContextVariable  = new ContextVariable(ONE_MORE_PARAM, ONE_MORE_PARAM_BEFORE_VALUE, ONE_MORE_PARAM_AFTER_VALUE);
        contextVariables = Arrays.asList(nameContextVariable, anotherContextVariable, oneMoreContextVariable);
    }

    /*
     * ------------------------------------------------------
     * | Pages              | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
     * | -------------------|-------------------------------|
     * | Not Modified (7)   | 3 | 3 | 1 |   |   |   |   |   |
     * | Modified     (4)   |   |   | 2 | 2 |   |   |   |   |
     * ------------------------------------------------------
     *                                           (3 per page)
     */
    @Test
    public void getPagedContextVariables_shouldReturnCorrectData_whenDataSplitPresentAndNoModifiedBigger() {
        List<ContextVariable> contextVariables = generateContextVariables(4, 7);
        List<ContextVariablesActiveTab> activeTabs = asList(ContextVariablesActiveTab.MODIFIED, ContextVariablesActiveTab.NOT_MODIFIED);
        int perPageSize = 3;

        assertPages(contextVariables, perPageSize, activeTabs, 4,
                new PageAssertRequest(0, 3, 0),
                new PageAssertRequest(1, 3, 0),
                new PageAssertRequest(2, 1, 2),
                new PageAssertRequest(3, 0, 2),
                new PageAssertRequest(4, 0, 0)
        );
    }

    /*
     * ------------------------------------------------------
     * | Pages              | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
     * | -------------------|-------------------------------|
     * | Not Modified (4)   | 3 | 1 |   |   |   |   |   |   |
     * | Modified     (7)   |   | 2 | 3 | 2 |   |   |   |   |
     * ------------------------------------------------------
     *                                           (3 per page)
     */
    @Test
    public void getPagedContextVariables_shouldReturnCorrectData_whenDataSplitPresentAndModifiedBigger() {
        List<ContextVariable> contextVariables = generateContextVariables(7, 4);
        List<ContextVariablesActiveTab> activeTabs = asList(ContextVariablesActiveTab.MODIFIED, ContextVariablesActiveTab.NOT_MODIFIED);
        int perPageSize = 3;

        assertPages(contextVariables, perPageSize, activeTabs, 4,
                new PageAssertRequest(0, 3, 0),
                new PageAssertRequest(1, 1, 2),
                new PageAssertRequest(2, 0, 3),
                new PageAssertRequest(3, 0, 2),
                new PageAssertRequest(4, 0, 0)
        );
    }

    /*
     * ------------------------------------------------------
     * | Pages              | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
     * | -------------------|-------------------------------|
     * | Not Modified (9)   | 2 | 2 | 2 | 2 | 1 |   |   |   |
     * ------------------------------------------------------
     *                                           (2 per page)
     */
    @Test
    public void getPagedContextVariables_shouldReturnCorrectData_whenDataSplitNotPresentAndModifiedAbsent() {
        List<ContextVariable> contextVariables = generateContextVariables(0, 9);
        List<ContextVariablesActiveTab> activeTabs = Collections.singletonList(ContextVariablesActiveTab.NOT_MODIFIED);
        int perPageSize = 2;

        assertPages(contextVariables, perPageSize, activeTabs, 5,
                new PageAssertRequest(0, 2, 0),
                new PageAssertRequest(1, 2, 0),
                new PageAssertRequest(2, 2, 0),
                new PageAssertRequest(3, 2, 0),
                new PageAssertRequest(4, 1, 0),
                new PageAssertRequest(5, 0, 0)
        );
    }

    /*
     * ------------------------------------------------------
     * | Pages              | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
     * | -------------------|-------------------------------|
     * | Modified (17)      | 5 | 5 | 5 | 2 |   |   |   |   |
     * ------------------------------------------------------
     *                                           (5 per page)
     */
    @Test
    public void getPagedContextVariables_shouldReturnCorrectData_whenDataSplitNotPresentAndNotModifiedAbsent() {
        List<ContextVariable> contextVariables = generateContextVariables(17, 0);
        List<ContextVariablesActiveTab> activeTabs = Collections.singletonList(ContextVariablesActiveTab.MODIFIED);
        int perPageSize = 5;

        assertPages(contextVariables, perPageSize, activeTabs, 4,
                new PageAssertRequest(0, 0, 5),
                new PageAssertRequest(1, 0, 5),
                new PageAssertRequest(2, 0, 5),
                new PageAssertRequest(3, 0, 2),
                new PageAssertRequest(4, 0, 0)
        );
    }

    /*
     * -------------------------------------------------------
     * | Pages               | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
     * | --------------------|-------------------------------|
     * | Not Modified (10)   | 3 | 3 | 3 | 1 |   |   |   |   |
     * | Modified     (10)   |   |   |   | 2 | 3 | 3 | 2 |   |
     * -------------------------------------------------------
     *                                            (3 per page)
     */
    @Test
    public void getPagedContextVariables_shouldReturnCorrectData_whenDataSplitPresentAndTabsSizeEqual() {
        List<ContextVariable> contextVariables = generateContextVariables(10, 10);
        List<ContextVariablesActiveTab> activeTabs = asList(ContextVariablesActiveTab.MODIFIED, ContextVariablesActiveTab.NOT_MODIFIED);
        int perPageSize = 3;

        assertPages(contextVariables, perPageSize, activeTabs, 7,
                new PageAssertRequest(0, 3, 0),
                new PageAssertRequest(1, 3, 0),
                new PageAssertRequest(2, 3, 0),
                new PageAssertRequest(3, 1, 2),
                new PageAssertRequest(4, 0, 3),
                new PageAssertRequest(5, 0, 3),
                new PageAssertRequest(6, 0, 2),
                new PageAssertRequest(7, 0, 0)
        );
    }

    @Test
    public void getPagedContextVariables_shouldReturnResponseWithTotalItemCount(){
        List<ContextVariable> allContextVariables = generateContextVariables(12, 7);
        int page = 0;
        int size = 15;
        List<ContextVariablesActiveTab> activeTabs = asList(ContextVariablesActiveTab.MODIFIED, ContextVariablesActiveTab.NOT_MODIFIED);
        ContextVariablesResponse response =
                service.getPagedContextVariables(allContextVariables, page, size, activeTabs);
        Assertions.assertEquals(allContextVariables.size(), response.getTotalItemCount());
    }

    @Test
    public void filterContextVariables_shouldReturnCorrectData_whenSearchParamsPresent() {
        final List<ContextVariable> searchContextVariables = Arrays.asList(anotherContextVariable, oneMoreContextVariable);
        final List<String> searchParameters = getParamNames(searchContextVariables);

        List<ContextVariable> result = service.filterContextVariables(contextVariables, searchParameters, null, null);

        assertSearch(searchContextVariables, result);
    }

    @Test
    public void filterContextVariables_shouldReturnCorrectData_whenSearchParamsAndBeforeValuePresent() {
        final List<ContextVariable> searchContextVariables = Arrays.asList(nameContextVariable, anotherContextVariable);
        final List<String> searchParameters = getParamNames(searchContextVariables);

        List<ContextVariable> result = service.filterContextVariables(contextVariables, searchParameters, "before", null);

        assertSearch(searchContextVariables, result);
    }
    @Test
    public void filterContextVariables_shouldReturnCorrectData_whenSearchParamsAndAfterValuePresent() {
        final List<ContextVariable> searchContextVariables = Collections.singletonList(oneMoreContextVariable);
        final List<String> searchParameters = getParamNames(searchContextVariables);

        List<ContextVariable> result = service.filterContextVariables(contextVariables, searchParameters, null, "after_p2");

        assertSearch(searchContextVariables, result);
    }

    @Test
    public void filterContextVariables_shouldReturnCorrectData_whenBeforeAndAfterValuePresents() {
        final List<ContextVariable> searchContextVariables = Collections.singletonList(anotherContextVariable);

        List<ContextVariable> result = service.filterContextVariables(contextVariables, null, "before_p1", "after_p1");

        assertSearch(searchContextVariables, result);
    }

    private List<String> getParamNames(List<ContextVariable> contextVariables) {
        return contextVariables.stream()
                .map(ContextVariable::getName)
                .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    static class PageAssertRequest {
        private int page;
        private int expNotModified;
        private int expModified;
    }

    private void assertSearch(List<ContextVariable> expected, List<ContextVariable> result) {
        Map<String, ContextVariable> resultMap = result.stream()
                .collect(Collectors.toMap(ContextVariable::getName, Function.identity()));

        Assertions.assertNotNull(resultMap);
        Assertions.assertEquals(expected.size(), resultMap.size());
        for (ContextVariable expectedVar : expected) {
            final String name = expectedVar.getName();
            Assertions.assertTrue(resultMap.containsKey(name));
            ContextVariable actualVar = resultMap.get(name);
            Assertions.assertEquals(expectedVar.getBeforeValue(), actualVar.getBeforeValue());
            Assertions.assertEquals(expectedVar.getAfterValue(), actualVar.getAfterValue());
        }
    }

    private void assertPages(List<ContextVariable> contextVariables,
                             int perPageSize,
                             List<ContextVariablesActiveTab> activeTabs,
                             int totalCount,
                             PageAssertRequest... requests) {
        for (PageAssertRequest request : requests) {
            ContextVariablesResponse page = service.getPagedContextVariables(contextVariables, request.page, perPageSize, activeTabs);
            assertPageResponse(page, totalCount, request.expNotModified, request.expModified);
        }
    }

    private void assertPageResponse(ContextVariablesResponse response,
                                    int expectedTotalCount,
                                    int expectedNotModifiedCount,
                                    int expectedModifiedCount) {
        Assertions.assertNotNull(response);
        final List<ContextVariable> modified = response.getModified();
        final List<ContextVariable> notModified = response.getNotModified();
        Assertions.assertNotNull(modified);
        Assertions.assertNotNull(notModified);
        Assertions.assertEquals(expectedModifiedCount, modified.size());
        Assertions.assertEquals(expectedNotModifiedCount, notModified.size());
        Assertions.assertEquals(expectedTotalCount, response.getTotalPageCount());
    }

    private List<ContextVariable> generateContextVariables(int modifiedCount, int notModifiedCount) {
        List<ContextVariable> result = new ArrayList<>();
        for (int i = 0; i < modifiedCount; i++) {
            String prefix = "m";
            String name = prefix + i;
            result.add(new ContextVariable(name, prefix, name));
        }
        for (int i = 0; i < notModifiedCount; i++) {
            String prefix = "nm";
            String name = prefix + i;
            result.add(new ContextVariable(name, name, name));
        }

        return result;
    }

}
