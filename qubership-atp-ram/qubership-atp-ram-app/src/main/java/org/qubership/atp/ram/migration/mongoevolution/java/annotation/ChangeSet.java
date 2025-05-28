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

package org.qubership.atp.ram.migration.mongoevolution.java.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a database schema change set (migration step).
 *
 * <p>Methods annotated with {@code @ChangeSet} define individual changes to be applied
 * to the database schema, such as creating collections, adding indexes, or updating data structures.
 *
 * <p>These methods are discovered within classes annotated with {@link ChangeLog} and executed
 * in the order specified by the {@code order} parameter.
 *
 * <p><b>Method Requirements:</b>
 * <ul>
 *   <li>Must be {@code public}.</li>
 *   <li>Can have zero parameters, a single {@link com.mongodb.client.MongoDatabase} parameter,
 *       or two parameters: {@link com.mongodb.client.MongoDatabase} and {@link java.util.Map}.</li>
 *   <li>Should contain idempotent logic to ensure safe re-execution, although execution is typically tracked.</li>
 * </ul>
 *
 * <p><b>Example usage:</b>
 * <pre>{@code
 * @ChangeSet(order = 1)
 * public void addNewCollection(MongoDatabase db) {
 *     db.createCollection("new_collection");
 * }
 * }</pre>
 *
 * @see ChangeLog
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ChangeSet {

    /**
     * Defines the execution order of this change set within its containing {@link ChangeLog} class.
     *
     * <p>Change sets with lower order values are executed first.
     * If multiple methods have the same order, their execution order is undefined.
     * Defaults to {@code 0}.
     *
     * @return the order of execution for this change set.
     */
    int order() default 0;
}
