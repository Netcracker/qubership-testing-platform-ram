package org.qubership.atp.ram.migration.mongoevolution.java.annotation;

import java.lang.reflect.Method;
import java.util.Comparator;

import org.bson.Document;
import org.qubership.atp.ram.migration.mongoevolution.java.MongoEvolution;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString(of = {"version", "orderChangeLog", "orderChangeSet", "changeSetMethod"})
@EqualsAndHashCode(of = {"version", "orderChangeLog", "orderChangeSet", "changeSetMethod", "changeLogInstance"})
public class ChangeEntry {

    public static final Comparator<ChangeEntry> COMPARATOR = Comparator
            .comparingLong(ChangeEntry::getVersion)
            .thenComparingInt(ChangeEntry::getOrderChangeLog)
            .thenComparingInt(ChangeEntry::getOrderChangeSet);

    public static final String CHANGELOG_COLLECTION = "_schema_change_log";

    private static final String KEY_VERSION = "version";
    private static final String KEY_CHANGELOGCLASS = "changeLogClass";
    private static final String KEY_CHANGESETMETHOD = "changeSetMethod";
    private static final String KEY_CHANGELOG_ORDER = "orderChangeLog";
    private static final String KEY_CHANGESET_ORDER = "orderChangeSet";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_UPDATETIME = "timeToUpdate";

    private final long version;
    private final int orderChangeLog;
    private final int orderChangeSet;
    private final long dateInMillis;
    @NonNull
    private final Method changeSetMethod;
    @NonNull
    private final Object changeLogInstance;

    /**
     * Constructs a new {@link ChangeEntry} representing a single change set to be applied to the database schema.
     * <p/>
     * Each {@code ChangeEntry} encapsulates metadata about the change, including its version,
     * execution order, associated method, and the instance of the change log class.
     * The timestamp of creation is automatically recorded.
     *
     * @param version           The version number of the change log to which this change set belongs.
     * @param orderChangeLog    The execution order of the change log within the specified version.
     * @param orderChangeSet    The execution order of this change set within the change log.
     * @param changeSetMethod   The method annotated as a change set to be executed. Must not be {@code null}.
     * @param changeLogInstance The instance of the class containing the change set method. Must not be {@code null}.
     */
    public ChangeEntry(long version,
                       int orderChangeLog,
                       int orderChangeSet,
                       @NonNull Method changeSetMethod,
                       @NonNull Object changeLogInstance) {
        this.version = version;
        this.orderChangeLog = orderChangeLog;
        this.orderChangeSet = orderChangeSet;
        this.dateInMillis = System.currentTimeMillis();
        this.changeSetMethod = changeSetMethod;
        this.changeLogInstance = changeLogInstance;
    }

    public String getChangeClassName() {
        return changeSetMethod.getDeclaringClass().getName();
    }

    public String getChangeMethodName() {
        return changeSetMethod.getName();
    }

    /**
     * Builds a MongoDB {@link Document} representing this change entry, including metadata such as
     * version, execution order, timestamps, and method identifiers.
     * <p/>
     * The document can be used for persistence in a MongoDB collection to track applied schema changes.
     *
     * @param updateTimeInMillis The time taken (in milliseconds) to apply the change set, calculated as the difference
     *                           between the current time and the creation time of this entry.
     * @return A {@link Document} containing all relevant fields describing this change entry.
     */
    public Document buildFullDBObject(long updateTimeInMillis) {
        return new Document()
                .append(KEY_VERSION, version)
                .append(KEY_CHANGELOG_ORDER, orderChangeLog)
                .append(KEY_CHANGESET_ORDER, orderChangeSet)
                .append(KEY_TIMESTAMP, dateInMillis)
                .append(KEY_UPDATETIME, updateTimeInMillis)
                .append(KEY_CHANGELOGCLASS, getChangeClassName())
                .append(KEY_CHANGESETMETHOD, getChangeMethodName());
    }

    /**
     * Saves this {@link ChangeEntry} to the MongoDB change log collection.
     * <p/>
     * The method inserts a document representing the change entry into the {@code _schema_change_log} collection.
     * After insertion, it updates the {@code timestamp} field to reflect the current MongoDB server time.
     * <p/>
     * This ensures that each applied change set is properly recorded for future reference
     * and prevents re-execution of the same change set.
     *
     * @param db The {@link MongoDatabase} instance where the change log collection is located.
     */
    public void saveEntryInChangeLog(MongoDatabase db) {
        long updateTimeInMillis = System.currentTimeMillis() - dateInMillis;
        Document entryDoc = buildFullDBObject(updateTimeInMillis);
        MongoCollection<Document> collection = db.getCollection(CHANGELOG_COLLECTION);
        collection.insertOne(entryDoc);

        BasicDBObject query = (new BasicDBObject()).append("_id", entryDoc.get("_id"));
        MongoEvolution.updateFieldWithMongoCurrentDate(collection, KEY_TIMESTAMP, query);
    }
}
