/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.document.ItemCollection;
import software.amazon.awssdk.services.dynamodb.document.KeyAttribute;
import software.amazon.awssdk.services.dynamodb.document.KeyCondition;
import software.amazon.awssdk.services.dynamodb.document.QueryFilter;
import software.amazon.awssdk.services.dynamodb.document.QueryOutcome;
import software.amazon.awssdk.services.dynamodb.document.RangeKeyCondition;
import software.amazon.awssdk.services.dynamodb.document.Table;
import software.amazon.awssdk.services.dynamodb.document.api.QueryApi;
import software.amazon.awssdk.services.dynamodb.document.spec.QuerySpec;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

/**
 * The implementation for <code>QueryApi</code> of a table.
 */
public class QueryImpl extends AbstractImpl implements QueryApi {
    public QueryImpl(DynamoDbClient client, Table table) {
        super(client, table);
    }

    @Override
    public ItemCollection<QueryOutcome> query(String hashKeyName, Object hashKey) {
        return doQuery(new QuerySpec()
                               .withHashKey(new KeyAttribute(hashKeyName, hashKey)));
    }

    @Override
    public ItemCollection<QueryOutcome> query(KeyAttribute hashKey) {
        return doQuery(new QuerySpec().withHashKey(hashKey));
    }

    @Override
    public ItemCollection<QueryOutcome> query(KeyAttribute hashKey,
                                              RangeKeyCondition rangeKeyCondition) {
        return doQuery(new QuerySpec().withHashKey(hashKey)
                                      .withRangeKeyCondition(rangeKeyCondition));
    }

    @Override
    public ItemCollection<QueryOutcome> query(KeyAttribute hashKey,
                                              RangeKeyCondition rangeKeyCondition, QueryFilter... queryFilters) {
        return doQuery(new QuerySpec().withHashKey(hashKey)
                                      .withRangeKeyCondition(rangeKeyCondition)
                                      .withQueryFilters(queryFilters));
    }

    @Override
    public ItemCollection<QueryOutcome> query(KeyAttribute hashKey,
                                              RangeKeyCondition rangeKeyCondition, String filterExpression,
                                              Map<String, String> nameMap, Map<String, Object> valueMap) {
        return doQuery(new QuerySpec().withHashKey(hashKey)
                                      .withRangeKeyCondition(rangeKeyCondition)
                                      .withFilterExpression(filterExpression)
                                      .withNameMap(nameMap)
                                      .valueMap(valueMap));
    }

    @Override
    public ItemCollection<QueryOutcome> query(KeyAttribute hashKey,
                                              RangeKeyCondition rangeKeyCondition, String filterExpression,
                                              String projectionExpression, Map<String, String> nameMap,
                                              Map<String, Object> valueMap) {
        return doQuery(new QuerySpec().withHashKey(hashKey)
                                      .withRangeKeyCondition(rangeKeyCondition)
                                      .withFilterExpression(filterExpression)
                                      .withProjectionExpression(projectionExpression)
                                      .withNameMap(nameMap)
                                      .valueMap(valueMap));
    }

    @Override
    public ItemCollection<QueryOutcome> query(QuerySpec spec) {
        return doQuery(spec);
    }

    protected ItemCollection<QueryOutcome> doQuery(QuerySpec spec) {
        // set the table name
        String tableName = getTable().getTableName();
        QueryRequest.Builder requestBuilder = spec.getRequest().toBuilder().tableName(tableName);

        Map<String, Condition> conditions = new LinkedHashMap<>();

        if (spec.getRequest().keyConditions() != null) {
            conditions.putAll(spec.getRequest().keyConditions());
        }

        // hash key
        final KeyAttribute hashKey = spec.getHashKey();
        if (hashKey != null) {
            conditions.put(hashKey.name(),
                    Condition.builder()
                            .comparisonOperator(ComparisonOperator.EQ)
                            .attributeValueList(InternalUtils.toAttributeValue(hashKey.value())).build());
        }
        // range key condition
        RangeKeyCondition rangeKeyCond = spec.getRangeKeyCondition();
        if (rangeKeyCond != null) {
            KeyCondition keyCond = rangeKeyCond.getKeyCondition();
            if (keyCond == null) {
                throw new IllegalArgumentException("key condition not specified in range key condition");
            }
            Object[] values = rangeKeyCond.values();
            if (values == null) {
                throw new IllegalArgumentException("key condition values not specified in range key condition");
            }
            conditions.put(rangeKeyCond.getAttrName(),
                    Condition.builder()
                            .comparisonOperator(keyCond.toComparisonOperator())
                            .attributeValueList(InternalUtils.toAttributeValues(values)).build());
        }

        requestBuilder.keyConditions(conditions);

        // query filters;
        Collection<QueryFilter> filters = spec.getQueryFilters();
        if (filters != null) {
            requestBuilder.queryFilter(InternalUtils.toAttributeConditionMap(filters));
        }

        // set up the start key, if any
        Collection<KeyAttribute> startKey = spec.getExclusiveStartKey();
        if (startKey != null) {
            requestBuilder.exclusiveStartKey(InternalUtils.toAttributeValueMap(startKey));
        }

        // set up the value map, if any (when expression API is used)
        final Map<String, AttributeValue> attrValMap = InternalUtils.fromSimpleMap(spec.valueMap());
        // set up expressions, if any
        requestBuilder.expressionAttributeNames(spec.nameMap())
               .expressionAttributeValues(attrValMap);

        spec.setRequest(requestBuilder.build());
        return new QueryCollection(getClient(), spec);
    }

    @Override
    public ItemCollection<QueryOutcome> query(String hashKeyName,
                                              Object hashKeyValue, RangeKeyCondition rangeKeyCondition) {
        return query(new KeyAttribute(hashKeyName, hashKeyValue), rangeKeyCondition);
    }

    @Override
    public ItemCollection<QueryOutcome> query(String hashKeyName,
                                              Object hashKeyValue, RangeKeyCondition rangeKeyCondition,
                                              QueryFilter... queryFilters) {
        return query(new KeyAttribute(hashKeyName, hashKeyValue),
                     rangeKeyCondition, queryFilters);
    }

    @Override
    public ItemCollection<QueryOutcome> query(String hashKeyName,
                                              Object hashKeyValue, RangeKeyCondition rangeKeyCondition,
                                              String filterExpression, Map<String, String> nameMap,
                                              Map<String, Object> valueMap) {
        return query(new KeyAttribute(hashKeyName, hashKeyValue),
                     rangeKeyCondition, filterExpression, nameMap, valueMap);
    }

    @Override
    public ItemCollection<QueryOutcome> query(String hashKeyName,
                                              Object hashKeyValue, RangeKeyCondition rangeKeyCondition,
                                              String filterExpression, String projectionExpression,
                                              Map<String, String> nameMap, Map<String, Object> valueMap) {
        return query(new KeyAttribute(hashKeyName, hashKeyValue),
                     rangeKeyCondition, filterExpression, projectionExpression,
                     nameMap, valueMap);
    }
}
