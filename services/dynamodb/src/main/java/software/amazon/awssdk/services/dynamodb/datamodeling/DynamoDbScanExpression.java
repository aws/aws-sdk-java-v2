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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.ConditionalOperator;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.Select;

/**
 * Options for filtering results from a scan operation. For example, callers can
 * specify filter conditions so that only objects whose attributes match
 * different conditions are returned (see {@link ComparisonOperator} for more
 * information on the available comparison types).
 *
 * @see DynamoDbMapper#scan(Class, DynamoDbScanExpression)
 */
public class DynamoDbScanExpression {

    /** Optional filter to limit the results of the scan. */
    private Map<String, Condition> scanFilter;

    /** The exclusive start key for this scan. */
    private Map<String, AttributeValue> exclusiveStartKey;

    /** The limit of items to scan during this scan. */
    private Integer limit;

    /**
     * The total number of segments into which the scan will be divided.
     * Only required for parallel scan operation.
     */
    private Integer totalSegments;

    /**
     * The ID (zero-based) of the segment to be scanned.
     * Only required for parallel scan operation.
     */
    private Integer segment;

    /**
     * The logical operator on the filter conditions of this scan.
     */
    private String conditionalOperator;

    /**
     * Evaluates the scan results and returns only the desired values. <p>The
     * condition you specify is applied to the items scanned; any items that
     * do not match the expression are not returned.
     */
    private String filterExpression;

    /**
     * One or more substitution variables for simplifying complex
     * expressions. The following are some use cases for an
     * ExpressionAttributeName: <ul> <li> <p>Shorten an attribute name that
     * is very long or unwieldy in an expression. </li> <li> <p>Create a
     * placeholder for repeating occurrences of an attribute name in an
     * expression. </li> <li> <p>Prevent special characters in an attribute
     * name from being misinterpreted in an expression. </li> </ul> <p>Use
     * the <b>#</b> character in an expression to dereference an attribute
     * name. For example, consider the following expression:
     * <ul><li><p><code>order.customerInfo.LastName = "Smith" OR
     * order.customerInfo.LastName = "Jones"</code></li></ul> <p>Now suppose
     * that you specified the following for <i>ExpressionAttributeNames</i>:
     * <ul><li><p><code>{"n":"order.customerInfo.LastName"}</code></li></ul>
     * <p>The expression can now be simplified as follows:
     * <ul><li><p><code>#n = "Smith" OR #n = "Jones"</code></li></ul>
     */
    private java.util.Map<String, String> expressionAttributeNames;

    /**
     * One or more values that can be substituted in an expression. <p>Use
     * the <b>:</b> character in an expression to dereference an attribute
     * value. For example, consider the following expression:
     * <ul><li><p><code>ProductStatus IN
     * ("Available","Backordered","Discontinued")</code></li></ul> <p>Now
     * suppose that you specified the following for
     * <i>ExpressionAttributeValues</i>: <ul><li><p><code>{
     * "a":{"S":"Available"}, "b":{"S":"Backordered"},
     * "d":{"S":"Discontinued"} }</code></li></ul> <p>The expression can now
     * be simplified as follows: <ul><li> <p><code>ProductStatus IN
     * (:a,:b,:c)</code></li></ul>
     */
    private java.util.Map<String, AttributeValue> expressionAttributeValues;

