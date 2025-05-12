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

package org.qubership.atp.ram.validators;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpIllegalNullableArgumentException;
import org.qubership.atp.ram.model.request.RootCauseUpsertRequest;
import org.qubership.atp.ram.models.RootCause;
import org.qubership.atp.ram.models.RootCauseType;
import org.qubership.atp.ram.repositories.RootCauseRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class RootCauseUpsertRequestValidatorTest {

    @InjectMocks
    private RootCauseUpsertRequestValidator validator;

    @Mock
    private RootCauseRepository repository;

    @Test
    public void validate_invalidRcWithoutSpecifiedType_exceptionExpected() {
        // given
        RootCauseUpsertRequest request = new RootCauseUpsertRequest();
        request.setName("Migration Issue");

        // when
        Assertions.assertThrows(AtpIllegalNullableArgumentException.class, () -> {
            validator.validate(request, null);
        });
    }

    // Custom Root Cause validations
    @Test
    public void save_invalidRcWithoutProjectId_exceptionExpected() {
        // given
        RootCauseUpsertRequest request = new RootCauseUpsertRequest();
        request.setName("Migration Issue");
        request.setType(RootCauseType.CUSTOM);

        // when
        Assertions.assertThrows(AtpIllegalNullableArgumentException.class, () -> {
            validator.validate(request, null);
        });
    }

    // Global Root Cause validations
    @Test
    public void save_invalidRcWithIncompatibleTypes_exceptionExpected() {
        // given
        RootCauseUpsertRequest request = new RootCauseUpsertRequest();
        request.setName("Common Issue");
        request.setType(RootCauseType.CUSTOM);

        RootCause parentRootCause = new RootCause();
        parentRootCause.setName("Migration Issue");
        parentRootCause.setType(RootCauseType.GLOBAL);
        parentRootCause.setParentId(UUID.randomUUID());

//        when(repository.findById(parentRootCause.getParentId())).thenReturn(Optional.of(parentRootCause));

        // when
        Assertions.assertThrows(AtpIllegalNullableArgumentException.class, () -> {
            validator.validate(request, null);
        });
    }

    @Test
    public void save_invalidRcWithGlobalTypeAndSpecifiedProjectId_exceptionExpected() {
        // given
        RootCauseUpsertRequest request = new RootCauseUpsertRequest();
        request.setName("Migration Issue");
        request.setType(RootCauseType.GLOBAL);
        request.setProjectId(UUID.randomUUID());

        // when
        Assertions.assertThrows(AtpIllegalNullableArgumentException.class, () -> {
            validator.validate(request, null);
        });
    }
}
