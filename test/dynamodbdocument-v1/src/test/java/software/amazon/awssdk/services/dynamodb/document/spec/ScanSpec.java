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
import software.amazon.awssdk.services.dynamodb.document.ScanFilter;
import software.amazon.awssdk.services.dynamodb.model.ConditionalOperator;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.Select;

/**
 * API for fully specifying all the parameters of a Table-centric Scan API.
 */
public class ScanSpec extends AbstractCollectionSpec<ScanRequest> {
    private Collection<ScanFilter> scanFilters;
    private Map<String, String> nameMap;
    private Map<String, Object> valueMap;

    private Collection<KeyAttribute> exclusiveStartKey;

    public ScanSpec() {
        super(ScanRequest.builder().build());
    }

    /**
     * @see ScanRequest#scanFilter()
     */
    public Collection<ScanFilter> scanFilters() {
        return scanFilters;
    }

    /**
     * @see ScanRequest#withScanFilter(Map)
     */
    public ScanSpec withScanFilters(ScanFilter... scanFilters) {
        if (scanFilters == null) {
            this.scanFilters = null;
        } else {
            Set<String> names = new LinkedHashSet<String>();
            for (ScanFilter e : scanFilters) {
                names.add(e.getAttribute());
            }
            if (names.size() != scanFilters.length) {
                throw new IllegalArgumentException(
                        "attribute names must not duplicate in the list of scan filters");
            }
            this.scanFilters = Arrays.asList(scanFilters);
        }
        return this;
    }

    /**
     * AND|OR that applies to all the conditions in the ScanFilters.
     *
     * @see ScanRequest#getConditionalOperator()
     */
    public String getConditionalOperator() {
        return getRequest().conditionalOperatorString();
    }

    /**
     * @see ScanRequest#withConditionalOperator(ConditionalOperator)
     */
    public ScanSpec withConditionalOperator(ConditionalOperator op) {
        setRequest(getRequest().toBuilder().conditionalOperator(op).build());
        return this;
    }

    /**
     * @see ScanRequest#getAttributesToGet()
     */
    public List<String> getAttributesToGet() {
        return getRequest().attributesToGet();
    }

    /**
     * @see ScanRequest#withAttributesToGet(String...)
     */
    public ScanSpec withAttributesToGet(String... attributes) {
        if (attributes == null) {
            setRequest(getRequest().toBuilder().attributesToGet((String []) null).build());
        } else {
            setRequest(getRequest().toBuilder().attributesToGet(Arrays.asList(attributes)).build());
        }
        return this;
    }

    /**
     * Any query filters will be ignored if a filter expression has been
     * specified. When a filter expression is specified, the corresponding
     * name-map and value-map can also be specified via
     * {@link #withNameMap(Map)} and {@link #valueMap(Map)}.
     *
     * @see ScanRequest#getFilterExpression()
     */
    public String getFilterExpression() {
        return getRequest().filterExpression();
    }

    /**
     * @see ScanRequest#withFilterExpression(String)
     */
    public ScanSpec withFilterExpression(String filterExpression) {
        setRequest(getRequest().toBuilder().filterExpression(filterExpression).build());
        return this;
    }

    /**
     * @see ScanRequest#getProjectionExpression()
     */
    public String getProjectionExpression() {
        return getRequest().projectionExpression();
    }

    /**
     * @see ScanRequest#withProjectionExpression(String)
     */
    public ScanSpec withProjectionExpression(String projectionExpression) {
        setRequest(getRequest().toBuilder().projectionExpression(projectionExpression).build());
        return this;
    }

    /**
     * @see ScanRequest#getExpressionAttributeNames()
     */
    public Map<String, String> nameMap() {
        return nameMap;
    }

    /**
     * Applicable only when an expression has been specified.
     * Used to specify the actual values for the attribute-name placeholders,
     * where the value in the map can either be string for simple attribute
     * name, or a JSON path expression.
     *
     * @see ScanRequest#withExpressionAttributeNames(Map)
     */
    public ScanSpec withNameMap(Map<String, String> nameMap) {
        if (nameMap == null) {
            this.nameMap = null;
        } else {
            this.nameMap = Collections.unmodifiableMap(new LinkedHashMap<String, String>(nameMap));
        }
        return this;
    }

    /**
     * @see ScanRequest#getExpressionAttributeValues()
     */
    public Map<String, Object> valueMap() {
        return valueMap;
    }