    /**
     * The attributes to be returned in the result. You can retrieve all item
     * attributes, specific item attributes, the count of matching items, or
     * in the case of an index, some or all of the attributes projected into
     * the index. <ul> <li> <p><code>ALL_ATTRIBUTES</code> - Returns all of
     * the item attributes from the specified table or index. If you query a
     * local secondary index, then for each matching item in the index
     * DynamoDB will fetch the entire item from the parent table. If the
     * index is configured to project all item attributes, then all of the
     * data can be obtained from the local secondary index, and no fetching
     * is required. </li> <li> <p><code>ALL_PROJECTED_ATTRIBUTES</code> -
     * Allowed only when querying an index. Retrieves all attributes that
     * have been projected into the index. If the index is configured to
     * project all attributes, this return value is equivalent to specifying
     * <code>ALL_ATTRIBUTES</code>. </li> <li> <p><code>COUNT</code> -
     * Returns the number of matching items, rather than the matching items
     * themselves. </li> <li> <p> <code>SPECIFIC_ATTRIBUTES</code> - Returns
     * only the attributes listed in <i>AttributesToGet</i>. This return
     * value is equivalent to specifying <i>AttributesToGet</i> without
     * specifying any value for <i>Select</i>. <p>If you query a local
     * secondary index and request only attributes that are projected into
     * that index, the operation will read only the index and not the table.
     * If any of the requested attributes are not projected into the local
     * secondary index, DynamoDB will fetch each of these attributes from the
     * parent table. This extra fetching incurs additional throughput cost
     * and latency. <p>If you query a global secondary index, you can only
     * request attributes that are projected into the index. Global secondary
     * index queries cannot fetch attributes from the parent table. </li>
     * </ul> <p>If neither <i>Select</i> nor <i>AttributesToGet</i> are
     * specified, DynamoDB defaults to <code>ALL_ATTRIBUTES</code> when
     * accessing a table, and <code>ALL_PROJECTED_ATTRIBUTES</code> when
     * accessing an index. You cannot use both <i>Select</i> and
     * <i>AttributesToGet</i> together in a single request, unless the value
     * for <i>Select</i> is <code>SPECIFIC_ATTRIBUTES</code>. (This usage is
     * equivalent to specifying <i>AttributesToGet</i> without any value for
     * <i>Select</i>.)
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>ALL_ATTRIBUTES, ALL_PROJECTED_ATTRIBUTES, SPECIFIC_ATTRIBUTES, COUNT
     */
    private String select;

    /**
     * A string that identifies one or more attributes to retrieve from the
     * table. These attributes can include scalars, sets, or elements of a
     * JSON document. The attributes in the expression must be separated by
     * commas. <p>If no attribute names are specified, then all attributes
     * will be returned. If any of the requested attributes are not found,
     * they will not appear in the result. <p>For more information, go to <a
     * href="http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Accessing
     * Item Attributes</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    private String projectionExpression;

    /**
     * A value that if set to <code>TOTAL</code>, the response includes
     * <i>ConsumedCapacity</i> data for tables and indexes. If set to
     * <code>INDEXES</code>, the response includes <i>ConsumedCapacity</i>
     * for indexes. If set to <code>NONE</code> (the default),
     * <i>ConsumedCapacity</i> is not included in the response.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>INDEXES, TOTAL, NONE
     * <p>
     * If enabled, the underlying request to DynamoDB will include the
     * configured parameter value and the low-level response from DynamoDB will
     * include the amount of capacity consumed by the scan. Currently, the
     * consumed capacity is only exposed through the DynamoDBMapper when you
     * call {@code DynamoDBMapper.scanPage}, not {@code DynamoDBMapper.scan}.
     */
    private String returnConsumedCapacity;

    /**
     * Optional index name that can be specified for the scan operation.
     */
    private String indexName;

    private Boolean consistentRead;

    /**
     * Returns the name of the index to be used by this scan; or null if there
     * is none.
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Sets the name of the index to be used by this scan.
     */
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /**
     * Sets the name of the index to be used by this scan.
     * <p>
     * Returns a pointer to this object for method-chaining.
     */
    public DynamoDbScanExpression withIndexName(String indexName) {
        setIndexName(indexName);
        return this;
    }

    /**
     * Returns the scan filter as a map of attribute names to conditions.
     *
     * @return The scan filter as a map of attribute names to conditions.
     */
    public Map<String, Condition> scanFilter() {
        return scanFilter;
    }

    /**
     * Sets the scan filter to the map of attribute names to conditions given.
     *
     * @param scanFilter
     *            The map of attribute names to conditions to use when filtering
     *            scan results.
     */
    public void setScanFilter(Map<String, Condition> scanFilter) {
        this.scanFilter = scanFilter;
    }

    /**
     * Sets the scan filter to the map of attribute names to conditions given
     * and returns a pointer to this object for method-chaining.
     *
     * @param scanFilter
     *            The map of attribute names to conditions to use when filtering
     *            scan results.
     */
    public DynamoDbScanExpression withScanFilter(Map<String, Condition> scanFilter) {
        setScanFilter(scanFilter);
        return this;
    }

