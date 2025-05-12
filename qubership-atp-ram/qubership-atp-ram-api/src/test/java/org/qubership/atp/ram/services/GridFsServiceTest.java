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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qubership.atp.ram.exceptions.logrecords.RamLogRecordFileAsStringException;
import org.qubership.atp.ram.model.FileData;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.repositories.GridFsRepository;
import org.qubership.atp.ram.repositories.LogRecordRepository;
import org.qubership.atp.ram.utils.SourceShot;

public class GridFsServiceTest {

    private final String RESOURCES_DIRECTORY = "./src/test/resources";
    private final String SNAPSHOT_PNG = "snapshot.png";
    private GridFsService gridFsService;
    private LogRecordRepository lrRepository;
    private FileData fileData = new FileData();
    private LogRecord logRecord = new LogRecord();

    @BeforeEach
    public void setUp() {
        GridFsRepository mock = Mockito.mock(GridFsRepository.class);
        Mockito.when(mock.getFileData(Mockito.any())).thenReturn(Optional.of(fileData));
        Mockito.when(mock.getFileDataByFileName(Mockito.any(), Mockito.any())).thenReturn(Optional.of(fileData));
        lrRepository = Mockito.mock(LogRecordRepository.class);
        gridFsService = new GridFsService(mock, lrRepository);
        logRecord.setName("Log Record name");
        logRecord.setMessage("Log Record message");
    }

    @Test
    public void getScreenShot_screenShotAsHtml_returnContentOfSourceShotEqualEr() {
        Mockito.when(lrRepository.findByUuid(Mockito.any())).thenReturn(logRecord);
        String er = "<div>SomeText</div>";
        fileData.setContentType("text/html");
        fileData.setContent(er.getBytes());

        SourceShot sourceShot = gridFsService.getScreenShot(Mockito.any());

        Assertions.assertEquals(er, sourceShot.getContent());
    }

    @Test
    public void getScreenShot_screenShotAsScreenShot_returnSnapshotSourceEqualsEr() throws IOException {
        Mockito.when(lrRepository.findByUuid(Mockito.any())).thenReturn(logRecord);
        String er = "http://127.0.0.1:6800/common/uobject.jsp?object=1000";
        fileData.setContentType("image/png");
        fileData.setContent(Files.readAllBytes(Paths.get(RESOURCES_DIRECTORY, SNAPSHOT_PNG)));
        fileData.setSource("http://127.0.0.1:6800/common/uobject.jsp?object=1000");

        SourceShot sourceShot = gridFsService.getScreenShot(Mockito.any());

        Assertions.assertEquals(er, sourceShot.getSnapshotSource());
    }

    @Test
    public void downloadFileIntoStringTest_textFileIsFound_shouldReturnTextFileContent() {
        // given
        String expectedString = "<div>SomeText</div>";
        fileData.setContentType("text/html");
        fileData.setContent(expectedString.getBytes());
        // when
        Mockito.when(lrRepository.findByUuid(Mockito.any())).thenReturn(logRecord);
        String actualContent1 = gridFsService.downloadFileIntoString(logRecord.getUuid());
        String actualContent2 = gridFsService.downloadFileIntoStringByName(logRecord.getUuid(), "filename");
        // then
        Assertions.assertEquals(expectedString, actualContent1);
        Assertions.assertEquals(expectedString, actualContent2);
    }

    @Test
    public void downloadFileIntoStringTest_binaryFileIsFound_shouldExceptionShouldBeThrown() throws IOException {
        // given
        UUID logRecordId = UUID.randomUUID();
        logRecord.setUuid(logRecordId);
        String fileName = "test_name";
        fileData.setContentType("image/png");
        fileData.setContent(Files.readAllBytes(Paths.get(RESOURCES_DIRECTORY, SNAPSHOT_PNG)));
        // when
        Mockito.when(lrRepository.findByUuid(Mockito.any())).thenReturn(logRecord);
        RamLogRecordFileAsStringException actualException1 = Assertions.assertThrows(RamLogRecordFileAsStringException.class, () -> {
            gridFsService.downloadFileIntoString(logRecord.getUuid());
        });
        RamLogRecordFileAsStringException actualException2 = Assertions.assertThrows(RamLogRecordFileAsStringException.class, () -> {
            gridFsService.downloadFileIntoStringByName(logRecord.getUuid(), fileName);
        });
        // then
        Assertions.assertEquals(String.format(RamLogRecordFileAsStringException.DEFAULT_MESSAGE, "", logRecordId), actualException1.getMessage());
        Assertions.assertEquals(String.format(RamLogRecordFileAsStringException.DEFAULT_MESSAGE, fileName, logRecordId), actualException2.getMessage());
    }
}
