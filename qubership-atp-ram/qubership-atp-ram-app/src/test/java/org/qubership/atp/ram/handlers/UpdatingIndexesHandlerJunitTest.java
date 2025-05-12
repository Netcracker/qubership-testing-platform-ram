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

package org.qubership.atp.ram.handlers;

import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bson.BsonValue;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.qubership.atp.ram.migration.MigrationConstants;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.test.context.junit4.SpringRunner;

import com.mongodb.Function;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;

@RunWith(SpringRunner.class)
public class UpdatingIndexesHandlerJunitTest {

    private UpdatingIndexesHandler updatingIndexesHandler;

    @MockBean
    private MongoTemplate mongoTemplate;

    @Before
    public void setUp() throws Exception {
        updatingIndexesHandler = Mockito.spy(new UpdatingIndexesHandler(mongoTemplate));
        Mockito.when(mongoTemplate.getConverter()).thenReturn(Mockito.mock(MongoConverter.class));
    }

    @Test
    public void checkAndRecreateIndexes_WhenIndexesChanging_ShouldUpdateIndexesInDB() {
        long newValueLr = 90L;
        long newValueContextLr = 80L;
        MongoCollection<Document> mongoCollection = Mockito.mock(MongoCollection.class);
        List<Document> documentsList = new ArrayList<>();
        Document document = new Document();
        document.put(MigrationConstants.NAME_INDEX_FIELD, MigrationConstants.CREATED_DATE_INDEX_NAME);
        document.put(MigrationConstants.EXPIRE_DATE_INDEX_FIELD, 100L);
        documentsList.add(document);
        ListIndexesIterable<Document> documents = new ListIndexesIterable<Document>() {
            @Override
            public ListIndexesIterable<Document> maxTime(long maxTime, TimeUnit timeUnit) {
                return null;
            }

            @Override
            public ListIndexesIterable<Document> batchSize(int batchSize) {
                return null;
            }

            @Override
            public ListIndexesIterable<Document> comment(String comment) {
                return null;
            }

            @Override
            public ListIndexesIterable<Document> comment(BsonValue comment) {
                return null;
            }

            @Override
            public MongoCursor<Document> iterator() {
                return null;
            }

            @Override
            public MongoCursor<Document> cursor() {
                return null;
            }

            @Override
            public Document first() {
                return null;
            }

            @Override
            public <U> MongoIterable<U> map(Function<Document, U> mapper) {
                return null;
            }

            @Override
            public <A extends Collection<? super Document>> A into(A target) {
                return null;
            }

            @Override
            public void forEach(Consumer<? super Document> action) {
                documentsList.forEach(action);
            }
        };

        Mockito.when(mongoCollection.listIndexes()).thenReturn(documents);
        Mockito.when(mongoTemplate.getCollection(any()))
                .thenReturn(mongoCollection);
        updatingIndexesHandler.checkAndRecreateIndexes(newValueLr, newValueContextLr);
        Mockito.verify(updatingIndexesHandler)
                .recreateIndex(MigrationConstants.LOG_RECORDS_COLLECTION_NAME,
                        MigrationConstants.CREATED_DATE_INDEX_NAME, newValueLr);
    }
}
