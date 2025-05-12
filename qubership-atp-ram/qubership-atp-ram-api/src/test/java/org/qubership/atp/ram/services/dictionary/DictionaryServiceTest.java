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

package org.qubership.atp.ram.services.dictionary;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.qubership.atp.ram.repositories.DictionaryRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class DictionaryServiceTest {

    @InjectMocks
    private DictionaryService service;

    @Mock
    private DictionaryRepository repository;

    @BeforeEach
    public void setUp() {
        service.setRepository(repository);
    }

    @Test
    public void getAllByName() {
        String defectFoundInDictionaryName = DictionaryCatalogEnum.DEFECT_FOUND_IN.name();
        service.getAllByName(defectFoundInDictionaryName);

        verify(repository, times(1)).getAllByType(defectFoundInDictionaryName);
    }
}