    /**
     * Adds a new filter condition to the current scan filter.
     *
     * @param attributeName
     *            The name of the attribute on which the specified condition
     *            operates.
     * @param condition
     *            The condition which describes how the specified attribute is
     *            compared and if a row of data is included in the results
     *            returned by the scan operation.
     */
    public void addFilterCondition(String attributeName, Condition condition) {
        if (scanFilter == null) {
            scanFilter = new HashMap<String, Condition>();
        }

        scanFilter.put(attributeName, condition);
    }

    /**
     * Adds a new filter condition to the current scan filter and returns a
     * pointer to this object for method-chaining.
     *
     * @param attributeName
     *            The name of the attribute on which the specified condition
     *            operates.
     * @param condition
     *            The condition which describes how the specified attribute is
     *            compared and if a row of data is included in the results
     *            returned by the scan operation.
     */
    public DynamoDbScanExpression withFilterConditionEntry(String attributeName, Condition condition) {
        if (scanFilter == null) {
            scanFilter = new HashMap<String, Condition>();
        }

        scanFilter.put(attributeName, condition);
        return this;
    }


    /**
     * Returns the exclusive start key for this scan.
     */
    public Map<String, AttributeValue> getExclusiveStartKey() {
        return exclusiveStartKey;
    }

    /**
     * Sets the exclusive start key for this scan.
     */
    public void setExclusiveStartKey(Map<String, AttributeValue> exclusiveStartKey) {
        this.exclusiveStartKey = exclusiveStartKey;
    }

    /**
     * Sets the exclusive start key for this scan and returns a pointer to this
     * object for method-chaining.
     */
    public DynamoDbScanExpression withExclusiveStartKey(Map<String, AttributeValue> exclusiveStartKey) {
        this.exclusiveStartKey = exclusiveStartKey;
        return this;
    }

    /**
     * Returns the limit of items to scan during this scan.
     * <p>
     * Use with caution. Please note that this is <b>not</b> the same as the
     * number of items to return from the scan operation -- the operation will
     * cease and return as soon as this many items are scanned, even if no
     * matching results are found. Furthermore, {@link PaginatedScanList} will
     * execute as many scan operations as necessary until it either reaches the
     * end of the result set as indicated by DynamoDB or enough elements are
     * available to fulfill the list operation (e.g. iteration). Therefore,
     * except when scanning without a scan filter, it's usually bad practice to
     * set a low limit, since doing so will often generate the same amount of
     * traffic to DynamoDB but with a greater number of round trips and
     * therefore more overall latency.
     */
    public Integer limit() {
        return limit;
    }

    /**
     * Sets the limit of items to scan during this scan. Please note that this
     * is <b>not</b> the same as the number of items to return from the scan
     * operation -- the operation will cease and return as soon as this many
     * items are scanned, even if no matching results are found.
     *
     * @see DynamoDbScanExpression#limit()
     */
    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    /**
     * Sets the limit of items to scan and returns a pointer to this object for
     * method-chaining. Please note that this is <b>not</b> the same as the
     * number of items to return from the scan operation -- the operation will
     * cease and return as soon as this many items are scanned, even if no
     * matching results are found.
     *
     * @see DynamoDbScanExpression#limit()
     */
    public DynamoDbScanExpression withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Returns the total number of segments into which the scan will be divided.
     */
    public Integer getTotalSegments() {
        return totalSegments;
    }

    /**
     * Sets the total number of segments into which the scan will be divided.
     */
    public void setTotalSegments(Integer totalSegments) {
        this.totalSegments = totalSegments;
    }

    /**
     * Sets the total number of segments into which the scan will be divided and
     * returns a pointer to this object for method-chaining.
     */
    public DynamoDbScanExpression withTotalSegments(Integer totalSegments) {
        setTotalSegments(totalSegments);
        return this;
    }

    /**
     * Returns the ID of the segment to be scanned.
     */
    public Integer segment() {
        return segment;
    }

    /**
     * Sets the ID of the segment to be scanned.
     */
    public void setSegment(Integer segment) {
        this.segment = segment;
    }

    /**
     * Sets the ID of the segment to be scanned and returns a pointer to this
     * object for method-chaining.
     */
    public DynamoDbScanExpression withSegment(Integer segment) {
        setSegment(segment);
        return this;
    }

    /**
     * Returns the logical operator on the filter conditions of this scan.
     */
    public String getConditionalOperator() {
        return conditionalOperator;
    }

