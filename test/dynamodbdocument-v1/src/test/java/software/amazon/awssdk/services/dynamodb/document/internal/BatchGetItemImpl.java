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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.document.BatchGetItemOutcome;
import software.amazon.awssdk.services.dynamodb.document.PrimaryKey;
import software.amazon.awssdk.services.dynamodb.document.TableKeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.document.api.BatchGetItemApi;
import software.amazon.awssdk.services.dynamodb.document.spec.BatchGetItemSpec;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;

/**
 * The implementation for <code>BatchGetItemApi</code>.
 */
public class BatchGetItemImpl implements BatchGetItemApi {
    private final DynamoDbClient client;

    public BatchGetItemImpl(DynamoDbClient client) {
        this.client = client;
    }

    @Override
    public BatchGetItemOutcome batchGetItem(
            ReturnConsumedCapacity returnConsumedCapacity,
            TableKeysAndAttributes... tableKeysAndAttributes) {
        return doBatchGetItem(new BatchGetItemSpec()
                                      .withReturnConsumedCapacity(returnConsumedCapacity)
                                      .withTableKeyAndAttributes(tableKeysAndAttributes));
    }

    @Override
    public BatchGetItemOutcome batchGetItem(
            TableKeysAndAttributes... tableKeysAndAttributes) {
        return doBatchGetItem(new BatchGetItemSpec()
                                      .withTableKeyAndAttributes(tableKeysAndAttributes));
    }

    @Override
    public BatchGetItemOutcome batchGetItem(BatchGetItemSpec spec) {
        return doBatchGetItem(spec);
    }

    private BatchGetItemOutcome doBatchGetItem(BatchGetItemSpec spec) {
        final Collection<TableKeysAndAttributes> tableKeysAndAttributesCol =
                spec.getTableKeysAndAttributes();
        // Unprocessed keys take precedence
        Map<String, KeysAndAttributes> requestItems = spec.getUnprocessedKeys();
        if (requestItems == null || requestItems.size() == 0) {
            // handle new requests only if there is no unprocessed keys
            requestItems = new LinkedHashMap<String, KeysAndAttributes>();
        }
        if (tableKeysAndAttributesCol != null) {
            for (TableKeysAndAttributes tableKeysAndAttributes : tableKeysAndAttributesCol) {
                // attributes against one table
                final Set<String> attrNames = tableKeysAndAttributes.getAttributeNames();
                // primary keys against one table
                final List<PrimaryKey> pks = tableKeysAndAttributes.getPrimaryKeys();
                final List<Map<String, AttributeValue>> keys = new ArrayList<Map<String, AttributeValue>>(pks.size());
                for (PrimaryKey pk : pks) {
                    keys.add(InternalUtils.toAttributeValueMap(pk));
                }
                final KeysAndAttributes keysAndAttrs = KeysAndAttributes.builder()
                        .attributesToGet(attrNames)
                        .consistentRead(tableKeysAndAttributes.isConsistentRead())
                        .keys(keys)
                        .projectionExpression(tableKeysAndAttributes.getProjectionExpression())
                        .expressionAttributeNames(tableKeysAndAttributes.nameMap())
                        .build();
                requestItems.put(tableKeysAndAttributes.getTableName(), keysAndAttrs);
            }
        }
        BatchGetItemRequest req = spec.getRequest()
                .toBuilder()
                .requestItems(requestItems)
                .build();
        spec.setRequest(req);
        BatchGetItemResponse result = client.batchGetItem(req);
        return new BatchGetItemOutcome(result);
    }

    @Override
    public BatchGetItemOutcome batchGetItemUnprocessed(
            ReturnConsumedCapacity returnConsumedCapacity,
            Map<String, KeysAndAttributes> unprocessedKeys) {
        return doBatchGetItem(new BatchGetItemSpec()
                                      .withReturnConsumedCapacity(returnConsumedCapacity)
                                      .withUnprocessedKeys(unprocessedKeys));
    }

    @Override
    public BatchGetItemOutcome batchGetItemUnprocessed(
            Map<String, KeysAndAttributes> unprocessedKeys) {
        return doBatchGetItem(new BatchGetItemSpec()
                                      .withUnprocessedKeys(unprocessedKeys));
    }
}
