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
import static org.qubership.atp.ram.utils.StreamUtils.extractIds;
import static org.qubership.atp.ram.utils.StreamUtils.toIdEntityMap;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpIllegalNullableArgumentException;
import org.qubership.atp.auth.springbootstarter.utils.ExceptionUtils;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.exceptions.labeltemplates.RamLabelTemplateInconsistentLabelsPathGapException;
import org.qubership.atp.ram.exceptions.labeltemplates.RamLabelTemplateMultipleOneLevelNodeInclusionException;
import org.qubership.atp.ram.exceptions.labeltemplates.RamLabelTemplateWithoutChildrenNodesException;
import org.qubership.atp.ram.models.LabelTemplate;
import org.qubership.atp.ram.models.LabelTemplate.LabelTemplateNode;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.WidgetConfigTemplate;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabelTemplateNodeService {

    private final CatalogueService catalogueService;
    private final WidgetConfigTemplateService widgetConfigTemplateService;

    /**
     * Populate template with test runs.
     *
     * @param testRuns        test runs
     * @param labelTemplateId label template identifier
     * @return populated label template
     */
    public LabelTemplate populateLabelTemplateWithTestRuns(List<TestRun> testRuns,
                                                           UUID labelTemplateId) {
        LabelTemplate labelTemplate = this.getLabelTemplate(labelTemplateId);

        return populateLabelTemplateWithTestRuns(testRuns, labelTemplate);
    }

    /**
     * Populate template with test runs.
     *
     * @param testRuns      test runs
     * @param labelTemplate label template
     * @return populated label template
     */
    public LabelTemplate populateLabelTemplateWithTestRuns(List<TestRun> testRuns,
                                                           LabelTemplate labelTemplate) {
        Set<UUID> testRunTestCaseIds = extractIds(testRuns, TestRun::getTestCaseId);
        log.debug("Found test run test case references with ids '{}'", testRunTestCaseIds);

        // populate label template with test run ids for further conversion to tree model
        this.processLabelTemplateWithTestRuns(testRuns, labelTemplate);

        return labelTemplate;
    }

    /**
     * Get label template by specified identifier.
     *
     * @param labelTemplateId label template identifier
     * @return founded label template
     */
    public LabelTemplate getLabelTemplate(UUID labelTemplateId) {
        if (Objects.isNull(labelTemplateId)) {
            log.error("Found illegal nullable label template id");
            throw new AtpIllegalNullableArgumentException("label template id", "method parameter");
        }

        LabelTemplate labelTemplate = catalogueService.getLabelTemplateById(labelTemplateId);
        if (isEmpty(labelTemplate.getLabelNodes())) {
            log.error("Found label template '{}' without any children nodes", labelTemplateId);
            throw new RamLabelTemplateWithoutChildrenNodesException();
        }

        return labelTemplate;
    }

    public UUID getProjectIdByLabelTemplateId(UUID labelTemplateId) {
        LabelTemplate labelTemplate = catalogueService.getLabelTemplateById(labelTemplateId);
        return labelTemplate.getProjectId();
    }

    /**
     * Delete label template by specified identifier.
     *
     * @param labelTemplateId label template identifier
     */
    public void deleteLabelTemplate(UUID labelTemplateId) {
        log.info("Delete validation label config template '{}'", labelTemplateId);
        catalogueService.deleteLabelTemplateById(labelTemplateId);

        List<WidgetConfigTemplate> referencedWidgetConfigTemplates =
                widgetConfigTemplateService.getWidgetConfigTemplatesWithLabelTemplateId(labelTemplateId);

        referencedWidgetConfigTemplates.stream()
                .flatMap(template -> template.getWidgets()
                        .stream()
                        .filter(widgetConfig -> nonNull(widgetConfig.getLabelTemplateId())))
                .filter(widgetConfig -> labelTemplateId.equals(widgetConfig.getLabelTemplateId()))
                .forEach(widgetConfig -> widgetConfig.setLabelTemplateId(null));

        widgetConfigTemplateService.updateAll(referencedWidgetConfigTemplates);
    }

    private void processLabelTemplateWithTestRuns(List<TestRun> testRuns,
                                                  LabelTemplate labelTemplate) {
        UUID labelTemplateId = labelTemplate.getUuid();

        log.debug("Process label template with id '{}' with test runs '{}'", labelTemplateId, extractIds(testRuns));
        // determine label template node for each test run
        this.defineLabelTemplateNodeForRuns(testRuns, labelTemplate.createUnknownNode());
    }

    /**
     * Find and insert test runs into label template nodes.
     *
     * @param testRuns      test runs
     * @param labelTemplate label template
     */
    private void defineLabelTemplateNodeForRuns(List<TestRun> testRuns,
                                                LabelTemplate labelTemplate) {
        testRuns.forEach(testRun -> {
            final UUID testRunId = testRun.getUuid();
            log.info("Defining label template node for test run '{}'", testRunId);
            UUID testCaseId = testRun.getTestCaseId();
            Set<UUID> caseLabelIds = testRun.getLabelIds();
            if (isEmpty(caseLabelIds)) {
                log.warn("Found test case '{}' without labels", testCaseId);
                labelTemplate.addToUnknown(testRunId, "Referenced test case without labels");
            } else {
                log.info("Found referenced test case '{}' with labels '{}' for test run '{}'",
                        testCaseId, caseLabelIds, testRunId);

                List<LabelTemplateNode> labelNodes = labelTemplate.getLabelNodes();
                LabelTemplateNode unknownNode = labelTemplate.getUnknownNode();

                this.defineLabelTemplateNodeForRun(testRun, caseLabelIds, labelTemplate, unknownNode, labelNodes);
            }
        });
        Map<UUID, TestRun> testRunMap = toIdEntityMap(testRuns);
        calculateRatesForLabelNodes(labelTemplate.getLabelNodes(), testRunMap);
    }

    /**
     * Calculate rates for label nodes.
     *
     * @param labelNodes label nodes
     */
    public void calculateRatesForLabelNodes(List<LabelTemplateNode> labelNodes,
                                            Map<UUID, TestRun> testRunMap) {
        final Predicate<TestRun> calcSkipPredicate = testRun ->
                !TestingStatuses.SKIPPED.equals(testRun.getTestingStatus());

        labelNodes.forEach(labelTemplateNode -> {
            int passedCount = 0;
            int warningCount = 0;
            int failedCount = 0;
            int childrenTestRunsSum = 0;

            List<LabelTemplateNode> children = labelTemplateNode.getChildren();
            if (!isEmpty(children)) {
                calculateRatesForLabelNodes(children, testRunMap);
                passedCount = children.stream().mapToInt(LabelTemplateNode::getTestRunPassedCount).sum();
                warningCount = children.stream().mapToInt(LabelTemplateNode::getTestRunWarnedCount).sum();
                failedCount = children.stream().mapToInt(LabelTemplateNode::getTestRunFailedCount).sum();
                childrenTestRunsSum = children.stream().mapToInt(LabelTemplateNode::getTestRunCount).sum();
            }

            int totalTestRuns = childrenTestRunsSum;

            Set<UUID> testRunsIds = labelTemplateNode.getTestRunIds();
            if (!isEmpty(testRunsIds)) {
                List<TestRun> testRuns = StreamUtils.getEntitiesFromMap(testRunsIds, testRunMap)
                        .stream()
                        .filter(calcSkipPredicate)
                        .collect(Collectors.toList());

                totalTestRuns += testRuns.size();

                passedCount += testRuns.stream()
                        .filter(testRun -> TestingStatuses.PASSED.equals(testRun.getTestingStatus()))
                        .count();
                warningCount += testRuns.stream()
                        .filter(testRun -> TestingStatuses.WARNING.equals(testRun.getTestingStatus()))
                        .count();
                failedCount += testRuns.stream()
                        .filter(testRun -> TestingStatuses.FAILED.equals(testRun.getTestingStatus()))
                        .count();
            }

            int failedRate = (int) (Math.round((double) failedCount / totalTestRuns * 100));
            labelTemplateNode.setFailedRate(failedRate);
            labelTemplateNode.setTestRunFailedCount(failedCount);

            int warningRate = (int) (Math.round((double) warningCount / totalTestRuns * 100));
            labelTemplateNode.setWarningRate(warningRate);
            labelTemplateNode.setTestRunWarnedCount(warningCount);

            int passedRate = (int) (Math.round((double) passedCount / totalTestRuns * 100));
            labelTemplateNode.setPassedRate(passedRate);
            labelTemplateNode.setTestRunPassedCount(passedCount);

            labelTemplateNode.setTestRunCount(totalTestRuns);
        });
    }

    private void defineLabelTemplateNodeForRun(TestRun testRun,
                                               Set<UUID> labelIds,
                                               LabelTemplate labelTemplate,
                                               LabelTemplateNode rootLabelNode,
                                               List<LabelTemplateNode> labelNodes) {
        UUID testRunId = testRun.getUuid();
        try {
            log.debug("Validate multiple one level node inclusions for test run '{}'", testRunId);
            this.validateMultipleOneLevelNodeInclusions(labelNodes, labelIds);
        } catch (RamLabelTemplateMultipleOneLevelNodeInclusionException e) {
            log.error("Failed to define label template node for test run: {}. Reason: {}", testRunId, e);
            labelTemplate.addToUnknown(testRunId, e.getMessage());
            return;
        }

        if (isEmpty(labelNodes)) {
            log.debug("Add test run '{}' into label node '{}'", testRunId, rootLabelNode.getLabelId());
            rootLabelNode.addTestRun(testRunId);
        } else {
            Optional<LabelTemplateNode> foundedLabelNode = labelNodes.stream()
                    .filter(node -> labelIds.contains(node.getLabelId()))
                    .findFirst();

            if (foundedLabelNode.isPresent()) {
                LabelTemplateNode labelNode = foundedLabelNode.get();
                List<LabelTemplateNode> labelNodeChildren = labelNode.getChildren();
                this.defineLabelTemplateNodeForRun(testRun, labelIds, labelTemplate, labelNode, labelNodeChildren);
            } else {
                try {
                    log.debug("Validate test run '{}' for labels path gap", testRunId);
                    this.validateForLabelsPathGap(labelIds, labelNodes);
                    log.debug("Add test run '{}' into label node '{}'", testRunId, rootLabelNode.getLabelId());
                    rootLabelNode.addTestRun(testRunId);
                } catch (RamLabelTemplateInconsistentLabelsPathGapException e) {
                    log.error("Failed to define label template node for test run: {}. Reason: {}", testRunId, e);
                    labelTemplate.addToUnknown(testRunId, e.getMessage());
                }
            }
        }
    }

    /**
     * Validate if there are more than one label node intersections in one tree level. For example:*
     * </p>
     * TestRun with labels ['Offline', 'Online'] will fail because corresponding label nodes located on the same level
     * in template tree.
     * </p>
     * Template
     * Offline
     * Data National
     * New
     * Online
     *
     * @param labelNodes current level tree nodes
     * @param labelIds   test run -> test case label ids
     * @throws IllegalStateException if more than one label node intersections present
     */
    private void validateMultipleOneLevelNodeInclusions(List<LabelTemplateNode> labelNodes,
                                                        Set<UUID> labelIds) throws IllegalStateException {
        if (!isEmpty(labelNodes)) {
            Set<UUID> labelNodeIds = extractIds(labelNodes, LabelTemplateNode::getLabelId);
            labelNodeIds.retainAll(labelIds);

            if (labelNodeIds.size() > 1) {
                String inclusionNames = labelNodes.stream()
                        .filter(labelNode -> labelNodeIds.contains(labelNode.getLabelId()))
                        .map(LabelTemplateNode::getLabelName)
                        .collect(Collectors.joining(", "));

                ExceptionUtils.throwWithLog(log,
                        new RamLabelTemplateMultipleOneLevelNodeInclusionException(inclusionNames));
            }
        }
    }

    private void validateForLabelsPathGap(Set<UUID> labelIds,
                                          List<LabelTemplateNode> labelNodes) throws IllegalStateException {
        Optional<LabelTemplateNode> foundedLabelNode = labelNodes.stream()
                .filter(node -> labelIds.contains(node.getLabelId()))
                .findFirst();

        if (foundedLabelNode.isPresent()) {
            ExceptionUtils.throwWithLog(log, new RamLabelTemplateInconsistentLabelsPathGapException());
        } else {
            List<LabelTemplateNode> currentNotEmptyChildrenNodes = labelNodes.stream()
                    .filter(node -> !isEmpty(node.getChildren()))
                    .collect(Collectors.toList());

            for (LabelTemplateNode node : currentNotEmptyChildrenNodes) {
                this.validateForLabelsPathGap(labelIds, node.getChildren());
            }
        }
    }
}
