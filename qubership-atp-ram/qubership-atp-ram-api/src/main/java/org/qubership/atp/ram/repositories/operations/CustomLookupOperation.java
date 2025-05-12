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

package org.qubership.atp.ram.repositories.operations;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext;
import org.springframework.data.mongodb.core.aggregation.AggregationPipeline;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomLookupOperation implements AggregationOperation {
    private final String from;
    private final Document let;
    private final AggregationPipeline pipeline;
    private final String as;

    @Override
    public Document toDocument(AggregationOperationContext context) {

        Document lookupObject = new Document();

        lookupObject.append("from", from);
        lookupObject.append("let", let);

        List<Document> pipelineDocument = new ArrayList<>();
        for (AggregationOperation operation : pipeline.getOperations()) {
            pipelineDocument.add(operation.toDocument(context));
        }
        lookupObject.append("pipeline", pipelineDocument);
        lookupObject.append("as", as);

        return new Document(getOperator(), lookupObject);
    }

    @Override
    public String getOperator() {
        return "$lookup";
    }

}
