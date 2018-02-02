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

import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.document.GetItemOutcome;
import software.amazon.awssdk.services.dynamodb.document.Item;
import software.amazon.awssdk.services.dynamodb.document.KeyAttribute;
import software.amazon.awssdk.services.dynamodb.document.PrimaryKey;
import software.amazon.awssdk.services.dynamodb.document.Table;
import software.amazon.awssdk.services.dynamodb.document.api.GetItemApi;
import software.amazon.awssdk.services.dynamodb.document.spec.GetItemSpec;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

/**
 * The implementation for <code>GetItemApi</code>.
 */
public class GetItemImpl extends AbstractImpl implements GetItemApi {
    public GetItemImpl(DynamoDBClient client, Table table) {
        super(client, table);
    }

    @Override
    public GetItemOutcome getItemOutcome(KeyAttribute... primaryKeyComponents) {
        return doLoadItem(new GetItemSpec()
                                  .withPrimaryKey(primaryKeyComponents));
    }

    @Override
    public GetItemOutcome getItemOutcome(PrimaryKey primaryKey) {
        return doLoadItem(new GetItemSpec()
                                  .withPrimaryKey(primaryKey));
    }

    @Override
    public GetItemOutcome getItemOutcome(PrimaryKey primaryKey,
                                         String projectionExpression, Map<String, String> nameMap) {
        return doLoadItem(new GetItemSpec()
                                  .withPrimaryKey(primaryKey)
                                  .withProjectionExpression(projectionExpression)
                                  .withNameMap(nameMap));
    }

    @Override
    public GetItemOutcome getItemOutcome(GetItemSpec spec) {
        return doLoadItem(spec);
    }

    @Override
    public Item getItem(GetItemSpec spec) {
        return doLoadItem(spec).getItem();
    }

    private GetItemOutcome doLoadItem(GetItemSpec spec) {
        String tableName = getTable().getTableName();
        // Set up the key attributes
        GetItemRequest req = spec.getRequest().toBuilder()
                .tableName(tableName)
                .key(InternalUtils.toAttributeValueMap(spec.getKeyComponents()))
                .expressionAttributeNames(spec.nameMap())
                .build();

        GetItemResponse result = getClient().getItem(req);
        return new GetItemOutcome(result);
    }

    @Override
    public Item getItem(KeyAttribute... primaryKey) {
        return getItemOutcome(primaryKey).getItem();
    }

    @Override
    public Item getItem(PrimaryKey primaryKey) {
        return getItemOutcome(primaryKey).getItem();
    }

    @Override
    public Item getItem(PrimaryKey primaryKey, String projectionExpression,
                        Map<String, String> nameMap) {
        return getItemOutcome(primaryKey, projectionExpression, nameMap).getItem();
    }

    @Override
    public GetItemOutcome getItemOutcome(String hashKeyName, Object hashKeyValue) {
        return getItemOutcome(new KeyAttribute(hashKeyName, hashKeyValue));
    }

    @Override
    public GetItemOutcome getItemOutcome(String hashKeyName, Object hashKeyValue,
                                         String rangeKeyName, Object rangeKeyValue) {
        return getItemOutcome(new KeyAttribute(hashKeyName, hashKeyValue),
                              new KeyAttribute(rangeKeyName, rangeKeyValue));
    }

    @Override
    public Item getItem(String hashKeyName, Object hashKeyValue) {
        return getItemOutcome(hashKeyName, hashKeyValue).getItem();
    }

    @Override
    public Item getItem(String hashKeyName, Object hashKeyValue,
                        String rangeKeyName, Object rangeKeyValue) {
        return getItemOutcome(hashKeyName, hashKeyValue, rangeKeyName, rangeKeyValue).getItem();
    }

    @Override
    public GetItemOutcome getItemOutcome(String hashKeyName, Object hashKeyValue,
                                         String projectionExpression, Map<String, String> nameMap) {
        return getItemOutcome(new PrimaryKey(hashKeyName, hashKeyValue),
                              projectionExpression, nameMap);
    }

    @Override
    public GetItemOutcome getItemOutcome(String hashKeyName, Object hashKeyValue,
                                         String rangeKeyName, Object rangeKeyValue,
                                         String projectionExpression, Map<String, String> nameMap) {
        return getItemOutcome(
                new PrimaryKey(hashKeyName, hashKeyValue, rangeKeyName, rangeKeyValue),
                projectionExpression, nameMap);
    }

    @Override
    public Item getItem(String hashKeyName, Object hashKeyValue,
                        String projectionExpression, Map<String, String> nameMap) {
        return getItemOutcome(new PrimaryKey(hashKeyName, hashKeyValue),
                              projectionExpression, nameMap).getItem();
    }

    @Override
    public Item getItem(String hashKeyName, Object hashKeyValue,
                        String rangeKeyName, Object rangeKeyValue,
                        String projectionExpression, Map<String, String> nameMap) {
        return getItemOutcome(
                new PrimaryKey(hashKeyName, hashKeyValue, rangeKeyName, rangeKeyValue),
                projectionExpression, nameMap).getItem();
    }
}
