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

import org.apache.tika.Tika;
import org.qubership.atp.ram.enums.FileContentType;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileContentTypeDetector {
    private static final String MEDIA_TYPE_TEXT_MATLAB = "text/x-matlab";

    /**
     * Detect FileContentType for a file byteArray.
     *
     * @param fileFromDataset content byte[].
     * @return FileContentType.
     */
    public static FileContentType detect(byte[] fileFromDataset) {
        final Tika defaultTika = new Tika();
        String fileType;
        try {
            fileType = defaultTika.detect(fileFromDataset);
        } catch (Exception ioEx) {
            log.error("Unable to detect type of file", ioEx);
            fileType = null;
        }

        return fileType != null ? getFileContentType(fileType) : null;
    }

    private static FileContentType getFileContentType(String contentType) {
        switch (contentType) {
            case MediaType.APPLICATION_XML_VALUE:
            case MediaType.TEXT_XML_VALUE:
                return FileContentType.XML;
            case MediaType.APPLICATION_JSON_VALUE:
                return FileContentType.JSON;
            case MediaType.TEXT_HTML_VALUE:
                return FileContentType.HTML;
            case MEDIA_TYPE_TEXT_MATLAB:
            case MediaType.TEXT_PLAIN_VALUE:
                return FileContentType.TEXT;
            default:
                return FileContentType.BIN;
        }
    }
}