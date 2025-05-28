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

package org.qubership.atp.ram.migration.mongoevolution;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.qubership.atp.ram.migration.mongoevolution.java.annotation.AnnotationProcessor;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeEntry;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.ChangeLog;
import org.qubership.atp.ram.migration.mongoevolution.java.dataaccess.ConnectionSearchKey;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

public class SpringMongoEvolutionProcessor extends AnnotationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationProcessor.class);
    private static final String DEFAULT_PROFILE = "default";
    private static final Class[] SPRING_ENVIRONMENT_PARAMETERS_MATCH = new Class[]{Environment.class};
    private static final Class[] SPRING_APPLICATION_CONTEXT_PARAMETERS_MATCH = new Class[]{ApplicationContext.class};
    private final List<String> changeLogsPackages;
    private final List<String> activeProfiles;
    private ConnectionSearchKey connectionSearchKey;
    private MongoClient client;
    private String dbName;
    private static final ConcurrentHashMap<ConnectionSearchKey, List<ChangeEntry>> changeEntriesCache
            = new ConcurrentHashMap();
    private SpringDBManagerEntity springDBManagerEntity;
    private Environment environment;
    private Map<String, Object> classNamesAndBeans;

    /**
     * Constructs a {@code SpringMongoEvolutionProcessor} for scanning a single package without Spring context.
     *
     * @param client              The {@link MongoClient} instance for MongoDB connection.
     * @param dbName              The target MongoDB database name.
     * @param connectionSearchKey The key identifying the database connection context.
     * @param changeLogsBasePackage The package to scan for change logs.
     */
    public SpringMongoEvolutionProcessor(MongoClient client,
                                         String dbName,
                                         ConnectionSearchKey connectionSearchKey,
                                         String changeLogsBasePackage) {
        this(
                client,
                dbName,
                connectionSearchKey,
                Collections.singletonList(changeLogsBasePackage),
                null,
                null,
                null
        );
    }

    /**
     * Constructs a {@code SpringMongoEvolutionProcessor} with a single package and Spring {@link Environment}.
     *
     * @param client              The {@link MongoClient} instance for MongoDB connection.
     * @param dbName              The target MongoDB database name.
     * @param connectionSearchKey The key identifying the database connection context.
     * @param changeLogsBasePackage The package to scan for change logs.
     * @param environment         The Spring {@link Environment} for injecting configuration and profiles.
     */
    public SpringMongoEvolutionProcessor(MongoClient client,
                                         String dbName,
                                         ConnectionSearchKey connectionSearchKey,
                                         String changeLogsBasePackage,
                                         Environment environment) {
        this(
                client,
                dbName,
                connectionSearchKey,
                Collections.singletonList(changeLogsBasePackage),
                environment,
                null,
                null
        );
    }

    /**
     * Constructs a {@code SpringMongoEvolutionProcessor} with a single package, Spring {@link Environment},
     * and a map of beans for dependency injection.
     *
     * @param client              The {@link MongoClient} instance for MongoDB connection.
     * @param dbName              The target MongoDB database name.
     * @param connectionSearchKey The key identifying the database connection context.
     * @param changeLogsBasePackage The package to scan for change logs.
     * @param environment         The Spring {@link Environment}.
     * @param classNamesAndBeans  A map containing class names and corresponding Spring beans for injection.
     */
    public SpringMongoEvolutionProcessor(MongoClient client,
                                         String dbName,
                                         ConnectionSearchKey connectionSearchKey,
                                         String changeLogsBasePackage,
                                         Environment environment,
                                         Map<String, Object> classNamesAndBeans) {
        this(
                client,
                dbName,
                connectionSearchKey,
                Collections.singletonList(changeLogsBasePackage),
                environment,
                null,
                classNamesAndBeans
        );
    }

    /**
     * Constructs a fully configurable {@code SpringMongoEvolutionProcessor} with multiple packages,
     * Spring context, custom database manager, and bean injection support.
     *
     * @param client               The {@link MongoClient} instance for MongoDB connection.
     * @param dbName               The target MongoDB database name.
     * @param connectionSearchKey  The key identifying the database connection context.
     * @param changeLogsPackages   A list of packages to scan for change logs.
     * @param environment          The Spring {@link Environment}.
     * @param springDBManagerEntity The custom Spring database manager entity.
     * @param classNamesAndBeans   A map containing Spring beans for injection into change sets.
     */
    public SpringMongoEvolutionProcessor(MongoClient client,
                                         String dbName,
                                         ConnectionSearchKey connectionSearchKey,
                                         List<String> changeLogsPackages,
                                         Environment environment,
                                         SpringDBManagerEntity springDBManagerEntity,
                                         Map<String, Object> classNamesAndBeans) {
        super(client, dbName, connectionSearchKey, changeLogsPackages, springDBManagerEntity);
        this.client = client;
        this.dbName = dbName;
        this.connectionSearchKey = connectionSearchKey;
        this.changeLogsPackages = changeLogsPackages;
        this.environment = environment;
        this.springDBManagerEntity = null == springDBManagerEntity
                ? new SpringDBManagerEntity() : springDBManagerEntity;
        this.classNamesAndBeans = classNamesAndBeans;
        this.activeProfiles = environment != null
                && environment.getActiveProfiles() != null
                && environment.getActiveProfiles().length > 0
                ? Arrays.asList(environment.getActiveProfiles()) : Collections.singletonList("default");
    }

    private MongoTemplate getMongoTemplate() {
        if (null == this.springDBManagerEntity.getMongoTemplate()) {
            this.springDBManagerEntity.setMongoTemplate(new MongoTemplate(this.client, this.dbName));
        }

        return this.springDBManagerEntity.getMongoTemplate();
    }

    private boolean matchesActiveSpringProfile(AnnotatedElement element) {
        if (element.isAnnotationPresent(Profile.class)) {
            Profile profiles = (Profile)element.getAnnotation(Profile.class);
            List<String> values = Arrays.asList(profiles.value());
            Stream var10000 = this.activeProfiles.stream();
            values.getClass();
            return ((List)var10000.filter(values::contains).collect(Collectors.toList())).size() > 0;
        } else {
            return true;
        }
    }

    private List<Class<?>> filterByActiveProfiles(Collection<Class<?>> annotated) {
        List<Class<?>> filtered = new ArrayList<>();

        for (Class<?> clazz : annotated) {
            if (this.matchesActiveSpringProfile(clazz)) {
                filtered.add(clazz);
            }
        }

        return filtered;
    }

    /**
     * Retrieves a list of change log classes annotated with {@link ChangeLog},
     * filtered by active Spring profiles and database classifier.
     *
     * @return A list of change log classes applicable for the current environment and configuration.
     */
    @Override
    protected List<Class<?>> fetchChangeLogs() {
        Set<Class<?>> changeLogs = new HashSet<>();

        for (String changeLogsPackage : this.changeLogsPackages) {
            Reflections reflections = new Reflections(changeLogsPackage);
            changeLogs.addAll(reflections.getTypesAnnotatedWith(ChangeLog.class));
        }

        return filterByActiveProfiles(changeLogs).stream().filter((changelog) ->
                (changelog.getAnnotation(ChangeLog.class))
                        .dbClassifier()
                        .equals(this.connectionSearchKey.getDbClassifier()))
                .collect(Collectors.toList());
    }

    /**
     * Acquires an instance of the specified change log class.
     *
     * <p>If a constructor accepting {@link Environment} is available, it will be used for instantiation.
     * Otherwise, the default no-args constructor will be invoked.
     *
     * @param changelogClass The class representing the change log.
     * @return An instance of the change log class ready for executing change sets.
     */
    @Override
    protected Object acquireInstance(Class<?> changelogClass) {
        LOGGER.info("Try to acquire instance of {} to include it in change logs list", changelogClass.getName());
        return Arrays.stream(changelogClass.getConstructors())
                .filter(this::matchSpringEnvironment).findFirst()
                .map((constructor) -> {
                    LOGGER.info("Found one constructor appropriate to inject Spring Environment: {}",
                            constructor.getName());
                    try {
                        return (Object) constructor.newInstance(this.environment);
                    } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseGet(() -> {
                    try {
                        return changelogClass.getConstructor().newInstance();
                    } catch (IllegalAccessException
                             | InvocationTargetException
                             | NoSuchMethodException
                             | InstantiationException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private boolean matchSpringEnvironment(Constructor<?> constructor) {
        LOGGER.debug("Check if constructor {} is appropriate to inject Spring Environment", constructor.getName());
        return Arrays.equals(SPRING_ENVIRONMENT_PARAMETERS_MATCH, constructor.getParameterTypes());
    }

    /**
     * Executes a change set method with appropriate parameters based on its signature.
     *
     * <p>Supports method injection of:
     * <ul>
     *   <li>{@link com.mongodb.client.MongoDatabase}</li>
     *   <li>{@link org.springframework.data.mongodb.core.MongoTemplate}</li>
     *   <li>Optional {@link Map} for bean injection</li>
     * </ul>
     *
     * @param changeSetMethod   The method representing the change set to execute.
     * @param changeLogInstance The instance of the change log class containing the method.
     * @return The result of the invoked change set method, if any.
     * @throws Exception If the method signature is invalid or execution fails.
     */
    @Override
    protected Object executeChangeSetMethod(Method changeSetMethod, Object changeLogInstance) throws Exception {
        int methodParamsLength = changeSetMethod.getParameterTypes().length;
        Object result;
        if (methodParamsLength > 0 && changeSetMethod.getParameterTypes()[0].equals(MongoDatabase.class)) {
            LOGGER.debug("Invoking method with MongoDatabase argument: {}", changeSetMethod);
            result = methodParamsLength == 2 && changeSetMethod.getParameterTypes()[1].equals(Map.class)
                    ? changeSetMethod.invoke(changeLogInstance, this.getMongoDatabase(), this.classNamesAndBeans)
                    : changeSetMethod.invoke(changeLogInstance, this.getMongoDatabase());
        } else if (methodParamsLength > 0 && changeSetMethod.getParameterTypes()[0].equals(MongoTemplate.class)) {
            LOGGER.debug("method with MongoTemplate argument: {}", changeSetMethod);
            result = methodParamsLength == 2 && changeSetMethod.getParameterTypes()[1].equals(Map.class)
                    ? changeSetMethod.invoke(changeLogInstance, this.getMongoTemplate(), this.classNamesAndBeans)
                    : changeSetMethod.invoke(changeLogInstance, this.getMongoTemplate());
        } else {
            if (changeSetMethod.getParameterTypes().length != 0) {
                throw new Exception("ChangeSet method " + changeSetMethod
                        + " has wrong arguments list. Please see docs for more info!");
            }

            LOGGER.debug("method with no params: {}", changeSetMethod);
            result = changeSetMethod.invoke(changeLogInstance);
        }

        this.registerProcessUpdateTime();
        return result;
    }
}
