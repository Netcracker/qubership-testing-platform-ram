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

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.qubership.atp.ram.utils.ListUtils.applyPagination;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.qubership.atp.ram.dto.response.ContextVariablesResponse;
import org.qubership.atp.ram.enums.ContextVariablesActiveTab;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.springframework.stereotype.Service;

@Service
public class LogRecordContextVariableService {

    /**
     * Get context variables of logrecord.
     *
     * @param allContextVariables input context variables
     * @param page                number of page for pagination
     * @param size                size of 1 page for pagination
     * @return List of paginated context variables and total caount of context variables.
     */
    public ContextVariablesResponse getPagedContextVariables(List<ContextVariable> allContextVariables,
                                                             Integer page, Integer size,
                                                             List<ContextVariablesActiveTab> activeTabs) {

        ContextVariablesResponse response = new ContextVariablesResponse();
        ContextVariablesResponse splitVariables = splitContextVariables(allContextVariables);
        List<ContextVariable> modifiedVars = splitVariables.getModified();
        List<ContextVariable> notModifiedVars = splitVariables.getNotModified();

        int notModifiedCount = notModifiedVars.size();
        boolean isSizeSplit = notModifiedCount % size != 0;
        int sizeSplitPage = (int) Math.floor(notModifiedCount / (double) size);
        int shift = notModifiedCount % size;
        int totalPages = (int) Math.ceil(allContextVariables.size() / (double) size);

        response.setTotalPageCount(totalPages);
        response.setTotalItemCount(allContextVariables.size());

        ContextVariablesActiveTab modifiedTab = ContextVariablesActiveTab.MODIFIED;
        ContextVariablesActiveTab notModifiedTab = ContextVariablesActiveTab.NOT_MODIFIED;
        List<ContextVariablesActiveTab> allTabs = Arrays.asList(modifiedTab, notModifiedTab);

        if (activeTabs.containsAll(allTabs)) {
            List<ContextVariable> pagedNotModifiedVars = applyPagination(notModifiedVars, page, size, null);
            response.setNotModified(pagedNotModifiedVars);

            boolean applyModifiedPaging = isEmpty(pagedNotModifiedVars) || pagedNotModifiedVars.size() != size;
            if (applyModifiedPaging) {
                List<ContextVariable> pagedModifiedVars =
                        getPagedModifiedContextVariables(modifiedVars, page, size, shift, isSizeSplit, sizeSplitPage);

                response.setModified(pagedModifiedVars);
            }
        } else if (activeTabs.contains(modifiedTab)) {
            response.setModified(applyPagination(modifiedVars, page, size, null));
        } else if (activeTabs.contains(notModifiedTab)) {
            response.setNotModified(applyPagination(notModifiedVars, page, size, null));
        }

        return response;
    }

    private List<ContextVariable> getPagedModifiedContextVariables(List<ContextVariable> modifiedVars,
                                                                   int page,
                                                                   int size,
                                                                   int shift,
                                                                   boolean isSizeSplit,
                                                                   int sizeSplitPage) {
        if (isSizeSplit) {
            boolean isSizeSplitPage = page == sizeSplitPage;
            if (isSizeSplitPage) {
                return applyPagination(modifiedVars, page - sizeSplitPage, size - shift, null);
            } else {
                return applyPagination(modifiedVars, page - ++sizeSplitPage, size, size - shift);
            }
        } else {
            return applyPagination(modifiedVars, page - sizeSplitPage, size, null);
        }
    }

    private ContextVariablesResponse splitContextVariables(List<ContextVariable> allContextVariables) {
        ContextVariablesResponse response = new ContextVariablesResponse();

        if (isEmpty(allContextVariables)) {
            return response;
        }

        List<ContextVariable> notModified = response.getNotModified();
        List<ContextVariable> modified = response.getModified();

        allContextVariables.forEach(contextVariable -> {
            final String beforeValue = contextVariable.getBeforeValue();
            final String afterValue = contextVariable.getAfterValue();

            if (nonNull(beforeValue) && nonNull(afterValue) && beforeValue.equals(afterValue)) {
                notModified.add(contextVariable);
            } else {
                modified.add(contextVariable);
            }
        });

        Collections.sort(notModified);
        Collections.sort(modified);

        return response;
    }

    /**
     * Find and split context variable parameters into modified and not modified groups.
     *
     * @param contextVariables log record context variables
     * @param parameters parameter names
     * @param beforeValue before value
     * @param afterValue after value
     * @return result response
     */
    public ContextVariablesResponse filterAndSplitContextVariables(List<ContextVariable> contextVariables,
                                                                   List<String> parameters,
                                                                   String beforeValue,
                                                                   String afterValue) {
        ContextVariablesResponse response = splitContextVariables(contextVariables);

        final List<ContextVariable> filteredModifiedVariables =
                filterContextVariables(response.getModified(), parameters, beforeValue, afterValue);
        response.setModified(filteredModifiedVariables);

        final List<ContextVariable> filteredNotModifiedVariables =
                filterContextVariables(response.getNotModified(), parameters, beforeValue, afterValue);
        response.setNotModified(filteredNotModifiedVariables);

        return response;
    }

    /**
     * Filter context variable of logrecord by specified parameters.
     *
     * @param parameters  list of context variables names to filter by
     * @param beforeValue beforeValue to filter by using "contains" strategy
     * @param afterValue  afterValue to filter by using "contains" strategy
     * @return list of context variables that have passed all predicates.
     */
    public List<ContextVariable> filterContextVariables(List<ContextVariable> contextVariables,
                                                        List<String> parameters,
                                                        String beforeValue,
                                                        String afterValue) {
        if (contextVariables == null) {
            contextVariables = Collections.emptyList();
        } else {
            List<Predicate<ContextVariable>> predicates = new ArrayList<>();

            if (parameters != null && !parameters.isEmpty()) {
                Set<String> paramsSet = new HashSet<>(parameters);
                predicates.add(variable -> paramsSet.stream()
                        .anyMatch(param -> StringUtils.containsIgnoreCase(variable.getName(), param)));
            }
            if (StringUtils.isNotEmpty(beforeValue)) {
                predicates.add(variable ->
                        variable.getBeforeValue() != null
                                && StringUtils.containsIgnoreCase(variable.getBeforeValue(), beforeValue));
            }
            if (StringUtils.isNotEmpty(afterValue)) {
                predicates.add(variable ->
                        variable.getAfterValue() != null
                                && StringUtils.containsIgnoreCase(variable.getAfterValue(), afterValue));
            }
            contextVariables = contextVariables.stream()
                    .filter(variable -> predicates.stream()
                            .allMatch(predicate -> predicate.test(variable)))
                    .collect(Collectors.toList());
        }
        return contextVariables;
    }
}
