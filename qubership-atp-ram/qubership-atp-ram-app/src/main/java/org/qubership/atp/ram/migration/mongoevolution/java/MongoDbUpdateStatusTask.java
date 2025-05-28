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

package org.qubership.atp.ram.migration.mongoevolution.java;

import java.util.Timer;
import java.util.TimerTask;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoDbUpdateStatusTask extends TimerTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDbUpdateStatusTask.class);

    private final AbstractMongoEvolution tracker;
    private final Timer timer;
    private final MongoDatabase database;

    private Long versionBeforeUpdate;

    /**
     * Creates a new instance of {@link MongoDbUpdateStatusTask} to monitor the status of a MongoDB schema
     * update process.
     *
     * <p>This task periodically checks if the database update lock is still active.
     * If the update process is deemed inactive, it resets the update flag in the tracker collection
     * and stops the associated timer.
     *
     * <p>Typically used as part of the schema evolution mechanism to ensure that long-running or stalled updates
     * are properly detected and handled.
     *
     * @param timer     The {@link Timer} that schedules this task.
     *                  It will be cancelled if the update process is no longer active.
     * @param tracker   The {@link AbstractMongoEvolution} instance responsible for managing schema evolution logic.
     * @param database  The {@link MongoDatabase} where the update tracking information is stored.
     */
    public MongoDbUpdateStatusTask(Timer timer, AbstractMongoEvolution tracker, MongoDatabase database) {
        this.timer = timer;
        this.tracker = tracker;
        this.database = database;
    }

    @Override
    public void run() {
        try {
            if (!tracker.isDatabaseUpdateLockAlive()) {
                LOGGER.error("Update process is not active for database '{}'", database.getName());

                MongoCollection<Document> updatesTracker
                        = database.getCollection(AbstractMongoEvolution.TRACKER_COLLECTION);
                tracker.insertUpdateFlag(updatesTracker, versionBeforeUpdate, false);

                timer.cancel();
                LOGGER.info("MongoDbUpdateStatusTask stopped for database '{}'", database.getName());
            }
        } catch (Exception e) {
            LOGGER.error(String.format("MongoDbUpdateStatusTask failed: %s", e.getMessage()), e);
        }
    }

    public void setVersionBeforeUpdate(Long versionBeforeUpdate) {
        this.versionBeforeUpdate = versionBeforeUpdate;
    }
}
