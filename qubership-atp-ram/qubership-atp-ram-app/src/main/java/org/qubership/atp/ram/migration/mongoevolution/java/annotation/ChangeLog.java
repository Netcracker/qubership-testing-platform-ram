package org.qubership.atp.ram.migration.mongoevolution.java.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a container for database schema change sets.
 * <p/>
 * Classes annotated with {@code @ChangeLog} are scanned and processed to detect methods
 * annotated with {@code @ChangeSet}, which define individual schema changes (migrations).
 * <p/>
 * Each change log class groups related change sets and is associated with a specific version
 * and optional execution order. Additionally, it can be linked to a particular database context
 * via the {@code dbClassifier} parameter.
 *
 * <p><b>Example usage:</b>
 * <pre>{@code
 * @ChangeLog(version = 2, order = 1, dbClassifier = "production")
 * public class MySchemaChanges {
 *
 *     @ChangeSet(order = 1, id = "addUsersCollection")
 *     public void addUsers(MongoDatabase db) {
 *         // implementation
 *     }
 * }
 * }</pre>
 *
 * @see ChangeSet
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ChangeLog {

    /**
     * Defines the version number of this change log.
     *
     * <p>Used to determine which change logs should be applied based on the current schema version.
     *
     * @return the version of the change log.
     */
    long version();

    /**
     * Specifies the execution order of this change log relative to other change logs with the same version.
     *
     * <p>Change logs with lower order values are applied first.
     * Defaults to {@code 0} if not specified.
     *
     * @return the order of execution within the version.
     */
    int order() default 0;

    /**
     * Classifies the change log for a specific database context or environment.
     *
     * <p>This allows filtering change logs depending on the target database
     * (e.g., different schemas for different environments).
     * Defaults to {@code "default"}.
     *
     * @return the database classifier string.
     */
    String dbClassifier() default "default";
}
