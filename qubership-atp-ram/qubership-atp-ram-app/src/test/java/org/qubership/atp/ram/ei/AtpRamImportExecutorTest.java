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

package org.qubership.atp.ram.ei;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.qubership.atp.ei.node.constants.Constant;
import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ExportScope;
import org.qubership.atp.ei.node.dto.ValidationResult;
import org.qubership.atp.ei.node.services.FileService;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.qubership.atp.ei.node.services.ObjectSaverToDiskService;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.models.RamObject;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.repositories.FailPatternRepository;
import org.qubership.atp.ram.repositories.RootCauseRepository;
import org.qubership.atp.ram.services.RootCauseService;
import org.qubership.atp.ram.testdata.FailPatternMock;

import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
public class AtpRamImportExecutorTest {

    @Mock
    private RootCauseRepository rootCauseRepository;

    @Mock
    private FailPatternRepository failPatternRepository;

    @Mock
    private RootCauseService rootCauseService;

    private final ObjectSaverToDiskService objectSaverToDiskService = new ObjectSaverToDiskService(new FileService(), false);
    private final ObjectLoaderFromDiskService objectLoaderFromDiskService = new ObjectLoaderFromDiskService();
    private AtpRamImportExecutor importExecutor;
    private AtpRamExportExecutor exportExecutor;

    private UUID projectId;

    private static final String DEFAULT_RESOURCES_PATH = "src/test/resources";

    private List<RootCause> rootCauses;
    private RootCause globalTopLevelRootCause;
    private RootCause customChildRootCause;
    private RootCause customCustomChildRootCause;
    private RootCause customTopLevelRootCause;
    private List<FailPattern> failPatterns;

    @Before
    public void setUp() {
        importExecutor = new AtpRamImportExecutor(
                rootCauseRepository, rootCauseService, failPatternRepository, objectLoaderFromDiskService);
        exportExecutor = new AtpRamExportExecutor(rootCauseService, rootCauseRepository, failPatternRepository,
                objectSaverToDiskService);
        projectId = UUID.randomUUID();
        globalTopLevelRootCause = FailPatternMock.newRootCause(UUID.randomUUID(),
                "globalTopLevelRootCause");
        customChildRootCause = FailPatternMock.newRootCause(UUID.randomUUID(),
                "customChildRootCause");
        customChildRootCause.setParentId(globalTopLevelRootCause.getUuid());
        customCustomChildRootCause = FailPatternMock.newRootCause(UUID.randomUUID(),
                "customCustomChildRootCause");
        customChildRootCause.setParentId(customChildRootCause.getUuid());
        customTopLevelRootCause = FailPatternMock.newRootCause(UUID.randomUUID(),
                "customTopLevelRootCause");
        customTopLevelRootCause.setProjectId(projectId);
        rootCauses = Arrays.asList(
                globalTopLevelRootCause,
                customChildRootCause,
                customCustomChildRootCause,
                customTopLevelRootCause
        );
        FailPattern failPattern = FailPatternMock.newFailPattern(UUID.randomUUID(), "myFailPattern");
        failPattern.setProjectId(projectId);
        failPatterns = Collections.singletonList(failPattern);
    }

    @After
    public void clear() throws IOException {
        FileUtils.deleteDirectory(getRootPath().toFile());
    }

    @Test
    public void importData_importedCorrectly() throws RuntimeException {
        // given
        ExportImportData exportData = new ExportImportData(projectId, new ExportScope(),
                ExportFormat.ATP);
        exportData.getExportScope().getEntities().put(Constant.ENTITY_PROJECTS, Sets.newHashSet(projectId.toString()));
        exportData.getExportScope().getEntities().put(ServiceScopeEntities.ENTITY_RAM_FAIL_REASONS.getValue(),
                rootCauses.stream().map(entity -> entity.getUuid().toString()).collect(Collectors.toSet()));
        exportData.getExportScope().getEntities().put(ServiceScopeEntities.ENTITY_RAM_FAIL_PATTERNS.getValue(),
                failPatterns.stream().map(entity -> entity.getUuid().toString()).collect(Collectors.toSet()));
        // when
        createExportDir(exportData);
        importExecutor.importData(exportData, getRootPath());
        // then
        ArgumentCaptor<RootCause> rootCauseArgumentCaptor = ArgumentCaptor.forClass(RootCause.class);
        ArgumentCaptor<FailPattern> failPatternArgumentCaptor = ArgumentCaptor.forClass(FailPattern.class);
        verify(rootCauseRepository, times(4)).save(rootCauseArgumentCaptor.capture());
        verify(failPatternRepository, times(1)).save(failPatternArgumentCaptor.capture());
        List<RootCause> actualRootCauses = rootCauseArgumentCaptor.getAllValues();
        compareListsByAttributes(rootCauses, actualRootCauses);
        List<FailPattern> actualFailPatterns = failPatternArgumentCaptor.getAllValues();
        compareListsByAttributes(failPatterns, actualFailPatterns);
    }

    @Test
    public void validateTest() throws Exception {
        // given
        ExportImportData exportData = new ExportImportData(projectId, new ExportScope(),
                ExportFormat.ATP, false, true, UUID.randomUUID(),
                new HashMap<>(), new HashMap<>(), null, false);
        exportData.getExportScope().getEntities().put(Constant.ENTITY_PROJECTS, Sets.newHashSet(projectId.toString()));
        exportData.getExportScope().getEntities().put(ServiceScopeEntities.ENTITY_RAM_FAIL_REASONS.getValue(),
                rootCauses.stream().map(entity -> entity.getUuid().toString()).collect(Collectors.toSet()));
        exportData.getExportScope().getEntities().put(ServiceScopeEntities.ENTITY_RAM_FAIL_PATTERNS.getValue(),
                failPatterns.stream().map(entity -> entity.getUuid().toString()).collect(Collectors.toSet()));
        // when
        createExportDir(exportData);
        ValidationResult result = importExecutor.validateData(exportData, getRootPath());
        // then
        assertEquals(5, result.getReplacementMap().size());
        assertTrue(result.isValid());
        assertNotNull(result.getDetails());
    }

    public Path getRootPath() {
        return Paths.get(DEFAULT_RESOURCES_PATH, "exportImportData");
    }

    private void createExportDir(ExportImportData data) {
        when(rootCauseRepository.findAllByParentIdIsNullAndType(any()))
                .thenReturn(Collections.singletonList(globalTopLevelRootCause));
        when(rootCauseRepository.findAllByParentIdIsNullAndProjectIdAndType(any(), any()))
                .thenReturn(Collections.singletonList(customTopLevelRootCause));
        when(rootCauseService.getChildrenRootCauses(eq(globalTopLevelRootCause), any()))
                .thenReturn(Collections.singletonList(customChildRootCause));
        when(rootCauseService.getChildrenRootCauses(eq(customChildRootCause), any()))
                .thenReturn(Collections.singletonList(customCustomChildRootCause));
        when(failPatternRepository.findAllByProjectId(any())).thenReturn(failPatterns);
        exportExecutor.exportToFolder(data, getRootPath());
    }

    private <T extends RamObject> void compareListsByAttributes(List<T> expectedList, List<T> actualList) {
        Map<UUID, T> expectedListMap = expectedList.stream()
                .collect(Collectors.toMap(
                        T::getUuid,
                        Function.identity()
                ));
        for (T actualObject : actualList) {
            T expectedObject = expectedListMap.get(actualObject.getUuid());
            assertThat(actualObject)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedObject);
        }
    }
}
