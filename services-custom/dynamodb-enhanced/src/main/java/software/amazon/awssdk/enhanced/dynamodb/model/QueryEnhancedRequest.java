/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.model;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Defines parameters used to when querying a DynamoDb table or index using the query() operation (such as
 * {@link DynamoDbTable#query(QueryEnhancedRequest)} or {@link DynamoDbAsyncIndex#query(QueryEnhancedRequest)}).
 * <p>
 * A valid request object must contain a {@link QueryConditional} condition specifying how DynamoDb
 * should match items in the table.
 * <p>
 * All other parameters are optional.
 */
@SdkPublicApi
public final class QueryEnhancedRequest {

    private final QueryConditional queryConditional;
    private final Map<String, AttributeValue> exclusiveStartKey;
    private final Boolean scanIndexForward;
    private final Integer limit;
    private final Boolean consistentRead;
    private final Expression filterExpression;

    private QueryEnhancedRequest(Builder builder) {
        this.queryConditional = builder.queryConditional;
        this.exclusiveStartKey = builder.exclusiveStartKey;
        this.scanIndexForward = builder.scanIndexForward;
        this.limit = builder.limit;
        this.consistentRead = builder.consistentRead;
        this.filterExpression = builder.filterExpression;
    }

    /**
     * Creates a newly initialized builder for a request object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a builder initialized with all existing values on the request object.
     */
    public Builder toBuilder() {
        return builder().queryConditional(queryConditional)
                        .exclusiveStartKey(exclusiveStartKey)
                        .scanIndexForward(scanIndexForward)
                        .limit(limit)
                        .consistentRead(consistentRead)
                        .filterExpression(filterExpression);
    }

    /**
     * Returns the matching condition of the query.
     */
    public QueryConditional queryConditional() {
        return queryConditional;
    }

    /**
     * Returns the value of the exclusive start key set on this request object, or null if it doesn't exist.
     */
    public Map<String, AttributeValue> exclusiveStartKey() {
        return exclusiveStartKey;
    }

    /**
     * Returns the value of scan index forward, meaning an ascending result sort order, or true if it
     * has not been set.
     */
    public Boolean scanIndexForward() {
        return scanIndexForward;
    }

    /**
     * Returns the value of limit set on this request object, or null if it doesn't exist.
     */
    public Integer limit() {
        return limit;
    }

    /**
     * Returns the value of consistent read, or false if it has not been set.
     */
    public Boolean consistentRead() {
        return consistentRead;
    }

    /**
     * Returns the return result filter {@link Expression} set on this request object, or null if it doesn't exist.
     */
    public Expression filterExpression() {
        return filterExpression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        QueryEnhancedRequest query = (QueryEnhancedRequest) o;

        if (queryConditional != null ? ! queryConditional.equals(query.queryConditional) :
            query.queryConditional != null) {
            return false;
        }
        if (exclusiveStartKey != null ? ! exclusiveStartKey.equals(query.exclusiveStartKey) :
            query.exclusiveStartKey != null) {
            return false;
        }
        if (scanIndexForward != null ? ! scanIndexForward.equals(query.scanIndexForward) :
            query.scanIndexForward != null) {
            return false;
        }
        if (limit != null ? ! limit.equals(query.limit) : query.limit != null) {
            return false;
        }
        if (consistentRead != null ? ! consistentRead.equals(query.consistentRead) : query.consistentRead != null) {
            return false;
        }
        return filterExpression != null ? filterExpression.equals(query.filterExpression) : query.filterExpression == null;
    }

    @Override
    public int hashCode() {
        int result = queryConditional != null ? queryConditional.hashCode() : 0;
        result = 31 * result + (exclusiveStartKey != null ? exclusiveStartKey.hashCode() : 0);
        result = 31 * result + (scanIndexForward != null ? scanIndexForward.hashCode() : 0);
        result = 31 * result + (limit != null ? limit.hashCode() : 0);
        result = 31 * result + (consistentRead != null ? consistentRead.hashCode() : 0);
        result = 31 * result + (filterExpression != null ? filterExpression.hashCode() : 0);
        return result;
    }

    /**
     * A builder that is used to create a request with the desired parameters.
     * <p>
     * A valid builder must set the {@link #queryConditional} parameter. Other parameters are optional.
     */
    public static final class Builder {
        private QueryConditional queryConditional;
        private Map<String, AttributeValue> exclusiveStartKey;
        private Boolean scanIndexForward;
        private Integer limit;
        private Boolean consistentRead;
        private Expression filterExpression;

        private Builder() {
        }

        /**
         * Determines the matching conditions for this query request. See {@link QueryConditional} for examples
         * and constraints. <b>Required</b>.
         *
         * @param queryConditional the query conditions
         * @return a builder of this type
         */
        public Builder queryConditional(QueryConditional queryConditional) {
            this.queryConditional = queryConditional;
            return this;
        }

        /**
         * Results are sorted by sort key in ascending order if {@link #scanIndexForward} is true. If its false, the
         * order is descending. The default value is true.
         *
         * @param scanIndexForward the sort order
         * @return a builder of this type
         */
        public Builder scanIndexForward(Boolean scanIndexForward) {
            this.scanIndexForward = scanIndexForward;
            return this;
        }

        /**
         * The primary key of the first item that this operation will evaluate. By default, the operation will evaluate
         * the whole dataset. If used, normally this parameter is populated with the value that was returned for
         * {@link Page#lastEvaluatedKey()} in the previous operation.
         *
         * @param exclusiveStartKey the primary key value where DynamoDb should start to evaluate items
         * @return a builder of this type
         */
        public Builder exclusiveStartKey(Map<String, AttributeValue> exclusiveStartKey) {
            this.exclusiveStartKey = exclusiveStartKey != null ? new HashMap<>(exclusiveStartKey) : null;
            return this;
        }

        /**
         * Sets a limit on how many items to evaluate in the query. If not set, the operation uses
         * the maximum values allowed.
         * <p>
         * <b>Note:</b>The limit does not refer to the number of items to return, but how many items
         * the database should evaluate while executing the query. Use limit together with {@link Page#lastEvaluatedKey()}
         * and {@link #exclusiveStartKey} in subsequent query calls to evaluate <em>limit</em> items per call.
         *
         * @param limit the maximum number of items to evalute
         * @return a builder of this type
         */
        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Determines the read consistency model: If set to true, the operation uses strongly consistent reads; otherwise,
         * the operation uses eventually consistent reads.
         * <p>
         * By default, the value of this property is set to <em>false</em>.
         *
         * @param consistentRead sets consistency model of the operation to use strong consistency
         * @return a builder of this type
         */
        public Builder consistentRead(Boolean consistentRead) {
            this.consistentRead = consistentRead;
            return this;
        }

        /**
         * Refines the query results by applying the filter expression on the results returned
         * from the query and discards items that do not match. See {@link Expression} for examples
         * and constraints.
         * <p>
         * <b>Note:</b> Using the filter expression does not reduce the cost of the query, since it is applied
         * <em>after</em> the database has found matching items.
         *
         * @param filterExpression an expression that filters results of evaluating the query
         * @return a builder of this type
         */
        public Builder filterExpression(Expression filterExpression) {
            this.filterExpression = filterExpression;
            return this;
        }

        public QueryEnhancedRequest build() {
            return new QueryEnhancedRequest(this);
        }
    }
}
