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
import java.util.concurrent.TimeUnit;

import org.bson.BsonTimestamp;
import org.bson.Document;
import org.qubership.atp.ram.migration.mongoevolution.java.annotation.AnnotationProcessor;
import org.qubership.atp.ram.migration.mongoevolution.java.dataaccess.ConnectionSearchKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;


public abstract class AbstractMongoEvolution {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMongoEvolution.class);

    private static final long INITIAL_VERSION = 0L;
    public static final String TRACKER_COLLECTION = "_schema_evolution";

    public static final String TRACKER_KEY_UPDATE_START = "startTime";
    public static final String TRACKER_KEY_UPDATE_END = "endTime";
    public static final String TRACKER_KEY_UPDATE_LAST = "lastUpdateTime";
    public static final String TRACKER_IN_PROGRESS = "inProgress";
    public static final String TRACKER_CURRENT_VERSION = "currentVersion";

    public static final int WAIT_TIME_SECONDS_FOR_UPDATE = 30;
    public static final int WAIT_TIME_MILLISEC_WITHIN_UPDATE = 1000;

    private static final int ERR_CODE_MONGO_DUPLICATE_KEY = 11000;

    public static final long WAIT_TIME_FOR_UPDATE_STATUS_TASK = TimeUnit.MINUTES.toMillis(5);
    public static final long DELAY_STATUS_TASK = TimeUnit.SECONDS.toMillis(30);

    private final MongoClient client;
    private final String dbName;
    private final ConnectionSearchKey connectionSearchKey;
    private final MongoDatabase database;

    private MongoDbUpdateStatusTask statusTask = null;

    protected AbstractMongoEvolution(MongoClient client, String dbName, ConnectionSearchKey connectionSearchKey) {
        this.client = client;
        this.dbName = dbName;
        this.connectionSearchKey = connectionSearchKey;
        this.database = client.getDatabase(dbName);
    }

    protected void doEvolve(AnnotationProcessor processor) throws Exception {
        Timer updateStatusTask = startMongoDbUpdateStatusTask();
        Long currentVersion = null;

        try {
            waitForUnlock();

            Long expectedVersion = processor.getMaxChangeLogVersion();
            boolean finishUpdate = false;

            while (!finishUpdate) {
                currentVersion = getDbCurrentVersion();
                if (expectedVersion > currentVersion) {
                    statusTask.setVersionBeforeUpdate(currentVersion);

                    MongoCollection<Document> updatesTracker = database.getCollection(TRACKER_COLLECTION);
                    boolean startUpdate = insertUpdateFlag(updatesTracker, currentVersion, true);

                    if (startUpdate) {
                        updateFieldWithMongoCurrentDate(updatesTracker, TRACKER_KEY_UPDATE_START, null);
                        updateFieldWithMongoCurrentDate(updatesTracker, TRACKER_KEY_UPDATE_LAST, null);

                        processor.applyChanges(currentVersion);

                        finishUpdate = insertUpdateFlag(updatesTracker, expectedVersion, false);
                        if (finishUpdate) {
                            updateFieldWithMongoCurrentDate(updatesTracker, TRACKER_KEY_UPDATE_END, null);
                        }
                    } else {
                        sleepMillis(WAIT_TIME_MILLISEC_WITHIN_UPDATE);
                    }
                } else {
                    finishUpdate = true;
                }
            }

        } catch (Exception e) {
            LOGGER.warn("Exception during evolve. Attempting to reset inProgress flag...");
            handleEvolveException(currentVersion);
            throw new Exception("executeChangeLogUpdate Exception during update.", e);
        } finally {
            updateStatusTask.cancel();
        }
    }

    private void waitForUnlock() throws Exception {
        while (isUpdateInProgress()) {
            LOGGER.info("Update in progress detected. Waiting {} seconds...", WAIT_TIME_SECONDS_FOR_UPDATE);
            Thread.sleep(TimeUnit.SECONDS.toMillis(WAIT_TIME_SECONDS_FOR_UPDATE));
        }
    }

    private void handleEvolveException(Long currentVersion) {
        try {
            MongoCollection<Document> updatesTracker = database.getCollection(TRACKER_COLLECTION);
            Long expectedVersion = getDbCurrentVersion();

            if (currentVersion != null && expectedVersion > currentVersion) {
                insertUpdateFlag(updatesTracker, expectedVersion, false);
            }
            if (currentVersion != null) {
                insertUpdateFlag(updatesTracker, currentVersion, false);
            }
        } catch (Exception ex) {
            LOGGER.error(String.format("Failed to reset inProgress flag for DB: %s", dbName), ex);
        }
    }

    private Timer startMongoDbUpdateStatusTask() {
        Timer updateStatusTimer = new Timer();
        this.statusTask = new MongoDbUpdateStatusTask(updateStatusTimer, this, database);
        updateStatusTimer.schedule(statusTask, DELAY_STATUS_TASK, DELAY_STATUS_TASK);
        return updateStatusTimer;
    }

    /**
     * Updates the specified field in a MongoDB document with the current server timestamp.
     *
     * <p>This method performs an {@code updateOne} operation using the {@code $currentDate} operator
     * to set the provided field to the current MongoDB server time.
     *
     * <p>If no query is provided, the update applies to the first document in the collection.
     *
     * @param collection The MongoDB collection where the update will be performed.
     * @param fieldName  The name of the field to be updated with the current timestamp.
     * @param query      The query to select the document for update. If {@code null}, a default query is used.
     */
    public static void updateFieldWithMongoCurrentDate(MongoCollection<Document> collection,
                                                       String fieldName,
                                                       BasicDBObject query) {
        BasicDBObject effectiveQuery = query != null ? query : new BasicDBObject();
        BasicDBObject update = new BasicDBObject("$currentDate",
                new BasicDBObject(fieldName, new BasicDBObject("$type", "timestamp")));
        collection.updateOne(effectiveQuery, update, new UpdateOptions());
    }

    /**
     * Creates a new tracker document representing the state of a schema evolution process.
     *
     * <p>This document is used to track the start and end times of an update, whether an update is in progress,
     * and the current version of the database schema.
     *
     * @param dateStart  The start time of the update process in milliseconds.
     * @param dateEnd    The end time of the update process in milliseconds.
     * @param inProgress Indicates whether an update is currently in progress.
     * @param version    The current schema version.
     * @return A {@link Document} containing the tracking information for schema evolution.
     */
    public Document createTrackerCollectionRecord(long dateStart, long dateEnd, boolean inProgress, long version) {
        return new Document()
                .append(TRACKER_KEY_UPDATE_START, dateStart)
                .append(TRACKER_IN_PROGRESS, inProgress)
                .append(TRACKER_KEY_UPDATE_END, dateEnd)
                .append(TRACKER_CURRENT_VERSION, version)
                .append(TRACKER_KEY_UPDATE_LAST, dateStart);
    }

    /**
     * Checks if the database update lock is still considered "alive".
     *
     * <p>This method verifies the time elapsed since the last update status timestamp.
     * If the difference is within the allowed threshold ({@code WAIT_TIME_FOR_UPDATE_STATUS_TASK}),
     * the lock is considered active.
     *
     * @return {@code true} if the update lock is active; {@code false} otherwise.
     */
    public boolean isDatabaseUpdateLockAlive() {
        MongoCollection<Document> updatesTracker = database.getCollection(TRACKER_COLLECTION);
        Document doc = updatesTracker.find().first();
        if (doc == null) {
            return false;
        }

        long lastUpdateStatusTimeMillis = 1000L * ((BsonTimestamp) doc.get(TRACKER_KEY_UPDATE_LAST)).getTime();
        long millisecDiff = System.currentTimeMillis() - lastUpdateStatusTimeMillis;
        return millisecDiff <= WAIT_TIME_FOR_UPDATE_STATUS_TASK;
    }

    /**
     * Determines whether a schema update process is currently in progress.
     *
     * <p>This method checks the {@code inProgress} flag in the tracker collection.
     * If the tracker document does not exist, it initializes the tracker.
     *
     * <p>Handles potential concurrent update scenarios and MongoDB-specific exceptions.
     *
     * @return {@code true} if an update is in progress; {@code false} otherwise.
     */
    public boolean isUpdateInProgress() {
        MongoCollection<Document> updatesTracker = database.getCollection(TRACKER_COLLECTION);

        try {
            Document doc = updatesTracker.find().first();
            if (doc == null) {
                initializeTracker(updatesTracker);
                return false;
            }
            return doc.getBoolean(TRACKER_IN_PROGRESS);
        } catch (MongoWriteException ex) {
            if (ex.getError().getCode() == ERR_CODE_MONGO_DUPLICATE_KEY) {
                LOGGER.debug("Concurrent insertion detected: {}", ex.getMessage());
                return true;
            }
            throw ex;
        } catch (Exception exception) {
            LOGGER.error(String.format("Failed to check update status: %s", exception.getMessage()), exception);
            throw exception;
        }
    }

    private void initializeTracker(MongoCollection<Document> updatesTracker) {
        long currentTime = System.currentTimeMillis();
        Document doc = createTrackerCollectionRecord(currentTime, currentTime, false, INITIAL_VERSION);
        updatesTracker.createIndex(new Document(TRACKER_IN_PROGRESS, 1), new IndexOptions().unique(true));
        updatesTracker.insertOne(doc);
        updateFieldWithMongoCurrentDate(updatesTracker, TRACKER_KEY_UPDATE_START, null);
        updateFieldWithMongoCurrentDate(updatesTracker, TRACKER_KEY_UPDATE_END, null);
    }

    boolean insertUpdateFlag(MongoCollection<Document> collection, Long expectedVersion, boolean updateInProgress) {
        try {
            long currentTime = System.currentTimeMillis();
            Document newDoc = new Document(TRACKER_IN_PROGRESS, updateInProgress);
            if (expectedVersion != null) {
                newDoc.append(TRACKER_CURRENT_VERSION, expectedVersion);
            }
            newDoc.append(updateInProgress ? TRACKER_KEY_UPDATE_START : TRACKER_KEY_UPDATE_END, currentTime);

            Document previousDoc = collection.findOneAndUpdate(Filters.eq(TRACKER_IN_PROGRESS, !updateInProgress),
                    new Document("$set", newDoc));
            return previousDoc != null;

        } catch (MongoCommandException ex) {
            if (ex.getErrorCode() == ERR_CODE_MONGO_DUPLICATE_KEY) {
                LOGGER.debug("Concurrent update detected: {}", ex.getMessage());
                return false;
            }
            throw ex;
        } catch (Exception e) {
            LOGGER.error(String.format("insertUpdateFlag failed: %s", e.getMessage()), e);
            throw e;
        }
    }

    /**
     * Retrieves the current version of the database schema from the tracker collection.
     *
     * <p>If the tracker document is not present, returns the initial version ({@code 0L}).
     *
     * @return The current schema version stored in the database.
     */
    public Long getDbCurrentVersion() {
        MongoCollection<Document> updatesTracker = database.getCollection(TRACKER_COLLECTION);
        Document doc = updatesTracker.find().first();
        return doc != null ? doc.getLong(TRACKER_CURRENT_VERSION) : INITIAL_VERSION;
    }

    private void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Sleep interrupted: {}", e.getMessage());
        }
    }
}
