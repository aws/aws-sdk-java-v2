/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.dynamodb.document.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.document.Item;
import software.amazon.awssdk.services.dynamodb.document.ItemCollection;
import software.amazon.awssdk.services.dynamodb.document.Page;
import software.amazon.awssdk.services.dynamodb.document.QueryOutcome;
import software.amazon.awssdk.services.dynamodb.document.spec.QuerySpec;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

class QueryCollection extends ItemCollection<QueryOutcome> {
    private final DynamoDBClient client;
    private final QuerySpec spec;
    private final Map<String, AttributeValue> startKey;

    QueryCollection(DynamoDBClient client, QuerySpec spec) {
        this.client = client;
        this.spec = spec;
        Map<String, AttributeValue> startKey =
                spec.getRequest().exclusiveStartKey();
        this.startKey = startKey == null
                        ? null
                        : new LinkedHashMap<String, AttributeValue>(startKey);
    }

    @Override
    public Page<Item, QueryOutcome> firstPage() {
        QueryRequest request = spec.getRequest().toBuilder()
                .exclusiveStartKey(startKey)
                .limit(InternalUtils.minimum(
                    spec.maxResultSize(),
                    spec.maxPageSize()))
                .build();
        spec.setRequest(request);
        QueryResponse result = client.query(request);
        QueryOutcome outcome = new QueryOutcome(result);
        setLastLowLevelResult(outcome);
        return new QueryPage(client, spec, request, 0, outcome);
    }

    @Override
    public Integer getMaxResultSize() {
        return spec.maxResultSize();
    }

    protected void setLastLowLevelResult(QueryOutcome lowLevelResult) {
        super.setLastLowLevelResult(lowLevelResult);
        QueryResponse result = lowLevelResult.getQueryResponse();
        accumulateStats(result.consumedCapacity(), result.count(),
                        result.scannedCount());
    }
}
