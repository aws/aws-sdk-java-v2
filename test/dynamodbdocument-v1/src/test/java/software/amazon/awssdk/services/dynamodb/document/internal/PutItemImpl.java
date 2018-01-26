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
import software.amazon.awssdk.services.dynamodb.document.Expected;
import software.amazon.awssdk.services.dynamodb.document.Item;
import software.amazon.awssdk.services.dynamodb.document.PutItemOutcome;
import software.amazon.awssdk.services.dynamodb.document.Table;
import software.amazon.awssdk.services.dynamodb.document.api.PutItemApi;
import software.amazon.awssdk.services.dynamodb.document.spec.PutItemSpec;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ExpectedAttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

/**
 * The implementation for <code>PutItemApi</code>.
 */
public class PutItemImpl extends AbstractImpl implements PutItemApi {
    public PutItemImpl(DynamoDBClient client, Table table) {
        super(client, table);
    }

    @Override
    public PutItemOutcome putItem(Item item) {
        return doPutItem(new PutItemSpec().withItem(item));
    }

    @Override
    public PutItemOutcome putItem(Item item, Expected... expected) {
        return doPutItem(new PutItemSpec()
                                 .withItem(item)
                                 .withExpected(expected));
    }

    @Override
    public PutItemOutcome putItem(Item item, String conditionExpression,
                                  Map<String, String> nameMap, Map<String, Object> valueMap) {
        return doPutItem(new PutItemSpec()
                                 .withItem(item)
                                 .withConditionExpression(conditionExpression)
                                 .withNameMap(nameMap)
                                 .valueMap(valueMap));
    }

    @Override
    public PutItemOutcome putItem(PutItemSpec spec) {
        return doPutItem(spec);
    }

    private PutItemOutcome doPutItem(PutItemSpec spec) {
        // set the table name
        String tableName = getTable().getTableName();
        PutItemRequest.Builder requestBuilder = spec.getRequest().toBuilder().tableName(tableName);
        // set up the item
        Item item = spec.getItem();
        final Map<String, AttributeValue> attributes = InternalUtils.toAttributeValues(item);
        // set up the expected attribute map, if any
        final Map<String, ExpectedAttributeValue> expectedMap =
                InternalUtils.toExpectedAttributeValueMap(spec.getExpected());
        // set up the value map, if any (when expression API is used)
        final Map<String, AttributeValue> attrValMap =
                InternalUtils.fromSimpleMap(spec.valueMap());
        // set up the request
        requestBuilder.item(attributes)
               .expected(expectedMap)
               .expressionAttributeNames(spec.nameMap())
               .expressionAttributeValues(attrValMap)
        ;
        PutItemResponse result = getClient().putItem(requestBuilder.build());
        return new PutItemOutcome(result);
    }
}
