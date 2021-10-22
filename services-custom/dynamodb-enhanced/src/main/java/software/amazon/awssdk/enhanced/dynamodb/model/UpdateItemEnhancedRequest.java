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
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnItemCollectionMetrics;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

/**
 * Defines parameters used to update an item to a DynamoDb table using the updateItem() operation (such as
 * {@link DynamoDbTable#updateItem(UpdateItemEnhancedRequest)} or
 * {@link DynamoDbAsyncTable#updateItem(UpdateItemEnhancedRequest)}).
 * <p>
 * A valid request object must contain the item that should be written to the table.
 *
 * @param <T> The type of the modelled object.
 */
@SdkPublicApi
public final class UpdateItemEnhancedRequest<T> {

    private final T item;
    private final Boolean ignoreNulls;
    private final Expression conditionExpression;
    private final String returnConsumedCapacity;
    private final String returnItemCollectionMetrics;


    private UpdateItemEnhancedRequest(Builder<T> builder) {
        this.item = builder.item;
        this.ignoreNulls = builder.ignoreNulls;
        this.conditionExpression = builder.conditionExpression;
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
        this.returnItemCollectionMetrics = builder.returnItemCollectionMetrics;
    }

    /**
     * Creates a newly initialized builder for the request object.
     *
     * @param itemClass the class that items in this table map to
     * @param <T> The type of the modelled object, corresponding to itemClass
     * @return a UpdateItemEnhancedRequest builder
     */
    public static <T> Builder<T> builder(Class<? extends T> itemClass) {
        return new Builder<>();
    }

    /**
     * Returns a builder initialized with all existing values on the request object.
     */
    public Builder<T> toBuilder() {
        return new Builder<T>().item(item)
                               .ignoreNulls(ignoreNulls)
                               .conditionExpression(conditionExpression)
                               .returnConsumedCapacity(returnConsumedCapacity)
                               .returnItemCollectionMetrics(returnItemCollectionMetrics);
    }

    /**
     * Returns the item for this update operation request.
     */
    public T item() {
        return item;
    }

    /**
     * Returns if the update operation should ignore attributes with null values, or false if it has not been set.
     */
    public Boolean ignoreNulls() {
        return ignoreNulls;
    }

    /**
     * Returns the condition {@link Expression} set on this request object, or null if it doesn't exist.
     */
    public Expression conditionExpression() {
        return conditionExpression;
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
     * @see DeleteItemRequest#returnItemCollectionMetrics()
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
        UpdateItemEnhancedRequest<?> that = (UpdateItemEnhancedRequest<?>) o;
        return Objects.equals(item, that.item)
               && Objects.equals(ignoreNulls, that.ignoreNulls)
               && Objects.equals(conditionExpression, that.conditionExpression)
               && Objects.equals(returnConsumedCapacity, that.returnConsumedCapacity)
               && Objects.equals(returnItemCollectionMetrics, that.returnItemCollectionMetrics);
    }

    @Override
    public int hashCode() {
        int result = item != null ? item.hashCode() : 0;
        result = 31 * result + (ignoreNulls != null ? ignoreNulls.hashCode() : 0);
        result = 31 * result + (conditionExpression != null ? conditionExpression.hashCode() : 0);
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
        private Boolean ignoreNulls;
        private Expression conditionExpression;
        private String returnConsumedCapacity;
        private String returnItemCollectionMetrics;

        private Builder() {
        }

        /**
         *  Sets if the update operation should ignore attributes with null values. By default, the value is false.
         *  <p>
         *  If set to true, any null values in the Java object will be ignored and not be updated on the persisted
         *  record. This is commonly referred to as a 'partial update'.
         *  If set to false, null values in the Java object will cause those attributes to be removed from the persisted
         *  record on update.
         * @param ignoreNulls the boolean value
         * @return a builder of this type
         */
        public Builder<T> ignoreNulls(Boolean ignoreNulls) {
            this.ignoreNulls = ignoreNulls;
            return this;
        }

        /**
         * Defines a logical expression on an item's attribute values which, if evaluating to true,
         * will allow the update operation to succeed. If evaluating to false, the operation will not succeed.
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
         * Whether to return the capacity consumed by this operation.
         *
         * @see UpdateItemRequest.Builder#returnConsumedCapacity(ReturnConsumedCapacity)
         */
        public Builder<T> returnConsumedCapacity(ReturnConsumedCapacity returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity == null ? null : returnConsumedCapacity.toString();
            return this;
        }

        /**
         * Whether to return the capacity consumed by this operation.
         *
         * @see UpdateItemRequest.Builder#returnConsumedCapacity(String)
         */
        public Builder<T> returnConsumedCapacity(String returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity;
            return this;
        }

        /**
         * Whether to return the item collection metrics.
         *
         * @see UpdateItemRequest.Builder#returnItemCollectionMetrics(ReturnItemCollectionMetrics)
         */
        public Builder<T> returnItemCollectionMetrics(ReturnItemCollectionMetrics returnItemCollectionMetrics) {
            this.returnItemCollectionMetrics = returnItemCollectionMetrics == null ? null :
                                               returnItemCollectionMetrics.toString();
            return this;
        }

        /**
         * Whether to return the item collection metrics.
         *
         * @see UpdateItemRequest.Builder#returnItemCollectionMetrics(String)
         */
        public Builder<T> returnItemCollectionMetrics(String returnItemCollectionMetrics) {
            this.returnItemCollectionMetrics = returnItemCollectionMetrics;
            return this;
        }

        public UpdateItemEnhancedRequest<T> build() {
            return new UpdateItemEnhancedRequest<>(this);
        }
    }
}
