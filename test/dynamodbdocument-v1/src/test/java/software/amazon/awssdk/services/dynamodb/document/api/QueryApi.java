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

package software.amazon.awssdk.services.dynamodb.document.api;

import java.util.Map;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.services.dynamodb.document.ItemCollection;
import software.amazon.awssdk.services.dynamodb.document.KeyAttribute;
import software.amazon.awssdk.services.dynamodb.document.QueryFilter;
import software.amazon.awssdk.services.dynamodb.document.QueryOutcome;
import software.amazon.awssdk.services.dynamodb.document.RangeKeyCondition;
import software.amazon.awssdk.services.dynamodb.document.spec.QuerySpec;

/**
 * A Table-centric Query API.
 */
@ThreadSafe
public interface QueryApi {
    /**
     * Retrieves items by the specified hash key.
     */
    ItemCollection<QueryOutcome> query(String hashKeyName, Object hashKeyValue);

    /**
     * Retrieves items by the specified hash key.
     */
    ItemCollection<QueryOutcome> query(KeyAttribute hashKey);

    /**
     * Retrieves items by the specified hash key and a range key condition.
     */
    ItemCollection<QueryOutcome> query(KeyAttribute hashKey,
                                       RangeKeyCondition rangeKeyCondition);

    ItemCollection<QueryOutcome> query(String hashKeyName, Object hashKeyValue,
                                       RangeKeyCondition rangeKeyCondition);

    /**
     * Retrieves items by the specified hash key, a range key condition
     * and a list of query filters.
     */
    ItemCollection<QueryOutcome> query(KeyAttribute hashKey,
                                       RangeKeyCondition rangeKeyCondition, QueryFilter... queryFilters);

    ItemCollection<QueryOutcome> query(String hashKeyName, Object hashKeyValue,
                                       RangeKeyCondition rangeKeyCondition, QueryFilter... queryFilters);

    /**
     * Retrieves items by the specified hash key, a range key condition, and
     * a filter expression string.
     *
     * @param filterExpression filter expression
     *     example: "(#a > :a) AND (#c > :c OR #e < :e)"
     *
     * @param nameMap actual values for the attribute-name place holders;
     *                can be null if there is no attribute-name placeholder.
     * @param valueMap actual values for the value place holders
     *                can be null if there is no attribute-value placeholder.
     */
    ItemCollection<QueryOutcome> query(KeyAttribute hashKey,
                                       RangeKeyCondition rangeKeyCondition,
                                       String filterExpression,
                                       Map<String, String> nameMap,
                                       Map<String, Object> valueMap);

    ItemCollection<QueryOutcome> query(String hashKeyName, Object hashKeyValue,
                                       RangeKeyCondition rangeKeyCondition,
                                       String filterExpression,
                                       Map<String, String> nameMap,
                                       Map<String, Object> valueMap);

    /**
     * Retrieves items by the specified hash key, a range key condition,
     * a filter expression and a projection expression.
     *
     * @param filterExpression filter expression
     *     example: "(#a > :a) AND (#c > :c OR #e < :e)"
     *
     * @param projectionExpression projection expression
     *     example: "a.b, c[0].e"
     *
     * @param nameMap actual values for the attribute-name place holders;
     *                can be null if there is no attribute-name placeholder.
     * @param valueMap actual values for the value place holders
     *                can be null if there is no attribute-value placeholder.
     */
    ItemCollection<QueryOutcome> query(KeyAttribute hashKey,
                                       RangeKeyCondition rangeKeyCondition,
                                       String filterExpression,
                                       String projectionExpression,
                                       Map<String, String> nameMap,
                                       Map<String, Object> valueMap);

    /**
     * Retrieves items by the specified hash key, a range key condition,
     * a filter expression and a projection expression.
     *
     * @param filterExpression filter expression
     *     example: "(#a > :a) AND (#c > :c OR #e < :e)"
     *
     * @param projectionExpression projection expression
     *     example: "a.b, c[0].e"
     *
     * @param nameMap actual values for the attribute-name place holders;
     *                can be null if there is no attribute-name placeholder.
     * @param valueMap actual values for the value place holders
     *                can be null if there is no attribute-value placeholder.
     */
    ItemCollection<QueryOutcome> query(String hashKeyName, Object hashKeyValue,
                                       RangeKeyCondition rangeKeyCondition,
                                       String filterExpression,
                                       String projectionExpression,
                                       Map<String, String> nameMap,
                                       Map<String, Object> valueMap);

    /**
     * Queries table by specifying all the details.
     */
    ItemCollection<QueryOutcome> query(QuerySpec spec);
}