    /**
     * Sets the logical operator on the filter conditions of this scan.
     */
    public void setConditionalOperator(String conditionalOperator) {
        this.conditionalOperator = conditionalOperator;
    }

    /**
     * Sets the logical operator on the filter conditions of this scan.
     */
    public void setConditionalOperator(ConditionalOperator conditionalOperator) {
        setConditionalOperator(conditionalOperator.toString());
    }

    /**
     * Sets the logical operator on the filter conditions of this scan and
     * returns a pointer to this object for method-chaining.
     */
    public DynamoDbScanExpression withConditionalOperator(String conditionalOperator) {
        setConditionalOperator(conditionalOperator);
        return this;
    }

    /**
     * Sets the logical operator on the filter conditions of this scan and
     * returns a pointer to this object for method-chaining.
     */
    public DynamoDbScanExpression withConditionalOperator(ConditionalOperator conditionalOperator) {
        return withConditionalOperator(conditionalOperator.toString());
    }

    /**
     * Evaluates the query results and returns only the desired values.
     * <p>
     * The condition you specify is applied to the items queried; any items that
     * do not match the expression are not returned.
     *
     * @return Evaluates the query results and returns only the desired values.
     *         <p>
     *         The condition you specify is applied to the items queried; any
     *         items that do not match the expression are not returned.
     * @see ScanRequest#getFilterExpression()
     */
    public String getFilterExpression() {
        return filterExpression;
    }

    /**
     * Evaluates the query results and returns only the desired values.
     * <p>
     * The condition you specify is applied to the items queried; any items that
     * do not match the expression are not returned.
     *
     * @param filterExpression
     *            Evaluates the query results and returns only the desired
     *            values.
     *            <p>
     *            The condition you specify is applied to the items queried; any
     *            items that do not match the expression are not returned.
     * @see ScanRequest#setFilterExpression(String)
     */
    public void setFilterExpression(String filterExpression) {
        this.filterExpression = filterExpression;
    }

    /**
     * Evaluates the query results and returns only the desired values.
     * <p>
     * The condition you specify is applied to the items queried; any items that
     * do not match the expression are not returned.
     * <p>
     * Returns a reference to this object so that method calls can be chained
     * together.
     *
     * @param filterExpression
     *            Evaluates the query results and returns only the desired
     *            values.
     *            <p>
     *            The condition you specify is applied to the items queried; any
     *            items that do not match the expression are not returned.
     *
     * @return A reference to this updated object so that method calls can be
     *         chained together.
     * @see ScanRequest#withFilterExpression(String)
     */
    public DynamoDbScanExpression withFilterExpression(String filterExpression) {
        this.filterExpression = filterExpression;
        return this;
    }

    /**
     * One or more substitution variables for simplifying complex expressions.
     *
     * @return One or more substitution variables for simplifying complex
     *         expressions.
     * @see scanRequest#getExpressionAttributeNames()
     */
    public java.util.Map<String, String> getExpressionAttributeNames() {

        return expressionAttributeNames;
    }

    /**
     * One or more substitution variables for simplifying complex expressions.
     *
     * @param expressionAttributeNames
     *            One or more substitution variables for simplifying complex
     *            expressions.
     * @see ScanRequest#setExpressionAttributeNames(Map)
     */
    public void setExpressionAttributeNames(
            java.util.Map<String, String> expressionAttributeNames) {
        this.expressionAttributeNames = expressionAttributeNames;
    }

    /**
     * One or more substitution variables for simplifying complex expressions.
     *
     * @param expressionAttributeNames
     *            One or more substitution variables for simplifying complex
     *            expressions.
     *
     * @return A reference to this updated object so that method calls can be
     *         chained together.
     * @see ScanRequest#withExpressionAttributeNames(Map)
     */
    public DynamoDbScanExpression withExpressionAttributeNames(
            java.util.Map<String, String> expressionAttributeNames) {
        setExpressionAttributeNames(expressionAttributeNames);
        return this;
    }

