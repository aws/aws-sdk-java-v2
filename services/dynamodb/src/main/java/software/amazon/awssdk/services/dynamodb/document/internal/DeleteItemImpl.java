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

import java.util.Collection;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.document.DeleteItemOutcome;
import software.amazon.awssdk.services.dynamodb.document.Expected;
import software.amazon.awssdk.services.dynamodb.document.KeyAttribute;
import software.amazon.awssdk.services.dynamodb.document.PrimaryKey;
import software.amazon.awssdk.services.dynamodb.document.Table;
import software.amazon.awssdk.services.dynamodb.document.api.DeleteItemApi;
import software.amazon.awssdk.services.dynamodb.document.spec.DeleteItemSpec;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ExpectedAttributeValue;

/**
 * The implementation for <code>DeleteItemApi</code>.
 */
public class DeleteItemImpl extends AbstractImpl implements DeleteItemApi {
    public DeleteItemImpl(DynamoDBClient client, Table table) {
        super(client, table);
    }

    @Override
    public DeleteItemOutcome deleteItem(KeyAttribute... primaryKeyComponents) {
        return doDeleteItem(new DeleteItemSpec()
                                    .withPrimaryKey(primaryKeyComponents));
    }

    @Override
    public DeleteItemOutcome deleteItem(PrimaryKey primaryKey) {
        return doDeleteItem(new DeleteItemSpec()
                                    .withPrimaryKey(primaryKey));
    }

    @Override
    public DeleteItemOutcome deleteItem(PrimaryKey primaryKeys,
                                        Expected... expected) {
        return doDeleteItem(new DeleteItemSpec()
                                    .withPrimaryKey(primaryKeys)
                                    .withExpected(expected));
    }

    @Override
    public DeleteItemOutcome deleteItem(PrimaryKey primaryKeys,
                                        String conditionExpression, Map<String, String> nameMap,
                                        Map<String, Object> valueMap) {
        return doDeleteItem(new DeleteItemSpec()
                                    .withPrimaryKey(primaryKeys)
                                    .withConditionExpression(conditionExpression)
                                    .withNameMap(nameMap)
                                    .valueMap(valueMap))
                ;
    }

    @Override
    public DeleteItemOutcome deleteItem(DeleteItemSpec spec) {
        return doDeleteItem(spec);
    }

    private DeleteItemOutcome doDeleteItem(DeleteItemSpec spec) {
        // set the table name
        final String tableName = getTable().getTableName();
        // set up the keys
        DeleteItemRequest.Builder requestBuilder = spec.getRequest().toBuilder()
                .tableName(tableName)
                .key(InternalUtils.toAttributeValueMap(spec.getKeyComponents()));
        // set up the expected attribute map, if any
        final Collection<Expected> expected = spec.getExpected();
        final Map<String, ExpectedAttributeValue> expectedMap =
                InternalUtils.toExpectedAttributeValueMap(expected);
        // set up the value map, if any (when expression API is used)
        final Map<String, AttributeValue> attrValMap =
                InternalUtils.fromSimpleMap(spec.valueMap());
        // set up the request
        requestBuilder.expected(expectedMap)
               .expressionAttributeNames(spec.nameMap())
               .expressionAttributeValues(attrValMap);
        DeleteItemResponse result = getClient().deleteItem(requestBuilder.build());
        return new DeleteItemOutcome(result);
    }

    @Override
    public DeleteItemOutcome deleteItem(String hashKeyName, Object hashKeyValue) {
        return deleteItem(new PrimaryKey(hashKeyName, hashKeyValue));
    }

    @Override
    public DeleteItemOutcome deleteItem(String hashKeyName,
                                        Object hashKeyValue, String rangeKeyName, Object rangeKeyValue) {
        return deleteItem(
                new PrimaryKey(hashKeyName, hashKeyValue, rangeKeyName, rangeKeyValue));
    }

    @Override
    public DeleteItemOutcome deleteItem(String hashKeyName,
                                        Object hashKeyValue, Expected... expected) {
        return deleteItem(new PrimaryKey(hashKeyName, hashKeyValue), expected);
    }

    @Override
    public DeleteItemOutcome deleteItem(String hashKeyName,
                                        Object hashKeyValue, String rangeKeyName, Object rangeKeyValue,
                                        Expected... expected) {
        return deleteItem(
                new PrimaryKey(hashKeyName, hashKeyValue, rangeKeyName, rangeKeyValue),
                expected);
    }

    @Override
    public DeleteItemOutcome deleteItem(String hashKeyName,
                                        Object hashKeyValue, String conditionExpression,
                                        Map<String, String> nameMap, Map<String, Object> valueMap) {
        return deleteItem(new PrimaryKey(hashKeyName, hashKeyValue),
                          conditionExpression, nameMap, valueMap);
    }

    @Override
    public DeleteItemOutcome deleteItem(String hashKeyName,
                                        Object hashKeyValue, String rangeKeyName, Object rangeKeyValue,
                                        String conditionExpression, Map<String, String> nameMap,
                                        Map<String, Object> valueMap) {
        return deleteItem(
                new PrimaryKey(hashKeyName, hashKeyValue, rangeKeyName, rangeKeyValue),
                conditionExpression, nameMap, valueMap);
    }
}
