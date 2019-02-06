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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;

/**
 * Container for a page of query results
 */
public class QueryResultPage<T> {

    private List<T> results;
    private Map<String, AttributeValue> lastEvaluatedKey;

    private Integer count;
    private Integer scannedCount;
    private ConsumedCapacity consumedCapacity;

    /**
     * Returns all matching items for this page of query results.
     */
    public List<T> getResults() {
        return results;
    }

    public void setResults(List<T> results) {
        this.results = results;
    }

    /**
     * Returns the last evaluated key, which can be used as the
     * exclusiveStartKey to fetch the next page of results. Returns null if this
     * is the last page of results.
     *
     * @return The key-value pairs which map from the attribute name of each component
     *             of the primary key to its value.
     */
    public Map<String, AttributeValue> lastEvaluatedKey() {
        return lastEvaluatedKey;
    }

    public void setLastEvaluatedKey(Map<String, AttributeValue> lastEvaluatedKey) {
        this.lastEvaluatedKey = lastEvaluatedKey;
    }

    /**
     * The number of items in the response. <p>If you used a
     * <i>QueryFilter</i> in the request, then <i>Count</i> is the number of
     * items returned after the filter was applied, and <i>ScannedCount</i>
     * is the number of matching items before> the filter was applied. <p>If
     * you did not use a filter in the request, then <i>Count</i> and
     * <i>ScannedCount</i> are the same.
     *
     * @return The number of items in the response. <p>If you used a
     *         <i>QueryFilter</i> in the request, then <i>Count</i> is the number of
     *         items returned after the filter was applied, and <i>ScannedCount</i>
     *         is the number of matching items before> the filter was applied. <p>If
     *         you did not use a filter in the request, then <i>Count</i> and
     *         <i>ScannedCount</i> are the same.
     */
    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    /**
     * The number of items evaluated, before any <i>QueryFilter</i> is
     * applied. A high <i>ScannedCount</i> value with few, or no,
     * <i>Count</i> results indicates an inefficient <i>Query</i> operation.
     * For more information, see <a
     * href="http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/QueryAndScan.html#Count">Count
     * and ScannedCount</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     * <p>If you did not use a filter in the request, then
     * <i>ScannedCount</i> is the same as <i>Count</i>.
     *
     * @return The number of items evaluated, before any <i>QueryFilter</i> is
     *         applied. A high <i>ScannedCount</i> value with few, or no,
     *         <i>Count</i> results indicates an inefficient <i>Query</i> operation.
     *         For more information, see <a
     *         href="http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/QueryAndScan.html#Count">Count
     *         and ScannedCount</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     *         <p>If you did not use a filter in the request, then
     *         <i>ScannedCount</i> is the same as <i>Count</i>.
     */
    public Integer scannedCount() {
        return scannedCount;
    }

    public void setScannedCount(Integer scannedCount) {
        this.scannedCount = scannedCount;
    }

    /**
     * The capacity units consumed by an operation. The data returned
     * includes the total provisioned throughput consumed, along with
     * statistics for the table and any indexes involved in the operation.
     * <i>ConsumedCapacity</i> is only returned if the request asked for it.
     * For more information, see <a
     * href="http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ProvisionedThroughputIntro.html">Provisioned
     * Throughput</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     *
     * @return The capacity units consumed by an operation. The data returned
     *         includes the total provisioned throughput consumed, along with
     *         statistics for the table and any indexes involved in the operation.
     *         <i>ConsumedCapacity</i> is only returned if the request asked for it.
     *         For more information, see <a
     *         href="http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ProvisionedThroughputIntro.html">Provisioned
     *         Throughput</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public ConsumedCapacity getConsumedCapacity() {
        return consumedCapacity;
    }

    public void setConsumedCapacity(ConsumedCapacity consumedCapacity) {
        this.consumedCapacity = consumedCapacity;
    }
}
