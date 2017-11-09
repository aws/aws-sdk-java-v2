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

import static software.amazon.awssdk.services.dynamodb.document.internal.InternalUtils.toItemList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.document.Item;
import software.amazon.awssdk.services.dynamodb.document.Page;
import software.amazon.awssdk.services.dynamodb.document.ScanOutcome;
import software.amazon.awssdk.services.dynamodb.document.spec.ScanSpec;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

class ScanPage extends Page<Item, ScanOutcome> {

    private final DynamoDBClient client;
    private final ScanSpec spec;
    private ScanRequest request;
    private final int index;
    private final Map<String, AttributeValue> lastEvaluatedKey;

    ScanPage(
        DynamoDBClient client,
        ScanSpec spec,
        ScanRequest request,
        int index,
        ScanOutcome outcome) {
        super(Collections.unmodifiableList(
                toItemList(outcome.scanResult().items())),
              outcome);
        this.client = client;
        this.spec = spec;
        this.request = request;
        this.index = index;

        final Integer max = spec.maxResultSize();
        final ScanResponse result = outcome.scanResult();
        final List<?> ilist = result.items();
        final int size = ilist == null ? 0 : ilist.size();
        if (max != null && (index + size) > max) {
            this.lastEvaluatedKey = null;
        } else {
            this.lastEvaluatedKey = result.lastEvaluatedKey();
        }
    }

    @Override
    public boolean hasNextPage() {
        if (lastEvaluatedKey == null) {
            return false;
        }
        Integer max = spec.maxResultSize();
        if (max == null) {
            return true;
        }
        return nextRequestLimit(max.intValue()) > 0;
    }

    private int nextRequestLimit(int max) {
        int nextIndex = index + this.size();
        return InternalUtils.minimum(
                max - nextIndex,
                spec.maxPageSize());
    }

    @Override
    public Page<Item, ScanOutcome> nextPage() {
        if (lastEvaluatedKey == null) {
            throw new NoSuchElementException("No more pages");
        }
        final Integer max = spec.maxResultSize();
        if (max != null) {
            int nextLimit = nextRequestLimit(max.intValue());
            if (nextLimit == 0) {
                throw new NoSuchElementException("No more pages");
            }
            request = request.toBuilder().limit(nextLimit).build();
        }
        request = request.toBuilder().exclusiveStartKey(lastEvaluatedKey).build();
        // fire off request to the server side
        ScanResponse result = client.scan(request);
        final int nextIndex = index + this.size();
        return new ScanPage(client, spec, request, nextIndex,
                            new ScanOutcome(result));
    }
}
