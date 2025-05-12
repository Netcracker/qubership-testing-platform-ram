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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.qubership.atp.ei.node.constants.Constant;
import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ExportScope;
import org.qubership.atp.ei.node.services.FileService;
import org.qubership.atp.ei.node.services.ObjectSaverToDiskService;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.repositories.FailPatternRepository;
import org.qubership.atp.ram.repositories.RootCauseRepository;
import org.qubership.atp.ram.services.RootCauseService;
import org.qubership.atp.ram.testdata.FailPatternMock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
public class AtpRamExportExecutorTest {

    private static final String DEFAULT_RESOURCES_PATH = "src/test/resources";

    private RootCauseService rootCauseService;
    private RootCauseRepository rootCauseRepository;
    private FailPatternRepository failPatternRepository;
    private final ObjectSaverToDiskService objectSaverToDiskService = new ObjectSaverToDiskService(new FileService(), false);

    private AtpRamExportExecutor exportExecutor;

    private UUID projectId;

    @Before
    public void setUpAtpExport() {
        projectId = UUID.randomUUID();
        rootCauseService = mock(RootCauseService.class);
        rootCauseRepository = mock(RootCauseRepository.class);
        failPatternRepository = mock(FailPatternRepository.class);
        exportExecutor = new AtpRamExportExecutor(rootCauseService, rootCauseRepository, failPatternRepository,
                objectSaverToDiskService);
    }

    @After
    public void after() throws IOException {
        FileUtils.deleteDirectory(getPath(RootCause.class.getSimpleName()).toFile());
        FileUtils.deleteDirectory(getPath(FailPattern.class.getSimpleName()).toFile());
    }

    /*
    fail reasons hierarchy:
        globalTopLevelRootCause
            customChildRootCause
                customCustomChildRootCause
        customTopLevelRootCause
     */
    @Test
    public void exportData_exportedCorrectly() {
        // given
        RootCause globalTopLevelRootCause = FailPatternMock.newRootCause(UUID.randomUUID(),
                "globalTopLevelRootCause");
        RootCause customChildRootCause = FailPatternMock.newRootCause(UUID.randomUUID(),
                "customChildRootCause");
        customChildRootCause.setParentId(globalTopLevelRootCause.getUuid());
        RootCause customCustomChildRootCause = FailPatternMock.newRootCause(UUID.randomUUID(),
                "customCustomChildRootCause");
        customChildRootCause.setParentId(customChildRootCause.getUuid());
        RootCause customTopLevelRootCause = FailPatternMock.newRootCause(UUID.randomUUID(),
                "customTopLevelRootCause");
        customTopLevelRootCause.setProjectId(projectId);
        FailPattern failPattern = FailPatternMock.newFailPattern(UUID.randomUUID(), "myFailPattern");
        ExportImportData exportData = new ExportImportData(projectId, new ExportScope(),
                ExportFormat.ATP);
        exportData.getExportScope().getEntities().put(ServiceScopeEntities.ENTITY_RAM_FAIL_REASONS.getValue(),
                Sets.newHashSet(
                        globalTopLevelRootCause.getUuid().toString(),
                        customChildRootCause.getUuid().toString(),
                        customCustomChildRootCause.getUuid().toString(),
                        customTopLevelRootCause.getUuid().toString()
                ));
        exportData.getExportScope().getEntities().put(ServiceScopeEntities.ENTITY_RAM_FAIL_PATTERNS.getValue(),
                Sets.newHashSet(failPattern.getUuid().toString()));
        exportData.getExportScope().getEntities().put(Constant.ENTITY_PROJECTS, Sets.newHashSet(projectId.toString()));
        // when
        when(rootCauseRepository.findAllByParentIdIsNullAndType(any()))
                .thenReturn(Collections.singletonList(globalTopLevelRootCause));
        when(rootCauseRepository.findAllByParentIdIsNullAndProjectIdAndType(any(), any()))
                .thenReturn(Collections.singletonList(customTopLevelRootCause));
        when(rootCauseService.getChildrenRootCauses(eq(globalTopLevelRootCause), any()))
                .thenReturn(Collections.singletonList(customChildRootCause));
        when(rootCauseService.getChildrenRootCauses(eq(customChildRootCause), any()))
                .thenReturn(Collections.singletonList(customCustomChildRootCause));
        when(failPatternRepository.findAllByProjectId(any())).thenReturn(Collections.singletonList(failPattern));
        exportExecutor.exportToFolder(exportData, getRootPath());
        // then
        List<RootCause> expectedRootCauses = Arrays.asList(
                globalTopLevelRootCause, customCustomChildRootCause, customChildRootCause, customTopLevelRootCause);
        List<RootCause> actualRootCauses = getActualRootCauses();
        assertTrue(CollectionUtils.isEqualCollection(expectedRootCauses, actualRootCauses));

        List<FailPattern> actualFailPatterns = getActualFailPatterns();
        assertEquals(1, actualFailPatterns.size());
        assertEquals(failPattern, actualFailPatterns.get(0));
    }

    private List<RootCause> getActualRootCauses() {
        String dirName = RootCause.class.getSimpleName();
        File dir = getPath(dirName).toFile();
        File[] files = dir.listFiles();
        return Arrays.stream(files)
                .map(file -> readObjectFromFilePath(RootCause.class, dirName, file.getName()))
                .collect(Collectors.toList());
    }

    private List<FailPattern> getActualFailPatterns() {
        String dirName = FailPattern.class.getSimpleName();
        File dir = getPath(dirName).toFile();
        File[] files = dir.listFiles();
        return Arrays.stream(files)
                .map(file -> readObjectFromFilePath(FailPattern.class, dirName, file.getName()))
                .collect(Collectors.toList());
    }

    public <T> T readObjectFromFilePath(Class<T> type, String... paths) {
        try {
            byte[] bytes = Files.readAllBytes(getPath(paths));
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(bytes, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Path getPath(String... path) {
        return Paths.get(getRootFilePath(), path);
    }

    private String getRootFilePath() {
        return getRootFile().getPath();
    }

    private File getRootFile() {
        return getRootPath().toFile();
    }

    public Path getRootPath() {
        String[] allSegments = AtpRamExportExecutorTest.class.getName().split("[.]");
        return Paths.get(getDefaultPath().toString(), allSegments);
    }

    public Path getDefaultPath() {
        return Paths.get(DEFAULT_RESOURCES_PATH);
    }
}
