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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.model.ArchiveData;
import org.qubership.atp.ram.model.FileData;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class FileResponseEntityServiceTest {

    private FileResponseEntityService fileResponseEntityService;

    @BeforeEach
    public void setUp() {
        fileResponseEntityService = new FileResponseEntityService();
    }

    @Test
    public void buildResponseEntity_buildForFileData() {
        FileData file = new FileData(new byte[]{0, 1, 2}, "image/png", "fileName");
        ResponseEntity<Resource> result =
                fileResponseEntityService.buildOctetStreamResponseEntity(file);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertTrue(result.getHeaders().containsKey(HttpHeaders.CONTENT_DISPOSITION));
        Assertions.assertTrue(
                result.getHeaders().getAccessControlExposeHeaders().contains(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    public void buildResponseEntity_buildForArchiveData() {
        ArchiveData file = new ArchiveData(new byte[]{0, 1, 2}, "fileName2");
        ResponseEntity<Resource> result =
                fileResponseEntityService.buildOctetStreamResponseEntity(file);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertTrue(result.getHeaders().containsKey(HttpHeaders.CONTENT_DISPOSITION));
        Assertions.assertTrue(
                result.getHeaders().getAccessControlExposeHeaders().contains(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    public void buildResponseEntity_buildForNull() {
        ArchiveData file = null;
        ResponseEntity<Resource> result =
                fileResponseEntityService.buildOctetStreamResponseEntity(file);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }
}
