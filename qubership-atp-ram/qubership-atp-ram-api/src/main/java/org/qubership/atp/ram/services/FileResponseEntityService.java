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

import java.util.Arrays;

import org.qubership.atp.ram.model.FileData;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileResponseEntityService {

    private static final String ATTACHMENT = "attachment";

    /**
     * Build octet stream response entity response entity.
     *
     * @param fileData the file data
     * @return the response entity
     */
    public ResponseEntity<Resource> buildOctetStreamResponseEntity(FileData fileData) {
        if (fileData == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .headers(getHeaders(fileData, MediaType.APPLICATION_OCTET_STREAM))
                .body(new ByteArrayResource(fileData.getContent()));
    }

    private HttpHeaders getHeaders(FileData fileData, MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setContentLength(fileData.getContent().length);
        headers.setAccessControlExposeHeaders(Arrays.asList(HttpHeaders.CONTENT_DISPOSITION));
        headers.setContentDisposition(ContentDisposition.builder(ATTACHMENT).filename(fileData.getSource()).build());
        return headers;
    }
}
