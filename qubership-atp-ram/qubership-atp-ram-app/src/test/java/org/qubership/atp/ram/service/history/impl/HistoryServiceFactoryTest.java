/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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

package org.qubership.atp.ram.service.history.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.ram.controllers.api.dto.history.HistoryItemTypeDto;
import org.qubership.atp.ram.service.history.RestoreHistoryService;
import org.qubership.atp.ram.service.history.RetrieveHistoryService;
import org.qubership.atp.ram.service.history.VersioningHistoryService;

@ExtendWith(MockitoExtension.class)
class HistoryServiceFactoryTest {

    @InjectMocks
    private HistoryServiceFactory factory;

    @BeforeEach
    void setUp() {
        List<RetrieveHistoryService> retrieveHistoryServices = Arrays.asList(
                new FailPatternRetrieveHistoryService(null),
                new RootCauseRetrieveHistoryService(null),
                new ReportTemplateRetrieveHistoryService(null));
        List<RestoreHistoryService> restoreHistoryServices =
                Arrays.asList(new FailPatternRestoreHistoryService(null,
                                null, null, null),
                new RootCauseRestoreHistoryService(null, null, null),
                        new ReportTemplateRestoreHistoryService(null, null, null));
        List<VersioningHistoryService> versioningHistoryServices =
                Arrays.asList(new FailPatternVersioningHistoryService(null, null),
                        new RootCauseVersioningHistoryService(null, null),
                        new ReportTemplateVersioningHistoryService(null, null));

        factory = new HistoryServiceFactory(retrieveHistoryServices, restoreHistoryServices, versioningHistoryServices);
    }

    @Test
    void test_GetRetrieveHistoryService() {
        Optional<RetrieveHistoryService> service =
                factory.getRetrieveHistoryService(HistoryItemTypeDto.FAILPATTERN.toString());
        assertTrue(service.isPresent());
        assertInstanceOf(FailPatternRetrieveHistoryService.class, service.get());

        service = factory.getRetrieveHistoryService(HistoryItemTypeDto.ROOTCAUSE.toString());
        assertTrue(service.isPresent());
        assertInstanceOf(RootCauseRetrieveHistoryService.class, service.get());

        service = factory.getRetrieveHistoryService(HistoryItemTypeDto.EMAILTEMPLATE.toString());
        assertTrue(service.isPresent());
        assertInstanceOf(ReportTemplateRetrieveHistoryService.class, service.get());

        service = factory.getRetrieveHistoryService("UnknownType");
        assertFalse(service.isPresent());
    }

    @Test
    void test_GetRestoreHistoryService() {
        Optional<RestoreHistoryService> service =
                factory.getRestoreHistoryService(HistoryItemTypeDto.FAILPATTERN.toString());
        assertTrue(service.isPresent());
        assertInstanceOf(FailPatternRestoreHistoryService.class, service.get());

        service = factory.getRestoreHistoryService(HistoryItemTypeDto.ROOTCAUSE.toString());
        assertTrue(service.isPresent());
        assertInstanceOf(RootCauseRestoreHistoryService.class, service.get());

        service = factory.getRestoreHistoryService(HistoryItemTypeDto.EMAILTEMPLATE.toString());
        assertTrue(service.isPresent());
        assertInstanceOf(ReportTemplateRestoreHistoryService.class, service.get());

        service = factory.getRestoreHistoryService("UnknownType");
        assertFalse(service.isPresent());
    }

    @Test
    void test_GetVersioningHistoryService() {
        Optional<VersioningHistoryService> service =
                factory.getVersioningHistoryService(HistoryItemTypeDto.FAILPATTERN.toString());
        assertTrue(service.isPresent());
        assertInstanceOf(FailPatternVersioningHistoryService.class, service.get());

        service = factory.getVersioningHistoryService(HistoryItemTypeDto.ROOTCAUSE.toString());
        assertTrue(service.isPresent());
        assertInstanceOf(RootCauseVersioningHistoryService.class, service.get());

        service = factory.getVersioningHistoryService(HistoryItemTypeDto.EMAILTEMPLATE.toString());
        assertTrue(service.isPresent());
        assertInstanceOf(ReportTemplateVersioningHistoryService.class, service.get());

        service = factory.getVersioningHistoryService("UnknownType");
        assertFalse(service.isPresent());
    }
}
