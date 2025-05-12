package org.qubership.atp.ram.migration.mongoevolution.java;

import java.util.Collections;
import java.util.List;

import org.qubership.atp.ram.migration.mongoevolution.java.annotation.AnnotationProcessor;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.DBManagerEntity;
import org.qubership.atp.ram.migration.mongoevolution.java.dataaccess.ConnectionSearchKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;

public class MongoEvolution extends AbstractMongoEvolution {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoEvolution.class);

    private final MongoClient client;
    private final String dbName;
    private final ConnectionSearchKey connectionSearchKey;

    /**
     * Constructs a new {@link MongoEvolution} instance for handling schema evolution in the specified MongoDB database.
     *
     * @param client              The {@link MongoClient} instance used to connect to MongoDB.
     * @param dbName              The name of the target MongoDB database.
     * @param connectionSearchKey The key representing the connection context or classifier for database operations.
     */
    public MongoEvolution(MongoClient client, String dbName, ConnectionSearchKey connectionSearchKey) {
        super(client, dbName, connectionSearchKey);
        this.client = client;
        this.dbName = dbName;
        this.connectionSearchKey = connectionSearchKey;
    }

    /**
     * Initiates the schema evolution process by scanning a single package for change logs.
     *
     * <p>This method locates classes annotated with ChangeLog annotation within the specified package
     * and applies all pending schema changes.
     *
     * @param changeLogsScanPackage The package name to scan for change logs.
     * @throws Exception If an error occurs during the migration process.
     */
    public void evolve(String changeLogsScanPackage) throws Exception {
        evolve(Collections.singletonList(changeLogsScanPackage));
    }

    /**
     * Initiates the schema evolution process by scanning a single package for change logs,
     * using a custom {@link DBManagerEntity} for database management.
     *
     * <p>This allows enhanced control over database interactions during the migration process.
     *
     * @param changeLogsScanPackage The package name to scan for change logs.
     * @param dbManagerEntity       The custom database manager entity to be used during evolution.
     * @throws Exception If an error occurs during the migration process.
     */
    public void evolve(String changeLogsScanPackage, DBManagerEntity dbManagerEntity) throws Exception {
        evolve(Collections.singletonList(changeLogsScanPackage), dbManagerEntity);
    }

    /**
     * Initiates the schema evolution process by scanning multiple packages for change logs.
     *
     * <p>All detected change logs across the provided packages will be processed and applied if needed.
     *
     * @param changeLogsScanPackages A list of package names to scan for change logs.
     * @throws Exception If an error occurs during the migration process.
     */
    public void evolve(List<String> changeLogsScanPackages) throws Exception {
        evolve(changeLogsScanPackages, null);
    }

    /**
     * Initiates the schema evolution process by scanning multiple packages for change logs,
     * using a custom {@link DBManagerEntity} for advanced database management.
     *
     * <p>This is the most flexible method, allowing both multi-package scanning and custom database handling.
     *
     * @param changeLogsScanPackages A list of package names to scan for change logs.
     * @param dbManagerEntity        The custom database manager entity to be used during evolution.
     *                               Can be {@code null}.
     * @throws Exception If an error occurs during the migration process.
     */
    public void evolve(List<String> changeLogsScanPackages, DBManagerEntity dbManagerEntity) throws Exception {
        LOGGER.info("Starting MongoDB evolution for DB: '{}', packages: {}", dbName, changeLogsScanPackages);

        AnnotationProcessor processor
                = new AnnotationProcessor(client, dbName, connectionSearchKey, changeLogsScanPackages, dbManagerEntity);
        doEvolve(processor);
    }
}
