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

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnItemCollectionMetrics;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

/**
 * Defines parameters used to write an item to a DynamoDb table using the putItem() operation (such as
 * {@link DynamoDbTable#putItem(PutItemEnhancedRequest)} or {@link DynamoDbAsyncTable#putItem(PutItemEnhancedRequest)}).
 * <p>
 * A valid request object must contain the item that should be written to the table.
 * @param <T> The type of the modelled object.
 */
@SdkPublicApi
public final class PutItemEnhancedRequest<T> {

    private final T item;
    private final Expression conditionExpression;
    private final String returnValues;
    private final String returnConsumedCapacity;
    private final String returnItemCollectionMetrics;

    private PutItemEnhancedRequest(Builder<T> builder) {
        this.item = builder.item;
        this.conditionExpression = builder.conditionExpression;
        this.returnValues = builder.returnValues;
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
        this.returnItemCollectionMetrics = builder.returnItemCollectionMetrics;
    }

    /**
     * Creates a newly initialized builder for the request object.
     *
     * @param itemClass the class that items in this table map to
     * @param <T> The type of the modelled object, corresponding to itemClass
     * @return a PutItemEnhancedRequest builder
     */
    public static <T> Builder<T> builder(Class<? extends T> itemClass) {
        return new Builder<>();
    }

    /**
     * Returns a builder initialized with all existing values on the request object.
     */
    public Builder<T> toBuilder() {
        return new Builder<T>().item(item)
                               .conditionExpression(conditionExpression)
                               .returnValues(returnValues)
                               .returnConsumedCapacity(returnConsumedCapacity)
                               .returnItemCollectionMetrics(returnItemCollectionMetrics);
    }

    /**
     * Returns the item for this put operation request.
     */
    public T item() {
        return item;
    }

    /**
     * Returns the condition {@link Expression} set on this request object, or null if it doesn't exist.
     */
    public Expression conditionExpression() {
        return conditionExpression;
    }

    /**
     * Whether to return the values of the item before this request.
     *
     * @see PutItemRequest#returnValues()
     */
    public ReturnValue returnValues() {
        return ReturnValue.fromValue(returnValues);
    }

    /**
     * Whether to return the values of the item before this request.
     * <p>
     * Similar to {@link #returnValues()} but returns the value as a string. This is useful in situations where the value is
     * not defined in {@link ReturnValue}.
     */
    public String returnValuesAsString() {
        return returnValues;
    }

    /**
     * Whether to return the capacity consumed by this operation.
     *
     * @see PutItemRequest#returnConsumedCapacity()
     */
    public ReturnConsumedCapacity returnConsumedCapacity() {
        return ReturnConsumedCapacity.fromValue(returnConsumedCapacity);
    }

    /**
     * Whether to return the capacity consumed by this operation.
     * <p>
     * Similar to {@link #returnConsumedCapacity()} but return the value as a string. This is useful in situations where the
     * value is not defined in {@link ReturnConsumedCapacity}.
     */
    public String returnConsumedCapacityAsString() {
        return returnConsumedCapacity;
    }

    /**
     * Whether to return the item collection metrics.
     *
     * @see PutItemRequest#returnItemCollectionMetrics()
     */
    public ReturnItemCollectionMetrics returnItemCollectionMetrics() {
        return ReturnItemCollectionMetrics.fromValue(returnItemCollectionMetrics);
    }

    /**
     * Whether to return the item collection metrics.
     * <p>
     * Similar to {@link #returnItemCollectionMetrics()} but return the value as a string. This is useful in situations 
     * where the
     * value is not defined in {@link ReturnItemCollectionMetrics}.
     */
    public String returnItemCollectionMetricsAsString() {
        return returnItemCollectionMetrics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PutItemEnhancedRequest<?> that = (PutItemEnhancedRequest<?>) o;
        return Objects.equals(item, that.item)
               && Objects.equals(conditionExpression, that.conditionExpression)
               && Objects.equals(returnValues, that.returnValues)
               && Objects.equals(returnConsumedCapacity, that.returnConsumedCapacity)
               && Objects.equals(returnItemCollectionMetrics, that.returnItemCollectionMetrics);
    }

