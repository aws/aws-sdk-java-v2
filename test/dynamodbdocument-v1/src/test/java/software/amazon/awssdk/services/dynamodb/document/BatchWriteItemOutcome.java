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

package software.amazon.awssdk.services.dynamodb.document;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.document.api.BatchWriteItemApi;
import software.amazon.awssdk.services.dynamodb.document.spec.BatchWriteItemSpec;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * The outcome of a batch write-item operation from DynamoDB.
 */
public class BatchWriteItemOutcome {
    private final BatchWriteItemResponse result;

    /**
     * @param result the low-level result; must not be null
     */
    public BatchWriteItemOutcome(BatchWriteItemResponse result) {
        if (result == null) {
            throw new IllegalArgumentException();
        }
        this.result = result;
    }

    /**
     * Convenient method to return the low-level unprocessed items.
     *
     * @see BatchWriteItemApi#batchWriteItemUnprocessed(Map)
     * @see BatchWriteItemSpec#withUnprocessedItems(Map)
     */
    public Map<String, List<WriteRequest>> getUnprocessedItems() {
        return result.unprocessedItems();
    }

    /**
     * Returns a non-null low-level result returned from the server side.
     */
    public BatchWriteItemResponse batchWriteItemResult() {
        return result;
    }

    @Override
    public String toString() {
        return String.valueOf(result);
    }
}
