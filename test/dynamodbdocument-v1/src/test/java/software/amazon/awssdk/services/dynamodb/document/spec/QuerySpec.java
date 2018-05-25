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

package software.amazon.awssdk.services.dynamodb.document.spec;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.services.dynamodb.document.KeyAttribute;
import software.amazon.awssdk.services.dynamodb.document.PrimaryKey;
import software.amazon.awssdk.services.dynamodb.document.QueryFilter;
import software.amazon.awssdk.services.dynamodb.document.RangeKeyCondition;
import software.amazon.awssdk.services.dynamodb.model.ConditionalOperator;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.Select;

/**
 * Full parameter specification for the Query API.
 */
public class QuerySpec extends AbstractCollectionSpec<QueryRequest> {
    private KeyAttribute hashKey;
    private RangeKeyCondition rangeKeyCondition;
    private Collection<QueryFilter> queryFilters;
    private Map<String, String> nameMap;
    private Map<String, Object> valueMap;

    private Collection<KeyAttribute> exclusiveStartKey;

    public QuerySpec() {
        super(QueryRequest.builder().build());
    }

    public KeyAttribute getHashKey() {
        return hashKey;
    }

    public QuerySpec withHashKey(KeyAttribute hashKey) {
        this.hashKey = hashKey;
        return this;
    }

    public QuerySpec withHashKey(String hashKeyName, Object hashKeyValue) {
        this.hashKey = new KeyAttribute(hashKeyName, hashKeyValue);
        return this;
    }

    public RangeKeyCondition getRangeKeyCondition() {
        return rangeKeyCondition;
    }

    public QuerySpec withRangeKeyCondition(RangeKeyCondition rangeKeyCondition) {
        this.rangeKeyCondition = rangeKeyCondition;
        return this;
    }

    /**
     * When a key condition expression is specified, the corresponding name-map
     * and value-map can optionally be specified via {@link #withNameMap(Map)}
     * and {@link #valueMap(Map)}. (Note the hash key and range key
     * conditions must not be specified if a key condition expression has been
     * specified.)
     */
    public QuerySpec withKeyConditionExpression(String keyConditionExpression) {
        setRequest(getRequest().toBuilder().keyConditionExpression(keyConditionExpression).build());
        return this;
    }

    public String getKeyConditionExpression() {
        return getRequest().keyConditionExpression();
    }

    public QuerySpec withAttributesToGet(String... attributes) {
        setRequest(getRequest().toBuilder().attributesToGet(Arrays.asList(attributes)).build());
        return this;
    }

    public List<String> getAttributesToGet() {
        return getRequest().attributesToGet();
    }

    public QuerySpec withConditionalOperator(ConditionalOperator op) {
        setRequest(getRequest().toBuilder().conditionalOperator(op).build());
        return this;
    }

    public String getConditionalOperator() {
        return getRequest().conditionalOperatorAsString();
    }

    public QuerySpec withConsistentRead(boolean consistentRead) {
        setRequest(getRequest().toBuilder().consistentRead(consistentRead).build());
        return this;
    }

    public boolean isConsistentRead() {
        return getRequest().consistentRead();
    }

    public QuerySpec withQueryFilters(QueryFilter... queryFilters) {
        if (queryFilters == null) {
            this.queryFilters = null;
        } else {
            Set<String> names = new LinkedHashSet<String>();
            for (QueryFilter e : queryFilters) {
                names.add(e.getAttribute());
            }
            if (names.size() != queryFilters.length) {
                throw new IllegalArgumentException(
                        "attribute names must not duplicate in the list of query filters");
            }
            this.queryFilters = Arrays.asList(queryFilters);
        }
        return this;
    }

    public Collection<QueryFilter> getQueryFilters() {
        return queryFilters;
    }

    /**
     * When a filter expression is specified, the corresponding name-map and
     * value-map can optionally be specified via {@link #withNameMap(Map)} and
     * {@link #valueMap(Map)}. (Note query filters must not be specified if
     * a filter expression has been specified.)
     */
    public QuerySpec withFilterExpression(String filterExpression) {
        setRequest(getRequest().toBuilder().filterExpression(filterExpression).build());
        return this;
    }

    public String getFilterExpression() {
        return getRequest().filterExpression();
    }

