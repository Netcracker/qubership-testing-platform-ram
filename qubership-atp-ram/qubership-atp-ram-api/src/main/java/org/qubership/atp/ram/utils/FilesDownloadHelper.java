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

package org.qubership.atp.ram.utils;

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

import java.io.ByteArrayOutputStream;

import org.qubership.atp.ram.model.GridFsFileData;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FilesDownloadHelper {

    /**
     * Util method to add headers for successful file download to file system.
     *
     * @param fileName - file name.
     */
    public static HttpHeaders addDownloadToFileSystemHeaders(String fileName) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", fileName));
        responseHeaders.add(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
        return responseHeaders;
    }

    /**
     * Cover GridFS file into response entity.
     *
     * @param fileData file data
     * @return response entity
     */
    public ResponseEntity<Object> getGridFsFileResponseEntity(GridFsFileData fileData) {
        ByteArrayOutputStream outputStream = fileData.getOutputStream();
        String fileName = fileData.getFileName();
        HttpHeaders responseHeaders = addDownloadToFileSystemHeaders(fileName);

        return ResponseEntity.ok().headers(responseHeaders).body(outputStream.toByteArray());
    }
}