    /**
     * One or more substitution variables for simplifying complex expressions.
     * The method adds a new key-value pair into ExpressionAttributeNames
     * parameter, and returns a reference to this object so that method calls
     * can be chained together.
     *
     * @param key
     *            The key of the entry to be added into
     *            ExpressionAttributeNames.
     * @param value
     *            The corresponding value of the entry to be added into
     *            ExpressionAttributeNames.
     *
     * @see ScanRequest#addExpressionAttributeNamesEntry(String, String)
     */
    public DynamoDbScanExpression addExpressionAttributeNamesEntry(String key,
                                                                   String value) {
        if (null == this.expressionAttributeNames) {
            this.expressionAttributeNames = new java.util.HashMap<String, String>();
        }
        if (this.expressionAttributeNames.containsKey(key)) {
            throw new IllegalArgumentException("Duplicated keys (" + key + ") are provided.");
        }
        this.expressionAttributeNames.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into ExpressionAttributeNames.
     * <p>
     * Returns a reference to this object so that method calls can be chained
     * together.
     */
    public DynamoDbScanExpression clearExpressionAttributeNamesEntries() {
        this.expressionAttributeNames = null;
        return this;
    }

    /**
     * One or more values that can be substituted in an expression.
     *
     * @return One or more values that can be substituted in an expression.
     *
     * @see ScanRequest#getExpressionAttributeValues()
     */
    public java.util.Map<String, AttributeValue> getExpressionAttributeValues() {

        return expressionAttributeValues;
    }

    /**
     * One or more values that can be substituted in an expression.
     *
     * @param expressionAttributeValues
     *            One or more values that can be substituted in an expression.
     *
     * @see ScanRequest#setExpressionAttributeValues(Map)
     */
    public void setExpressionAttributeValues(
            java.util.Map<String, AttributeValue> expressionAttributeValues) {
        this.expressionAttributeValues = expressionAttributeValues;
    }

    /**
     * One or more values that can be substituted in an expression.
     *
     * @param expressionAttributeValues
     *            One or more values that can be substituted in an expression.
     *
     * @return A reference to this updated object so that method calls can be
     *         chained together.
     * @see ScanRequest#withExpressionAttributeValues(Map)
     */
    public DynamoDbScanExpression withExpressionAttributeValues(
            java.util.Map<String, AttributeValue> expressionAttributeValues) {
        setExpressionAttributeValues(expressionAttributeValues);
        return this;
    }

    /**
     * One or more values that can be substituted in an expression. The method
     * adds a new key-value pair into ExpressionAttributeValues parameter, and
     * returns a reference to this object so that method calls can be chained
     * together.
     *
     * @param key
     *            The key of the entry to be added into
     *            ExpressionAttributeValues.
     * @param value
     *            The corresponding value of the entry to be added into
     *            ExpressionAttributeValues.
     *
     * @see ScanRequest#addExpressionAttributeValuesEntry(String,
     *      AttributeValue)
     */
    public DynamoDbScanExpression addExpressionAttributeValuesEntry(String key,
                                                                    AttributeValue value) {
        if (null == this.expressionAttributeValues) {
            this.expressionAttributeValues = new java.util.HashMap<String, AttributeValue>();
        }
        if (this.expressionAttributeValues.containsKey(key)) {
            throw new IllegalArgumentException("Duplicated keys (" + key + ") are provided.");
        }
        this.expressionAttributeValues.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into ExpressionAttributeValues.
     * <p>
     * Returns a reference to this object so that method calls can be chained
     * together.
     */
    public DynamoDbScanExpression clearExpressionAttributeValuesEntries() {
        this.expressionAttributeValues = null;
        return this;
    }

    /**
     * The attributes to be returned in the result. You can retrieve all item
     * attributes, specific item attributes, the count of matching items, or
     * in the case of an index, some or all of the attributes projected into
     * the index.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>ALL_ATTRIBUTES, ALL_PROJECTED_ATTRIBUTES, SPECIFIC_ATTRIBUTES, COUNT
     *
     * @return The attributes to be returned in the result. You can retrieve all item
     *         attributes, specific item attributes, the count of matching items, or
     *         in the case of an index, some or all of the attributes projected into
     *         the index.
     *
     * @see software.amazon.awssdk.services.dynamodb.model.Select
     */
    public String select() {
        return select;
    }

    /**
     * The attributes to be returned in the result. You can retrieve all item
     * attributes, specific item attributes, the count of matching items, or
     * in the case of an index, some or all of the attributes projected into
     * the index.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>ALL_ATTRIBUTES, ALL_PROJECTED_ATTRIBUTES, SPECIFIC_ATTRIBUTES, COUNT
     *
     * @param select The attributes to be returned in the result. You can retrieve all item
     *         attributes, specific item attributes, the count of matching items, or
     *         in the case of an index, some or all of the attributes projected into
     *         the index.
     *
     * @see software.amazon.awssdk.services.dynamodb.model.Select
     */
    public void setSelect(String select) {
        this.select = select;
    }

    /**
     * The attributes to be returned in the result. You can retrieve all item
     * attributes, specific item attributes, the count of matching items, or
     * in the case of an index, some or all of the attributes projected into
     * the index.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>ALL_ATTRIBUTES, ALL_PROJECTED_ATTRIBUTES, SPECIFIC_ATTRIBUTES, COUNT
     *
     * @param select The attributes to be returned in the result. You can retrieve all item
     *         attributes, specific item attributes, the count of matching items, or
     *         in the case of an index, some or all of the attributes projected into
     *         the index.
     *
     * @see software.amazon.awssdk.services.dynamodb.model.Select
     */
    public void setSelect(Select select) {
        this.select = select.toString();
    }

    /**
     * The attributes to be returned in the result. You can retrieve all item
     * attributes, specific item attributes, the count of matching items, or
     * in the case of an index, some or all of the attributes projected into
     * the index.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>ALL_ATTRIBUTES, ALL_PROJECTED_ATTRIBUTES, SPECIFIC_ATTRIBUTES, COUNT
     *
     * @param select The attributes to be returned in the result. You can retrieve all item
     *         attributes, specific item attributes, the count of matching items, or
     *         in the case of an index, some or all of the attributes projected into
     *         the index.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     *
     * @see software.amazon.awssdk.services.dynamodb.model.Select
     */
    public DynamoDbScanExpression withSelect(String select) {
        this.select = select;
        return this;
    }

    /**
     * The attributes to be returned in the result. You can retrieve all item
     * attributes, specific item attributes, the count of matching items, or
     * in the case of an index, some or all of the attributes projected into
     * the index.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>ALL_ATTRIBUTES, ALL_PROJECTED_ATTRIBUTES, SPECIFIC_ATTRIBUTES, COUNT
     *
     * @param select The attributes to be returned in the result. You can retrieve all item
     *         attributes, specific item attributes, the count of matching items, or
     *         in the case of an index, some or all of the attributes projected into
     *         the index.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     *
     * @see software.amazon.awssdk.services.dynamodb.model.Select
     */
    public DynamoDbScanExpression withSelect(Select select) {
        this.select = select.toString();
        return this;
    }

    /**
     * A string that identifies one or more attributes to retrieve from the
     * table. These attributes can include scalars, sets, or elements of a
     * JSON document. The attributes in the expression must be separated by
     * commas. <p>If no attribute names are specified, then all attributes
     * will be returned. If any of the requested attributes are not found,
     * they will not appear in the result. <p>For more information, go to <a
     * href="http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Accessing
     * Item Attributes</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     *
     * @return A string that identifies one or more attributes to retrieve from the
     *         table. These attributes can include scalars, sets, or elements of a
     *         JSON document. The attributes in the expression must be separated by
     *         commas. <p>If no attribute names are specified, then all attributes
     *         will be returned. If any of the requested attributes are not found,
     *         they will not appear in the result. <p>For more information, go to <a
     *         href="http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Accessing
     *         Item Attributes</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public String getProjectionExpression() {
        return projectionExpression;
    }

    /**
     * A string that identifies one or more attributes to retrieve from the
     * table. These attributes can include scalars, sets, or elements of a
     * JSON document. The attributes in the expression must be separated by
     * commas. <p>If no attribute names are specified, then all attributes
     * will be returned. If any of the requested attributes are not found,
     * they will not appear in the result. <p>For more information, go to <a
     * href="http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Accessing
     * Item Attributes</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     *
     * @param projectionExpression A string that identifies one or more attributes to retrieve from the
     *         table. These attributes can include scalars, sets, or elements of a
     *         JSON document. The attributes in the expression must be separated by
     *         commas. <p>If no attribute names are specified, then all attributes
     *         will be returned. If any of the requested attributes are not found,
     *         they will not appear in the result. <p>For more information, go to <a
     *         href="http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Accessing
     *         Item Attributes</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public void setProjectionExpression(String projectionExpression) {
        this.projectionExpression = projectionExpression;
    }

    /**
     * A string that identifies one or more attributes to retrieve from the
     * table. These attributes can include scalars, sets, or elements of a
     * JSON document. The attributes in the expression must be separated by
     * commas. <p>If no attribute names are specified, then all attributes
     * will be returned. If any of the requested attributes are not found,
     * they will not appear in the result. <p>For more information, go to <a
     * href="http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Accessing
     * Item Attributes</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param projectionExpression A string that identifies one or more attributes to retrieve from the
     *         table. These attributes can include scalars, sets, or elements of a
     *         JSON document. The attributes in the expression must be separated by
     *         commas. <p>If no attribute names are specified, then all attributes
     *         will be returned. If any of the requested attributes are not found,
     *         they will not appear in the result. <p>For more information, go to <a
     *         href="http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Accessing
     *         Item Attributes</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public DynamoDbScanExpression withProjectionExpression(String projectionExpression) {
        this.projectionExpression = projectionExpression;
        return this;
    }

    /**
     * A value that if set to <code>TOTAL</code>, the response includes
     * <i>ConsumedCapacity</i> data for tables and indexes. If set to
     * <code>INDEXES</code>, the response includes <i>ConsumedCapacity</i>
     * for indexes. If set to <code>NONE</code> (the default),
     * <i>ConsumedCapacity</i> is not included in the response.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>INDEXES, TOTAL, NONE
     * <p>
     * If enabled, the underlying request to DynamoDB will include the
     * configured parameter value and the low-level response from DynamoDB will
     * include the amount of capacity consumed by the scan. Currently, the
     * consumed capacity is only exposed through the DynamoDBMapper when you
     * call {@code DynamoDBMapper.scanPage}, not {@code DynamoDBMapper.scan}.
     *
     * @return A value that if set to <code>TOTAL</code>, the response includes
     *         <i>ConsumedCapacity</i> data for tables and indexes. If set to
     *         <code>INDEXES</code>, the response includes <i>ConsumedCapacity</i>
     *         for indexes. If set to <code>NONE</code> (the default),
     *         <i>ConsumedCapacity</i> is not included in the response.
     *
     * @see software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity
     */
    public String getReturnConsumedCapacity() {
        return returnConsumedCapacity;
    }

    /**
     * A value that if set to <code>TOTAL</code>, the response includes
     * <i>ConsumedCapacity</i> data for tables and indexes. If set to
     * <code>INDEXES</code>, the response includes <i>ConsumedCapacity</i>
     * for indexes. If set to <code>NONE</code> (the default),
     * <i>ConsumedCapacity</i> is not included in the response.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>INDEXES, TOTAL, NONE
     * <p>
     * If enabled, the underlying request to DynamoDB will include the
     * configured parameter value and the low-level response from DynamoDB will
     * include the amount of capacity consumed by the scan. Currently, the
     * consumed capacity is only exposed through the DynamoDBMapper when you
     * call {@code DynamoDBMapper.scanPage}, not {@code DynamoDBMapper.scan}.
     *
     * @param returnConsumedCapacity A value that if set to <code>TOTAL</code>, the response includes
     *         <i>ConsumedCapacity</i> data for tables and indexes. If set to
     *         <code>INDEXES</code>, the response includes <i>ConsumedCapacity</i>
     *         for indexes. If set to <code>NONE</code> (the default),
     *         <i>ConsumedCapacity</i> is not included in the response.
     *
     * @see software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity
     */
    public void setReturnConsumedCapacity(String returnConsumedCapacity) {
        this.returnConsumedCapacity = returnConsumedCapacity;
    }

    /**
     * A value that if set to <code>TOTAL</code>, the response includes
     * <i>ConsumedCapacity</i> data for tables and indexes. If set to
     * <code>INDEXES</code>, the response includes <i>ConsumedCapacity</i>
     * for indexes. If set to <code>NONE</code> (the default),
     * <i>ConsumedCapacity</i> is not included in the response.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>INDEXES, TOTAL, NONE
     * <p>
     * If enabled, the underlying request to DynamoDB will include the
     * configured parameter value and the low-level response from DynamoDB will
     * include the amount of capacity consumed by the scan. Currently, the
     * consumed capacity is only exposed through the DynamoDBMapper when you
     * call {@code DynamoDBMapper.scanPage}, not {@code DynamoDBMapper.scan}.
     *
     * @param returnConsumedCapacity A value that if set to <code>TOTAL</code>, the response includes
     *         <i>ConsumedCapacity</i> data for tables and indexes. If set to
     *         <code>INDEXES</code>, the response includes <i>ConsumedCapacity</i>
     *         for indexes. If set to <code>NONE</code> (the default),
     *         <i>ConsumedCapacity</i> is not included in the response.
     *
     * @see software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity
     */
    public void setReturnConsumedCapacity(ReturnConsumedCapacity returnConsumedCapacity) {
        this.returnConsumedCapacity = returnConsumedCapacity.toString();
    }

    /**
     * A value that if set to <code>TOTAL</code>, the response includes
     * <i>ConsumedCapacity</i> data for tables and indexes. If set to
     * <code>INDEXES</code>, the response includes <i>ConsumedCapacity</i>
     * for indexes. If set to <code>NONE</code> (the default),
     * <i>ConsumedCapacity</i> is not included in the response.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>INDEXES, TOTAL, NONE
     * <p>
     * If enabled, the underlying request to DynamoDB will include the
     * configured parameter value and the low-level response from DynamoDB will
     * include the amount of capacity consumed by the scan. Currently, the
     * consumed capacity is only exposed through the DynamoDBMapper when you
     * call {@code DynamoDBMapper.scanPage}, not {@code DynamoDBMapper.scan}.
     *
     * @param returnConsumedCapacity A value that if set to <code>TOTAL</code>, the response includes
     *         <i>ConsumedCapacity</i> data for tables and indexes. If set to
     *         <code>INDEXES</code>, the response includes <i>ConsumedCapacity</i>
     *         for indexes. If set to <code>NONE</code> (the default),
     *         <i>ConsumedCapacity</i> is not included in the response.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     *
     * @see software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity
     */
    public DynamoDbScanExpression withReturnConsumedCapacity(String returnConsumedCapacity) {
        this.returnConsumedCapacity = returnConsumedCapacity;
        return this;
    }

    /**
     * A value that if set to <code>TOTAL</code>, the response includes
     * <i>ConsumedCapacity</i> data for tables and indexes. If set to
     * <code>INDEXES</code>, the response includes <i>ConsumedCapacity</i>
     * for indexes. If set to <code>NONE</code> (the default),
     * <i>ConsumedCapacity</i> is not included in the response.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>INDEXES, TOTAL, NONE
     * <p>
     * If enabled, the underlying request to DynamoDB will include the
     * configured parameter value and the low-level response from DynamoDB will
     * include the amount of capacity consumed by the scan. Currently, the
     * consumed capacity is only exposed through the DynamoDBMapper when you
     * call {@code DynamoDBMapper.scanPage}, not {@code DynamoDBMapper.scan}.
     *
     * @param returnConsumedCapacity A value that if set to <code>TOTAL</code>, the response includes
     *         <i>ConsumedCapacity</i> data for tables and indexes. If set to
     *         <code>INDEXES</code>, the response includes <i>ConsumedCapacity</i>
     *         for indexes. If set to <code>NONE</code> (the default),
     *         <i>ConsumedCapacity</i> is not included in the response.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     *
     * @see software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity
     */
    public DynamoDbScanExpression withReturnConsumedCapacity(ReturnConsumedCapacity returnConsumedCapacity) {
        this.returnConsumedCapacity = returnConsumedCapacity.toString();
        return this;
    }

    /**
     * Returns whether this scan uses consistent reads.
     *
     * @see ScanRequest#isConsistentRead()
     */
    public Boolean isConsistentRead() {
        return consistentRead;
    }

    /**
     * Sets whether this scan uses consistent reads.
     */
    public void setConsistentRead(Boolean consistentRead) {
        this.consistentRead = consistentRead;
    }

    /**
     * Sets whether this scan uses consistent reads and returns a reference
     * to this object for method chaining.
     */
    public DynamoDbScanExpression withConsistentRead(Boolean consistentRead) {
        this.consistentRead = consistentRead;
        return this;
    }
}
