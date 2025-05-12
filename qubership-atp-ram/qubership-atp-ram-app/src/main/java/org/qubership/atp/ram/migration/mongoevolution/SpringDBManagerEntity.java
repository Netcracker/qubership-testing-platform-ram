package org.qubership.atp.ram.migration.mongoevolution;

import org.qubership.atp.ram.migration.mongoevolution.java.annotation.DBManagerEntity;
import org.springframework.data.mongodb.core.MongoTemplate;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SpringDBManagerEntity extends DBManagerEntity {
    private MongoTemplate mongoTemplate;
}
