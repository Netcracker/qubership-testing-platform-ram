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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.RootCauseMock;
import org.qubership.atp.ram.exceptions.rootcauses.RamRootCauseAlreadyExistsException;
import org.qubership.atp.ram.exceptions.rootcauses.RamRootCauseIllegalAccessException;
import org.qubership.atp.ram.model.request.RootCauseUpsertRequest;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.RootCauseTreeNode;
import org.qubership.atp.ram.models.RootCauseType;
import org.qubership.atp.ram.models.TreeNode;
import org.qubership.atp.ram.repositories.RootCauseRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;

@ExtendWith(SpringExtension.class)
public class RootCauseServiceTest {

    private RootCauseService service;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private RootCauseRepository repository;

    @Mock
    private TestRunService testRunService;

    @Captor
    ArgumentCaptor<RootCause> argCaptor;

    @BeforeEach
    public void setUp() {
        this.service = new RootCauseService(repository, testRunService, new ModelMapper());
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void save_validGlobalTypeRootCauseRequestData_shouldBeSuccessfullySaved() {
        // given
        RootCause savedRootCause = new RootCause();
        savedRootCause.setName("Migration Issue");
        savedRootCause.setType(RootCauseType.GLOBAL);

        // when
        service.save(savedRootCause);

        // then
        verify(repository).save(argCaptor.capture());

        RootCause capturedSavedRootCause = argCaptor.getValue();

        assertNotNull(capturedSavedRootCause);
        assertEquals(savedRootCause.getName(), capturedSavedRootCause.getName(), "Root cause name shouldn't change during save");
        assertEquals(savedRootCause.getType(), capturedSavedRootCause.getType(), "Root cause type shouldn't change during save");
    }

    @Test
    public void create_invalidRcWithCustomTypeAndExistedName_exceptionExpected() {

        // given
        RootCauseUpsertRequest request = new RootCauseUpsertRequest();
        request.setName("Migration Issue");
        request.setType(RootCauseType.CUSTOM);
        request.setProjectId(UUID.randomUUID());

        RootCause existedRootCause = new RootCause();
        existedRootCause.setUuid(UUID.randomUUID());
        existedRootCause.setName(request.getName());
        existedRootCause.setProjectId(request.getProjectId());

        when(repository.findByNameAndProjectId(existedRootCause.getName(), existedRootCause.getProjectId())).thenReturn(existedRootCause);

        // when
        assertThrows(RamRootCauseAlreadyExistsException.class, () -> {
            service.create(request);
        });
    }

    @Test
    public void create_invalidRcWithGlobalTypeAndExistedName_exceptionExpected() {
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        // given
        RootCauseUpsertRequest request = new RootCauseUpsertRequest();
        request.setName("Migration Issue");
        request.setType(RootCauseType.GLOBAL);

        RootCause existedRootCause = new RootCause();
        existedRootCause.setUuid(UUID.randomUUID());
        existedRootCause.setName(request.getName());
        existedRootCause.setType(RootCauseType.GLOBAL);

        // when
        assertThrows(RamRootCauseIllegalAccessException.class, () -> {
            service.create(request);
        });
    }

    /*
     * Expected root cause tree:
     *
     * Migration Issue          [Global]
     *   Script Issue           [Custom]
     * VDHG RCA creation        [Custom]
     *   Invalid RCA Info       [Custom]
     *       Invalid First Name [Custom]
     * VDHU Kultura Issue       [Custom]
     * Build Issue              [Global]
     */
    @Test
    public void getRootCauseTree_invalidRcWithGlobalTypeAndExistedName_exceptionExpected() {
        final RootCause rc1 = RootCauseMock.generateRootCause("Migration Issue", RootCauseType.GLOBAL);
        final RootCause rc2 = RootCauseMock.generateRootCause("VDHU Script Issue", RootCauseType.CUSTOM);
        final RootCause rc3 = RootCauseMock.generateRootCause("VDHU RCA creation", RootCauseType.CUSTOM);
        final RootCause rc4 = RootCauseMock.generateRootCause("Invalid RCA Info", RootCauseType.CUSTOM);
        final RootCause rc5 = RootCauseMock.generateRootCause("Invalid First Name", RootCauseType.CUSTOM);
        final RootCause rc6 = RootCauseMock.generateRootCause("VDHU Kultura Issue", RootCauseType.CUSTOM);
        final RootCause rc7 = RootCauseMock.generateRootCause("Build Issue", RootCauseType.GLOBAL);
        rc2.setParentId(rc1.getUuid());
        rc4.setParentId(rc3.getUuid());
        rc5.setParentId(rc4.getUuid());

        final UUID projectId = UUID.randomUUID();

        final List<RootCause> customRootCauses = asList(rc2, rc3, rc4, rc5, rc6);
        customRootCauses.forEach(rootCause -> {
            rootCause.setProjectId(projectId);
        });
        final List<RootCause> topLevelCustomRootCauses = asList(rc3, rc6);
        final List<RootCause> topLevelGlobalRootCauses = asList(rc1, rc7);

        when(repository.findAllByParentIdIsNullAndProjectIdAndType(projectId, RootCauseType.CUSTOM)).thenReturn(topLevelCustomRootCauses);
        when(repository.findAllByParentIdIsNullAndType(RootCauseType.GLOBAL)).thenReturn(topLevelGlobalRootCauses);
        when(repository.findAllByParentIdAndProjectId(rc3.getUuid(), projectId)).thenReturn(Collections.singletonList(rc4));
        when(repository.findAllByParentIdAndProjectId(rc4.getUuid(), projectId)).thenReturn(Collections.singletonList(rc5));
        when(repository.findAllByParentId(rc1.getUuid())).thenReturn(Collections.singletonList(rc2));

        final List<RootCauseTreeNode> rootCauseTree = service.getRootCauseTree(projectId, false);

        assertNotNull(rootCauseTree, "Result tree shouldn't be null");
        assertFalse(rootCauseTree.isEmpty(), "Result tree shouldn't be empty");
        int expectedTreeSize = topLevelCustomRootCauses.size() + topLevelGlobalRootCauses.size();
        assertEquals(expectedTreeSize, rootCauseTree.size(), "Incorrect root cause tree size");

        final RootCauseTreeNode rc1Node = findRootCauseTreeNode(rootCauseTree, rc1.getUuid());
        assertEquals(rc1.getName(), rc1Node.getName());
        final List<TreeNode<RootCause>> rc1NodeChildren = rc1Node.getChildren();
        assertFalse(CollectionUtils.isEmpty(rc1NodeChildren));
        assertEquals(1, rc1NodeChildren.size());
    }

    private RootCauseTreeNode findRootCauseTreeNode(List<RootCauseTreeNode> tree, UUID nodeId) {
        return tree.stream()
                .filter(rootCauseTreeNode -> rootCauseTreeNode.getId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot find tree node"));
    }
}
