package org.qubership.atp.ram.migration.mongoevolution.java.annotation;

import com.mongodb.client.MongoDatabase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DBManagerEntity {
    private MongoDatabase mongoDatabase;
}
