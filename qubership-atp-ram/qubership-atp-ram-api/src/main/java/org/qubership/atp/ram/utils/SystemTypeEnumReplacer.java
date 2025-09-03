package org.qubership.atp.ram.utils;

import java.lang.reflect.Field;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import org.qubership.atp.ram.clients.api.dto.catalogue.BugTrackingSystemSynchronizationDto;
import org.qubership.atp.ram.clients.api.dto.catalogue.BugTrackingSystemSynchronizationDtoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SystemTypeEnumReplacer {

    @Value("${internal.system-type.alias}")
    private String alias;

    @PostConstruct
    public void patch() throws Exception {
        patchEnumValue(
                BugTrackingSystemSynchronizationDto.SystemTypeEnum.class,
                BugTrackingSystemSynchronizationDto.SystemTypeEnum.INTERNAL_JIRA
        );
        log.debug("BugTrackingSystemSynchronizationDto.SystemTypeEnum.INTERNAL_JIRA.getValue() = {}",
                BugTrackingSystemSynchronizationDto.SystemTypeEnum.INTERNAL_JIRA.getValue());

        patchEnumValue(
                BugTrackingSystemSynchronizationDtoDto.SystemTypeEnum.class,
                BugTrackingSystemSynchronizationDtoDto.SystemTypeEnum.INTERNAL_JIRA
        );
        log.debug("BugTrackingSystemSynchronizationDtoDto.SystemTypeEnum.INTERNAL_JIRA.getValue() = {}",
                BugTrackingSystemSynchronizationDtoDto.SystemTypeEnum.INTERNAL_JIRA.getValue());
    }

    private void patchEnumValue(Class<?> enumClass, Enum<?> systemTypeEnum) {
        try {
            Field field = enumClass.getDeclaredField("value");
            field.setAccessible(true);
            field.set(systemTypeEnum, alias);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to patch " + enumClass.getName() + "#" + systemTypeEnum, e);
        }
    }
}
