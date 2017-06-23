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
import software.amazon.awssdk.services.dynamodb.document.AttributeUpdate;
import software.amazon.awssdk.services.dynamodb.document.Expected;
import software.amazon.awssdk.services.dynamodb.document.PrimaryKey;
import software.amazon.awssdk.services.dynamodb.document.Table;
import software.amazon.awssdk.services.dynamodb.document.UpdateItemOutcome;
import software.amazon.awssdk.services.dynamodb.document.api.UpdateItemApi;
import software.amazon.awssdk.services.dynamodb.document.spec.UpdateItemSpec;
import software.amazon.awssdk.services.dynamodb.model.ExpectedAttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

/**
 * The implementation for <code>UpdateItemApi</code>.
 */
public class UpdateItemImpl implements UpdateItemApi {

    private final Table table;
    private final DynamoDBClient client;

    public UpdateItemImpl(DynamoDBClient client, Table table) {
        this.client = client;
        this.table = table;
    }

    @Override
    public UpdateItemOutcome updateItem(PrimaryKey primaryKey,
                                        AttributeUpdate... attributeUpdates) {
        return updateItem(new UpdateItemSpec()
                                  .withPrimaryKey(primaryKey)
                                  .withAttributeUpdate(attributeUpdates));
    }

    @Override
    public UpdateItemOutcome updateItem(PrimaryKey primaryKey,
                                        Collection<Expected> expected, AttributeUpdate... attributeUpdates) {
        return updateItem(new UpdateItemSpec()
                                  .withPrimaryKey(primaryKey)
                                  .withExpected(expected)
                                  .withAttributeUpdate(attributeUpdates));
    }

    @Override
    public UpdateItemOutcome updateItem(PrimaryKey primaryKey,
                                        String updateExpression, Map<String, String> nameMap,
                                        Map<String, Object> valueMap) {
        return updateItem(new UpdateItemSpec()
                                  .withPrimaryKey(primaryKey)
                                  .withUpdateExpression(updateExpression)
                                  .withNameMap(nameMap)
                                  .valueMap(valueMap));
    }

    @Override
    public UpdateItemOutcome updateItem(PrimaryKey primaryKey,
                                        String updateExpression, String conditionExpression,
                                        Map<String, String> nameMap, Map<String, Object> valueMap) {

        return updateItem(new UpdateItemSpec().withPrimaryKey(primaryKey)
                                              .withUpdateExpression(updateExpression)
                                              .withConditionExpression(conditionExpression)
                                              .withNameMap(nameMap)
                                              .valueMap(valueMap));
    }

    @Override
    public UpdateItemOutcome updateItem(UpdateItemSpec spec) {
        return doUpdateItem(spec);
    }

    private UpdateItemOutcome doUpdateItem(UpdateItemSpec spec) {
        final UpdateItemRequest.Builder requestBuilder = spec.getRequest().toBuilder();
        requestBuilder.key(InternalUtils.toAttributeValueMap(spec.getKeyComponents()));
        requestBuilder.tableName(table.getTableName());
        final Collection<Expected> expected = spec.getExpected();
        final Map<String, ExpectedAttributeValue> expectedMap =
                InternalUtils.toExpectedAttributeValueMap(expected);
        requestBuilder.expected(expectedMap);
        requestBuilder.attributeUpdates(
                InternalUtils.toAttributeValueUpdate(spec.getAttributeUpdate()));
        requestBuilder.expressionAttributeNames(spec.nameMap());
        requestBuilder.expressionAttributeValues(
                InternalUtils.fromSimpleMap(spec.valueMap()));
        return new UpdateItemOutcome(client.updateItem(requestBuilder.build()));
    }

    @Override
    public UpdateItemOutcome updateItem(String hashKeyName,
                                        Object hashKeyValue, AttributeUpdate... attributeUpdates) {
        return updateItem(new PrimaryKey(hashKeyName, hashKeyValue),
                          attributeUpdates);
    }

    @Override
    public UpdateItemOutcome updateItem(String hashKeyName,
                                        Object hashKeyValue, String rangeKeyName, Object rangeKeyValue,
                                        AttributeUpdate... attributeUpdates) {
        return updateItem(new PrimaryKey(hashKeyName, hashKeyValue,
                                         rangeKeyName, rangeKeyValue), attributeUpdates);
    }

    @Override
    public UpdateItemOutcome updateItem(String hashKeyName,
                                        Object hashKeyValue, Collection<Expected> expected,
                                        AttributeUpdate... attributeUpdates) {
        return updateItem(new PrimaryKey(hashKeyName, hashKeyValue),
                          expected,
                          attributeUpdates);
    }

    @Override
    public UpdateItemOutcome updateItem(
            String hashKeyName, Object hashKeyValue,
            String rangeKeyName, Object rangeKeyValue,
            Collection<Expected> expected,
            AttributeUpdate... attributeUpdates) {
        return updateItem(new PrimaryKey(hashKeyName, hashKeyValue,
                                         rangeKeyName, rangeKeyValue),
                          expected,
                          attributeUpdates);
    }

    @Override
    public UpdateItemOutcome updateItem(String hashKeyName,
                                        Object hashKeyValue, String updateExpression,
                                        Map<String, String> nameMap, Map<String, Object> valueMap) {
        return updateItem(new PrimaryKey(hashKeyName, hashKeyValue),
                          updateExpression, nameMap, valueMap);
    }

    @Override
    public UpdateItemOutcome updateItem(String hashKeyName, Object hashKeyValue,
                                        String rangeKeyName, Object rangeKeyValue,
                                        String updateExpression, Map<String, String> nameMap,
                                        Map<String, Object> valueMap) {
        return updateItem(new PrimaryKey(hashKeyName, hashKeyValue,
                                         rangeKeyName, rangeKeyValue),
                          updateExpression, nameMap, valueMap);
    }

    @Override
    public UpdateItemOutcome updateItem(String hashKeyName,
                                        Object hashKeyValue, String updateExpression,
                                        String conditionExpression, Map<String, String> nameMap,
                                        Map<String, Object> valueMap) {
        return updateItem(new PrimaryKey(hashKeyName, hashKeyValue),
                          updateExpression, conditionExpression, nameMap, valueMap);
    }

    @Override
    public UpdateItemOutcome updateItem(String hashKeyName, Object hashKeyValue,
                                        String rangeKeyName, Object rangeKeyValue,
                                        String updateExpression, String conditionExpression,
                                        Map<String, String> nameMap, Map<String, Object> valueMap) {
        return updateItem(new PrimaryKey(hashKeyName, hashKeyValue,
                                         rangeKeyName, rangeKeyValue),
                          updateExpression, conditionExpression, nameMap, valueMap);
    }
}
