package org.qubership.atp.ram.migration.mongoevolution.java.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.qubership.atp.ram.migration.mongoevolution.java.MongoEvolution;
import org.qubership.atp.ram.migration.mongoevolution.java.dataaccess.ConnectionSearchKey;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

public class AnnotationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationProcessor.class);
    private static final String SCHEMA_EVOLUTION_COLLECTION = "_schema_evolution";
    private static final String SCHEMA_CHANGE_LOG_COLLECTION = "_schema_change_log";

    private final List<String> changeLogsPackages;
    private final ConnectionSearchKey connectionSearchKey;
    private final MongoClient client;
    private final String dbName;
    private final DBManagerEntity dbManagerEntity;

    private static final ConcurrentHashMap<ConnectionSearchKey, List<ChangeEntry>> changeEntriesCache
            = new ConcurrentHashMap<>();

    public AnnotationProcessor(MongoClient client,
                               String dbName,
                               ConnectionSearchKey connectionSearchKey,
                               String changeLogsBasePackage) {
        this(client, dbName, connectionSearchKey, Collections.singletonList(changeLogsBasePackage), null);
    }

    /**
     * Constructs an AnnotationProcessor with specified MongoDB client, database name,
     * connection search key, list of packages to scan for change logs, and a database manager entity.
     * <p/>
     * If the provided {@code dbManagerEntity} is {@code null},
     * a new instance of {@link DBManagerEntity} will be created.
     *
     * @param client              The MongoDB client instance.
     * @param dbName              The name of the MongoDB database.
     * @param connectionSearchKey The key used to identify the database connection context.
     * @param changeLogsPackages  A list of package names to scan for classes annotated with {@link ChangeLog}.
     * @param dbManagerEntity     The database manager entity to manage MongoDB-related operations. Can be {@code null}.
     */
    public AnnotationProcessor(MongoClient client,
                               String dbName,
                               ConnectionSearchKey connectionSearchKey,
                               List<String> changeLogsPackages,
                               DBManagerEntity dbManagerEntity) {
        this.client = client;
        this.dbName = dbName;
        this.connectionSearchKey = connectionSearchKey;
        this.changeLogsPackages = changeLogsPackages;
        this.dbManagerEntity = dbManagerEntity != null ? dbManagerEntity : new DBManagerEntity();
    }

    protected MongoDatabase getMongoDatabase() {
        if (dbManagerEntity.getMongoDatabase() == null) {
            PojoCodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
            CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromProviders(pojoCodecProvider));
            dbManagerEntity.setMongoDatabase(client.getDatabase(dbName).withCodecRegistry(codecRegistry));
        }
        return dbManagerEntity.getMongoDatabase();
    }

    private List<Method> filterChangeSetAnnotation(List<Method> allMethods) {
        return allMethods.stream()
                .filter(method -> method.isAnnotationPresent(ChangeSet.class))
                .collect(Collectors.toList());
    }

    protected List<Class<?>> fetchChangeLogs() {
        Set<Class<?>> changeLogs = new HashSet<>();
        for (String changeLogsPackage : changeLogsPackages) {
            Reflections reflections = new Reflections(changeLogsPackage);
            changeLogs.addAll(reflections.getTypesAnnotatedWith(ChangeLog.class));
        }

        return changeLogs.stream()
                .filter(changelog -> changelog.getAnnotation(ChangeLog.class)
                        .dbClassifier().equals(connectionSearchKey.getDbClassifier()))
                .collect(Collectors.toList());
    }

    private List<Method> fetchChangeSets(Class<?> type) {
        return filterChangeSetAnnotation(Arrays.asList(type.getDeclaredMethods()));
    }

    private ChangeEntry createChangeEntry(Method changesetMethod,
                                          Object changeLogInstance, long version, int orderChangeLog) {
        ChangeSet annotation = changesetMethod.getAnnotation(ChangeSet.class);
        return annotation != null ? new ChangeEntry(version,
                orderChangeLog, annotation.order(), changesetMethod, changeLogInstance) : null;
    }

    protected void registerProcessUpdateTime() {
        MongoCollection<Document> updatesTracker = getMongoDatabase().getCollection(SCHEMA_EVOLUTION_COLLECTION);
        MongoEvolution.updateFieldWithMongoCurrentDate(updatesTracker, "lastUpdateTime", null);
    }

    private List<ChangeEntry> getSortedChangeEntries() throws Exception {
        List<ChangeEntry> changesList = new ArrayList<>();

        for (Class<?> changelogClass : fetchChangeLogs()) {
            try {
                ChangeLog changeLogAnnotation = changelogClass.getAnnotation(ChangeLog.class);
                Object changelogInstance = acquireInstance(changelogClass);
                List<Method> changesetMethods = fetchChangeSets(changelogClass);

                for (Method changesetMethod : changesetMethods) {
                    ChangeEntry entry = createChangeEntry(changesetMethod,
                            changelogInstance, changeLogAnnotation.version(), changeLogAnnotation.order());
                    if (entry != null) {
                        changesList.add(entry);
                    }
                    registerProcessUpdateTime();
                }

            } catch (Exception e) {
                LOGGER.error("Error during annotations collecting: {}", e.getMessage());
                throw new Exception(e.getMessage(), e);
            }
        }

        changesList.sort(ChangeEntry.COMPARATOR);
        registerProcessUpdateTime();
        return changesList;
    }

    protected Object acquireInstance(Class<?> changelogClass) {
        LOGGER.info("Try to acquire instance of {} to include it in change logs list", changelogClass.getName());
        try {
            return changelogClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate " + changelogClass.getName(), e);
        }
    }

    protected Object executeChangeSetMethod(Method changeSetMethod, Object changeLogInstance) throws Exception {
        Class<?>[] parameterTypes = changeSetMethod.getParameterTypes();
        Object result;

        if (parameterTypes.length == 2
                && parameterTypes[0].equals(MongoDatabase.class)
                && parameterTypes[1].equals(Map.class)) {

            LOGGER.debug("Invoking method with MongoDatabase and Map arguments: {}", changeSetMethod);
            result = changeSetMethod.invoke(changeLogInstance, getMongoDatabase(), new HashMap<>());

        } else if (parameterTypes.length == 1 && parameterTypes[0].equals(MongoDatabase.class)) {

            LOGGER.debug("Invoking method with MongoDatabase argument: {}", changeSetMethod);
            result = changeSetMethod.invoke(changeLogInstance, getMongoDatabase());

        } else if (parameterTypes.length == 0) {

            LOGGER.debug("Invoking method with no params: {}", changeSetMethod);
            result = changeSetMethod.invoke(changeLogInstance);

        } else {
            throw new Exception("ChangeSet method " + changeSetMethod
                    + " has unsupported parameters. Please see docs for more info!");
        }

        registerProcessUpdateTime();
        return result;
    }

    private List<ChangeEntry> getChangeEntriesFromCache() {
        return changeEntriesCache.computeIfAbsent(connectionSearchKey, key -> {
            try {
                return getSortedChangeEntries();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public long getMaxChangeLogVersion() {
        List<ChangeEntry> changeEntries = getChangeEntriesFromCache();
        return changeEntries.isEmpty() ? 0L : changeEntries.get(changeEntries.size() - 1).getVersion();
    }

    private boolean entryIsNotPresentInDb(ChangeEntry entry) {
        MongoCollection<Document> collection = getMongoDatabase().getCollection(SCHEMA_CHANGE_LOG_COLLECTION);
        Document doc = collection.find(Filters.and(
                Filters.gte("version", entry.getVersion()),
                Filters.eq("changeLogClass", entry.getChangeClassName()),
                Filters.eq("changeSetMethod", entry.getChangeMethodName())
        )).first();

        if (doc != null) {
            LOGGER.debug("ChangeEntry {} is already present in db: {}", entry, doc.toJson());
            return false;
        }
        return true;
    }

    /**
     * Applies database schema changes based on detected change sets.
     * <p/>
     * This method iterates through all collected {@link ChangeEntry} objects whose version
     * is greater than the specified {@code currentVersion} and applies them if they have not
     * been previously executed (i.e., not present in the change log collection).
     * <p/>
     * Each successfully applied change set is recorded in the MongoDB change log collection.
     * If any change set execution fails, an exception is thrown, halting the process.
     *
     * @param currentVersion The current version of the database schema.
     *                       Only change sets with a higher version will be applied.
     * @throws Exception If execution of any change set fails or if an internal error occurs during processing.
     */
    public void applyChanges(long currentVersion) throws Exception {
        for (ChangeEntry entry : getChangeEntriesFromCache()) {
            if (entry.getVersion() > currentVersion && entryIsNotPresentInDb(entry)) {
                try {
                    executeChangeSetMethod(entry.getChangeSetMethod(), entry.getChangeLogInstance());
                    entry.saveEntryInChangeLog(getMongoDatabase());
                } catch (Exception e) {
                    throw new Exception("The changeset failed: " + entry, e);
                }
                registerProcessUpdateTime();
            }
        }
    }
}
