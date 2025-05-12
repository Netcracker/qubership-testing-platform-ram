package org.qubership.atp.ram.migration.mongoevolution;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.qubership.atp.ram.migration.mongoevolution.java.AbstractMongoEvolution;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.AnnotationProcessor;
import org.qubership.atp.ram.migration.mongoevolution.java.dataaccess.ConnectionSearchKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import com.mongodb.client.MongoClient;

public class SpringMongoEvolution extends AbstractMongoEvolution {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringMongoEvolution.class);

    private final MongoClient client;
    private final String dbName;
    private final ConnectionSearchKey connectionSearchKey;

    /**
     * Constructs a {@code SpringMongoEvolution} instance for handling MongoDB schema migrations
     * with Spring Framework support.
     *
     * @param client              The {@link MongoClient} instance used to connect to MongoDB.
     * @param dbName              The name of the target MongoDB database.
     * @param connectionSearchKey The key identifying the connection context.
     */
    public SpringMongoEvolution(MongoClient client, String dbName, ConnectionSearchKey connectionSearchKey) {
        super(client, dbName, connectionSearchKey);
        this.client = client;
        this.dbName = dbName;
        this.connectionSearchKey = connectionSearchKey;
    }

    /**
     * Starts the schema evolution process by scanning the specified package for change logs
     * using the Spring {@link Environment}.
     *
     * @param changeLogsScanPackage The package to scan for classes annotated with ChangeLog.
     * @param environment           The Spring environment context for configuration parameters.
     * @throws Exception If an error occurs during the migration process.
     */
    public void evolve(String changeLogsScanPackage, Environment environment) throws Exception {
        evolve(Collections.singletonList(changeLogsScanPackage), environment);
    }

    /**
     * Starts the schema evolution process using the Spring environment and a custom database manager.
     *
     * @param changeLogsScanPackage The package to scan for change logs.
     * @param environment           The Spring {@link Environment}.
     * @param springDBManagerEntity The custom database manager for handling connections and parameters.
     * @throws Exception If an error occurs during the migration process.
     */
    public void evolve(String changeLogsScanPackage,
                       Environment environment,
                       SpringDBManagerEntity springDBManagerEntity) throws Exception {
        evolve(Collections.singletonList(changeLogsScanPackage), environment, springDBManagerEntity);
    }

    /**
     * Starts the schema evolution process with the provided Spring {@link Environment}
     * and a map of beans for dependency injection into change logs or change set methods.
     *
     * @param changeLogsScanPackage The package to scan for change logs.
     * @param environment           The Spring {@link Environment}.
     * @param classNamesAndBeans    A map containing class names and their corresponding Spring beans.
     * @throws Exception If an error occurs during the migration process.
     */
    public void evolve(String changeLogsScanPackage,
                       Environment environment,
                       Map<String, Object> classNamesAndBeans) throws Exception {
        evolve(Collections.singletonList(changeLogsScanPackage), environment, classNamesAndBeans);
    }

    /**
     * Starts the migration process with all parameters: package, Spring environment,
     * custom database manager, and a map of beans.
     *
     * @param changeLogsScanPackage The package to scan for change logs.
     * @param environment           The Spring {@link Environment}.
     * @param springDBManagerEntity The custom database manager.
     * @param classNamesAndBeans    A map of Spring beans for dependency injection.
     * @throws Exception If an error occurs during the migration process.
     */
    public void evolve(String changeLogsScanPackage,
                       Environment environment,
                       SpringDBManagerEntity springDBManagerEntity,
                       Map<String, Object> classNamesAndBeans) throws Exception {
        evolve(Collections.singletonList(changeLogsScanPackage),
                environment, springDBManagerEntity, classNamesAndBeans);
    }

    /**
     * Starts the schema evolution process by scanning a list of packages using the Spring {@link Environment}.
     *
     * @param changeLogsScanPackages The list of packages to scan for change logs.
     * @param environment            The Spring environment context.
     * @throws Exception If an error occurs during the migration process.
     */
    public void evolve(List<String> changeLogsScanPackages, Environment environment) throws Exception {
        evolve(changeLogsScanPackages, environment, null, null);
    }

    /**
     * Starts the migration process using a list of packages and a custom database manager.
     *
     * @param changeLogsScanPackages The list of packages to scan for change logs.
     * @param environment            The Spring {@link Environment}.
     * @param springDBManagerEntity  The custom database manager.
     * @throws Exception If an error occurs during the migration process.
     */
    public void evolve(List<String> changeLogsScanPackages,
                       Environment environment,
                       SpringDBManagerEntity springDBManagerEntity) throws Exception {
        evolve(changeLogsScanPackages, environment, springDBManagerEntity, null);
    }

    /**
     * Starts the migration process using a list of packages and a map of Spring beans.
     *
     * @param changeLogsScanPackages The list of packages to scan for change logs.
     * @param environment            The Spring {@link Environment}.
     * @param classNamesAndBeans     A map of Spring beans for dependency injection.
     * @throws Exception If an error occurs during the migration process.
     */
    public void evolve(List<String> changeLogsScanPackages,
                       Environment environment,
                       Map<String, Object> classNamesAndBeans) throws Exception {
        evolve(changeLogsScanPackages, environment, null, classNamesAndBeans);
    }

    /**
     * The core method for initiating the schema evolution process with maximum configuration flexibility.
     *
     * <p>Allows specifying multiple packages, Spring environment context, a custom database manager,
     * and a map of beans.
     *
     * @param changeLogsScanPackages The list of packages to scan for change logs.
     * @param environment            The Spring {@link Environment}.
     * @param springDBManagerEntity  The custom database manager. Can be {@code null}.
     * @param classNamesAndBeans     A map of Spring beans for dependency injection. Can be {@code null}.
     * @throws Exception If an error occurs during the migration process.
     */
    public void evolve(List<String> changeLogsScanPackages,
                       Environment environment,
                       SpringDBManagerEntity springDBManagerEntity,
                       Map<String, Object> classNamesAndBeans) throws Exception {
        LOGGER.info("Starting Spring MongoDB evolution for DB '{}', packages: {}, with Spring context",
                dbName, changeLogsScanPackages);

        SpringDBManagerEntity effectiveManager
                = springDBManagerEntity != null ? springDBManagerEntity : new SpringDBManagerEntity();

        AnnotationProcessor processor = new SpringMongoEvolutionProcessor(
                client,
                dbName,
                connectionSearchKey,
                changeLogsScanPackages,
                environment,
                effectiveManager,
                classNamesAndBeans
        );
        doEvolve(processor);
    }
}