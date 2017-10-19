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

package software.amazon.awssdk.services.dynamodb.document;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.document.api.BatchGetItemApi;
import software.amazon.awssdk.services.dynamodb.document.internal.InternalUtils;
import software.amazon.awssdk.services.dynamodb.document.spec.BatchGetItemSpec;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;

/**
 * The outcome of a batch get-item operation from DynamoDB.
 */
public class BatchGetItemOutcome {
    private final BatchGetItemResponse result;

    /**
     * @param result the low-level result; must not be null
     */
    public BatchGetItemOutcome(BatchGetItemResponse result) {
        if (result == null) {
            throw new IllegalArgumentException();
        }
        this.result = result;
    }

    /**
     * Returns a map of table name to the list of retrieved items
     */
    public Map<String, List<Item>> getTableItems() {
        Map<String, List<Map<String, AttributeValue>>> res =
                result.responses();
        Map<String, List<Item>> map = new LinkedHashMap<String, List<Item>>(res.size());
        for (Map.Entry<String, List<Map<String, AttributeValue>>> e
                : res.entrySet()) {
            String tableName = e.getKey();
            List<Map<String, AttributeValue>> items = e.getValue();
            map.put(tableName, InternalUtils.toItemList(items));
        }
        return map;
    }

    /**
     * Convenient method to return the low-level unprocessed keys.
     *
     * @see BatchGetItemApi#batchGetItemUnprocessed(Map)
     * @see BatchGetItemApi#batchGetItemUnprocessed(software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity,
     *      Map)
     * @see BatchGetItemSpec#withUnprocessedKeys(Map)
     */
    public Map<String, KeysAndAttributes> getUnprocessedKeys() {
        return result.unprocessedKeys();
    }

    /**
     * Returns a non-null low-level result returned from the server side.
     */
    public BatchGetItemResponse batchGetItemResponse() {
        return result;
    }

    @Override
    public String toString() {
        return String.valueOf(result);
    }
}
