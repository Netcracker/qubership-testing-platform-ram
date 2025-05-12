package org.qubership.atp.ram.migration.mongoevolution.java.dataaccess;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public final class ConnectionSearchKey {
    private final String tenantId;
    private final String dbClassifier;
}
