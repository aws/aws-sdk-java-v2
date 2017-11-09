/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.services.dynamodb.document.ScanOutcome;
import software.amazon.awssdk.services.dynamodb.document.spec.ScanSpec;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

class ScanCollection extends ItemCollection<ScanOutcome> {

    private final DynamoDBClient client;
    private final ScanSpec spec;
    private final Map<String, AttributeValue> startKey;

    ScanCollection(DynamoDBClient client, ScanSpec spec) {
        this.client = client;
        this.spec = spec;
        Map<String, AttributeValue> startKey = spec.getRequest()
                                                   .exclusiveStartKey();
        this.startKey = startKey == null ? null : new LinkedHashMap<String, AttributeValue>(startKey);
    }

    @Override
    public Page<Item, ScanOutcome> firstPage() {
        ScanRequest request = spec.getRequest();
        request = request.toBuilder()
                .exclusiveStartKey(startKey)
                .limit(InternalUtils.minimum(
                        spec.maxResultSize(),
                        spec.maxPageSize()))
                .build();

        spec.setRequest(request);

        ScanResponse result = client.scan(request);
        ScanOutcome outcome = new ScanOutcome(result);
        setLastLowLevelResult(outcome);
        return new ScanPage(client, spec, request, 0, outcome);
    }

    @Override
    public Integer getMaxResultSize() {
        return spec.maxResultSize();
    }

    protected void setLastLowLevelResult(ScanOutcome lowLevelResult) {
        super.setLastLowLevelResult(lowLevelResult);
        ScanResponse result = lowLevelResult.scanResult();
        accumulateStats(result.consumedCapacity(), result.count(),
                        result.scannedCount());
    }
}
