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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;

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

    private UpdateItemEnhancedRequest(Builder<T> builder) {
        this.item = builder.item;
        this.ignoreNulls = builder.ignoreNulls;
        this.conditionExpression = builder.conditionExpression;
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
        return new Builder<T>().item(item).ignoreNulls(ignoreNulls);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UpdateItemEnhancedRequest<?> that = (UpdateItemEnhancedRequest<?>) o;

        if (item != null ? ! item.equals(that.item) : that.item != null) {
            return false;
        }
        return ignoreNulls != null ? ignoreNulls.equals(that.ignoreNulls) : that.ignoreNulls == null;
    }

    @Override
    public int hashCode() {
        int result = item != null ? item.hashCode() : 0;
        result = 31 * result + (ignoreNulls != null ? ignoreNulls.hashCode() : 0);
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

        public UpdateItemEnhancedRequest<T> build() {
            return new UpdateItemEnhancedRequest<>(this);
        }
    }
}
