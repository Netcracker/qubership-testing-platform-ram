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

package org.qubership.atp.ram.mapper;

import org.qubership.atp.ram.dto.response.LogRecordShort;
import org.qubership.atp.ram.dto.response.logrecord.BvLogRecordShort;
import org.qubership.atp.ram.dto.response.logrecord.CompoundLogRecordShort;
import org.qubership.atp.ram.dto.response.logrecord.ItfLogRecordShort;
import org.qubership.atp.ram.dto.response.logrecord.RbmLogRecordShort;
import org.qubership.atp.ram.dto.response.logrecord.RestLogRecordShort;
import org.qubership.atp.ram.dto.response.logrecord.SqlLogRecordShort;
import org.qubership.atp.ram.dto.response.logrecord.SshLogRecordShort;
import org.qubership.atp.ram.dto.response.logrecord.TechnicalLogRecordShort;
import org.qubership.atp.ram.dto.response.logrecord.UiLogRecordShort;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.logrecords.BvLogRecord;
import org.qubership.atp.ram.models.logrecords.CompoundLogRecord;
import org.qubership.atp.ram.models.logrecords.ItfLogRecord;
import org.qubership.atp.ram.models.logrecords.RbmLogRecord;
import org.qubership.atp.ram.models.logrecords.RestLogRecord;
import org.qubership.atp.ram.models.logrecords.SqlLogRecord;
import org.qubership.atp.ram.models.logrecords.SshLogRecord;
import org.qubership.atp.ram.models.logrecords.TechnicalLogRecord;
import org.qubership.atp.ram.models.logrecords.UiLogRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Setter;

/**
 * Custom mapper {@link LogRecord} to {@link LogRecordShort}.
 */
@Setter
@Component
public class LogRecordMapper extends AbstractMapper<LogRecord, LogRecordShort> {

    @Autowired
    public LogRecordMapper() {
        super(LogRecord.class, LogRecordShort.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogRecordShort entityToDto(LogRecord entity) {
        if (entity instanceof UiLogRecord
                && entity.getType().equals(TypeAction.UI)) {
            return mapper.map(entity, UiLogRecordShort.class);
        } else if (entity instanceof BvLogRecord
                && entity.getType().equals(TypeAction.BV)) {
            return mapper.map(entity, BvLogRecordShort.class);
        } else if (entity instanceof RbmLogRecord
                && entity.getType().equals(TypeAction.R_B_M)) {
            return mapper.map(entity, RbmLogRecordShort.class);
        } else if (entity instanceof ItfLogRecord
                && entity.getType().equals(TypeAction.ITF)) {
            return mapper.map(entity, ItfLogRecordShort.class);
        } else if (entity instanceof RestLogRecord
                && entity.getType().equals(TypeAction.REST)) {
            return mapper.map(entity, RestLogRecordShort.class);
        } else if (entity instanceof SqlLogRecord
                && entity.getType().equals(TypeAction.SQL)) {
            return mapper.map(entity, SqlLogRecordShort.class);
        } else if (entity instanceof SshLogRecord
                && entity.getType().equals(TypeAction.SSH)) {
            return mapper.map(entity, SshLogRecordShort.class);
        } else if (entity instanceof CompoundLogRecord
                && entity.getType().equals(TypeAction.COMPOUND)) {
            return mapper.map(entity, CompoundLogRecordShort.class);
        } else if (entity instanceof TechnicalLogRecord
                && entity.getType().equals(TypeAction.TECHNICAL)) {
            return mapper.map(entity, TechnicalLogRecordShort.class);
        }
        return mapper.map(entity, LogRecordShort.class);
    }
}