    /**
     * When a projection expression is specified, the corresponding name-map and
     * value-map can optionally be specified via {@link #withNameMap(Map)} and
     * {@link #valueMap(Map)}. (Note attributes-to-get must not be specified
     * if a projection expression has been specified.)
     */
    public QuerySpec withProjectionExpression(String projectionExpression) {
        setRequest(getRequest().toBuilder().projectionExpression(projectionExpression).build());
        return this;
    }

    public String getProjectionExpression() {
        return getRequest().projectionExpression();
    }

    public Map<String, String> nameMap() {
        return nameMap;
    }

    /**
     * Applicable only when an expression has been specified.
     * Used to specify the actual values for the attribute-name placeholders,
     * where the value in the map can either be string for simple attribute
     * name, or a JSON path expression.
     */
    public QuerySpec withNameMap(Map<String, String> nameMap) {
        if (nameMap == null) {
            this.nameMap = null;
        } else {
            this.nameMap = Collections.unmodifiableMap(new LinkedHashMap<String, String>(nameMap));
        }
        return this;
    }

    public Map<String, Object> valueMap() {
        return valueMap;
    }

    /**
     * Applicable only when an expression has been specified. Used to
     * specify the actual values for the attribute-value placeholders.
     */
    public QuerySpec valueMap(Map<String, Object> valueMap) {
        if (valueMap == null) {
            this.valueMap = null;
        } else {
            this.valueMap = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(valueMap));
        }
        return this;
    }

    public String getReturnConsumedCapacity() {
        return getRequest().returnConsumedCapacityAsString();
    }

    public QuerySpec withReturnConsumedCapacity(
            ReturnConsumedCapacity returnConsumedCapacity) {
        setRequest(getRequest().toBuilder().returnConsumedCapacity(returnConsumedCapacity).build());
        return this;
    }

    public QuerySpec withScanIndexForward(boolean scanIndexForward) {
        setRequest(getRequest().toBuilder().scanIndexForward(scanIndexForward).build());
        return this;
    }

    public boolean isScanIndexForward() {
        return getRequest().scanIndexForward();
    }

    public QuerySpec withSelect(Select select) {
        setRequest(getRequest().toBuilder().select(select).build());
        return this;
    }

    public String select() {
        return getRequest().selectAsString();
    }

    // Exclusive start key

    public Collection<KeyAttribute> getExclusiveStartKey() {
        return exclusiveStartKey;
    }

    public QuerySpec withExclusiveStartKey(KeyAttribute... exclusiveStartKey) {
        if (exclusiveStartKey == null) {
            this.exclusiveStartKey = null;
        } else {
            this.exclusiveStartKey = Arrays.asList(exclusiveStartKey);
        }
        return this;
    }

    public QuerySpec withExclusiveStartKey(PrimaryKey exclusiveStartKey) {
        if (exclusiveStartKey == null) {
            this.exclusiveStartKey = null;
        } else {
            this.exclusiveStartKey = exclusiveStartKey.getComponents();
        }
        return this;
    }

    public QuerySpec withExclusiveStartKey(
            String hashKeyName, Object hashKeyValue) {
        return withExclusiveStartKey(new KeyAttribute(hashKeyName, hashKeyValue));
    }

    public QuerySpec withExclusiveStartKey(
            String hashKeyName, Object hashKeyValue,
            String rangeKeyName, Object rangeKeyValue) {
        return withExclusiveStartKey(
                new KeyAttribute(hashKeyName, hashKeyValue),
                new KeyAttribute(rangeKeyName, rangeKeyValue));
    }

    // Max result size

    @Override
    public QuerySpec withMaxResultSize(Integer maxResultSize) {
        setMaxResultSize(maxResultSize);
        return this;
    }

    @Override
    public QuerySpec withMaxResultSize(int maxResultSize) {
        setMaxResultSize(maxResultSize);
        return this;
    }

    @Override
    public QuerySpec withMaxPageSize(Integer maxPageSize) {
        setMaxPageSize(maxPageSize);
        return this;
    }

    @Override
    public QuerySpec withMaxPageSize(int maxPageSize) {
        setMaxPageSize(maxPageSize);
        return this;
    }
}