    /**
     * Applicable only when an expression has been specified. Used to
     * specify the actual values for the attribute-value placeholders.
     *
     * @see ScanRequest#withExpressionAttributeValues(Map)
     */
    public ScanSpec valueMap(Map<String, Object> valueMap) {
        if (valueMap == null) {
            this.valueMap = null;
        } else {
            this.valueMap = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(valueMap));
        }
        return this;
    }

    /**
     * @see ScanRequest#getReturnConsumedCapacity()
     */
    public String getReturnConsumedCapacity() {
        return getRequest().returnConsumedCapacityString();
    }

    /**
     * @see ScanRequest#withReturnConsumedCapacity(ReturnConsumedCapacity)
     */
    public ScanSpec withReturnConsumedCapacity(ReturnConsumedCapacity capacity) {
        setRequest(getRequest().toBuilder().returnConsumedCapacity(capacity).build());
        return this;
    }

    /**
     * Specifies the attributes to be returned.
     *
     * @see ScanRequest#select()
     */
    // ALL_ATTRIBUTES | ALL_PROJECTED_ATTRIBUTES | SPECIFIC_ATTRIBUTES | COUNT
    public String select() {
        return getRequest().selectString();
    }

    /**
     * @see ScanRequest#withSelect(Select)
     */
    public ScanSpec withSelect(Select select) {
        setRequest(getRequest().toBuilder().select(select).build());
        return this;
    }

    /**
     * @see ScanRequest#segment()
     */
    public Integer segment() {
        return getRequest().segment();
    }

    /**
     * @see ScanRequest#withSegment(Integer)
     */
    public ScanSpec withSegment(Integer segment) {
        setRequest(getRequest().toBuilder().segment(segment).build());
        return this;
    }

    /**
     * @see ScanRequest#getTotalSegments()
     */
    public Integer getTotalSegments() {
        return getRequest().totalSegments();
    }

    /**
     * @see ScanRequest#withTotalSegments(Integer)
     */
    public ScanSpec withTotalSegments(Integer totalSegments) {
        setRequest(getRequest().toBuilder().totalSegments(totalSegments).build());
        return this;
    }

    /**
     * @see ScanRequest#isConsistentRead()
     */
    public Boolean isConsistentRead() {
        return getRequest().consistentRead();
    }

    /**
     * @see ScanRequest#withConsistentRead(Boolean)
     */
    public ScanSpec withConsistentRead(Boolean consistentRead) {
        setRequest(getRequest().toBuilder().consistentRead(consistentRead).build());
        return this;
    }

    // Exclusive start key

    /**
     * @see ScanRequest#getExclusiveStartKey()
     */
    public Collection<KeyAttribute> getExclusiveStartKey() {
        return exclusiveStartKey;
    }

    /**
     * @see ScanRequest#withExclusiveStartKey(Map)
     */
    public ScanSpec withExclusiveStartKey(KeyAttribute... exclusiveStartKey) {
        if (exclusiveStartKey == null) {
            this.exclusiveStartKey = null;
        } else {
            this.exclusiveStartKey = Arrays.asList(exclusiveStartKey);
        }
        return this;
    }

    /**
     * @see ScanRequest#withExclusiveStartKey(Map)
     */
    public ScanSpec withExclusiveStartKey(PrimaryKey exclusiveStartKey) {
        if (exclusiveStartKey == null) {
            this.exclusiveStartKey = null;
        } else {
            this.exclusiveStartKey = exclusiveStartKey.getComponents();
        }
        return this;
    }

    /**
     * @see ScanRequest#withExclusiveStartKey(Map)
     */
    public ScanSpec withExclusiveStartKey(
            String hashKeyName, Object hashKeyValue) {
        return withExclusiveStartKey(new KeyAttribute(hashKeyName, hashKeyValue));
    }

    /**
     * @see ScanRequest#withExclusiveStartKey(Map)
     */
    public ScanSpec withExclusiveStartKey(
            String hashKeyName, Object hashKeyValue,
            String rangeKeyName, Object rangeKeyValue) {
        return withExclusiveStartKey(
                new KeyAttribute(hashKeyName, hashKeyValue),
                new KeyAttribute(rangeKeyName, rangeKeyValue));
    }

    // Max result size

    @Override
    public ScanSpec withMaxResultSize(Integer maxResultSize) {
        setMaxResultSize(maxResultSize);
        return this;
    }

    @Override
    public ScanSpec withMaxResultSize(int maxResultSize) {
        setMaxResultSize(maxResultSize);
        return this;
    }

    @Override
    public ScanSpec withMaxPageSize(Integer maxPageSize) {
        setMaxPageSize(maxPageSize);
        return this;
    }

    @Override
    public ScanSpec withMaxPageSize(int maxPageSize) {
        setMaxPageSize(maxPageSize);
        return this;
    }
}