    @Override
    public int hashCode() {
        int result = item != null ? item.hashCode() : 0;
        result = 31 * result + (conditionExpression != null ? conditionExpression.hashCode() : 0);
        result = 31 * result + (returnValues != null ? returnValues.hashCode() : 0);
        result = 31 * result + (returnConsumedCapacity != null ? returnConsumedCapacity.hashCode() : 0);
        result = 31 * result + (returnItemCollectionMetrics != null ? returnItemCollectionMetrics.hashCode() : 0);
        return result;
    }

    /**
     * A builder that is used to create a request with the desired parameters.
     * <p>
     * <b>Note</b>: A valid request builder must define an item.
     */
    public static final class Builder<T> {
        private T item;
        private Expression conditionExpression;
        private String returnValues;
        private String returnConsumedCapacity;
        private String returnItemCollectionMetrics;

        private Builder() {
        }

        /**
         * Sets the item to write to DynamoDb. Required.
         *
         * @param item the item to write
         * @return a builder of this type
         */
        public Builder<T> item(T item) {
            this.item = item;
            return this;
        }

        /**
         * Defines a logical expression on an item's attribute values which, if evaluating to true,
         * will allow the put operation to succeed. If evaluating to false, the operation will not succeed.
         * <p>
         * See {@link Expression} for condition syntax and examples.
         *
         * @param conditionExpression a condition written as an {@link Expression}
         * @return a builder of this type
         */
        public Builder<T> conditionExpression(Expression conditionExpression) {
            this.conditionExpression = conditionExpression;
            return this;
        }

        /**
         * Whether to return the values of the item before this request.
         *
         * @see PutItemRequest.Builder#returnValues(ReturnValue)
         */
        public Builder<T> returnValues(ReturnValue returnValues) {
            this.returnValues = returnValues == null ? null : returnValues.toString();
            return this;
        }

        /**
         * Whether to return the values of the item before this request.
         *
         * @see PutItemRequest.Builder#returnValues(String)
         */
        public Builder<T> returnValues(String returnValues) {
            this.returnValues = returnValues;
            return this;
        }

        /**
         * Whether to return the capacity consumed by this operation.
         * 
         * @see PutItemRequest.Builder#returnConsumedCapacity(ReturnConsumedCapacity)
         */
        public Builder<T> returnConsumedCapacity(ReturnConsumedCapacity returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity == null ? null : returnConsumedCapacity.toString();
            return this;
        }

        /**
         * Whether to return the capacity consumed by this operation.
         *
         * @see PutItemRequest.Builder#returnConsumedCapacity(String)
         */
        public Builder<T> returnConsumedCapacity(String returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity;
            return this;
        }

        /**
         * Whether to return the item collection metrics.
         *
         * @see PutItemRequest.Builder#returnItemCollectionMetrics(ReturnItemCollectionMetrics)
         */
        public Builder<T> returnItemCollectionMetrics(ReturnItemCollectionMetrics returnItemCollectionMetrics) {
            this.returnItemCollectionMetrics = returnItemCollectionMetrics == null ? null :
                                               returnItemCollectionMetrics.toString();
            return this;
        }

        /**
         * Whether to return the item collection metrics.
         *
         * @see PutItemRequest.Builder#returnItemCollectionMetrics(String)
         */
        public Builder<T> returnItemCollectionMetrics(String returnItemCollectionMetrics) {
            this.returnItemCollectionMetrics = returnItemCollectionMetrics;
            return this;
        }

        public PutItemEnhancedRequest<T> build() {
            return new PutItemEnhancedRequest<>(this);
        }
    }
}
