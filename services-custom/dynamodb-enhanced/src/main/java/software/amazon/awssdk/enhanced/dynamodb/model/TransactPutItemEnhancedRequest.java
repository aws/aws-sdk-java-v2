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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;

/**
 * Defines parameters used to write an item to a DynamoDb table using
 * {@link DynamoDbEnhancedClient#transactWriteItems(TransactWriteItemsEnhancedRequest)} and
 * {@link DynamoDbEnhancedAsyncClient#transactWriteItems(TransactWriteItemsEnhancedRequest)}.
 * <p>
 * A valid request object must contain the item that should be written to the table.
 *
 * @param <T> The type of the modelled object.
 */
@SdkPublicApi
public final class TransactPutItemEnhancedRequest<T> {
    private final T item;
    private final Expression conditionExpression;
    private final String returnValuesOnConditionCheckFailure;

    private TransactPutItemEnhancedRequest(Builder<T> builder) {
        this.item = builder.item;
        this.conditionExpression = builder.conditionExpression;
        this.returnValuesOnConditionCheckFailure = builder.returnValuesOnConditionCheckFailure;
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
                               .returnValuesOnConditionCheckFailure(returnValuesOnConditionCheckFailureAsString());
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
     * Returns what values to return if the condition check fails.
     * <p>
     * If the service returns an enum value that is not available in the current SDK version,
     * {@link #returnValuesOnConditionCheckFailure} will return
     * {@link ReturnValuesOnConditionCheckFailure#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is
     * available from {@link #returnValuesOnConditionCheckFailureAsString}.
     *
     * @return What values to return on condition check failure.
     */
    public ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure() {
        return ReturnValuesOnConditionCheckFailure.fromValue(returnValuesOnConditionCheckFailure);
    }

    /**
     * Returns what values to return if the condition check fails.
     * <p>
     * If the service returns an enum value that is not available in the current SDK version,
     * {@link #returnValuesOnConditionCheckFailure} will return
     * {@link ReturnValuesOnConditionCheckFailure#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is
     * available from {@link #returnValuesOnConditionCheckFailureAsString}.
     *
     * @return What values to return on condition check failure.
     */
    public String returnValuesOnConditionCheckFailureAsString() {
        return returnValuesOnConditionCheckFailure;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransactPutItemEnhancedRequest<?> that = (TransactPutItemEnhancedRequest<?>) o;

        if (!Objects.equals(item, that.item)) {
            return false;
        }
        if (!Objects.equals(conditionExpression, that.conditionExpression)) {
            return false;
        }
        return Objects.equals(returnValuesOnConditionCheckFailure, that.returnValuesOnConditionCheckFailure);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(item);
        result = 31 * result + Objects.hashCode(conditionExpression);
        result = 31 * result + Objects.hashCode(returnValuesOnConditionCheckFailure);
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
        private String returnValuesOnConditionCheckFailure;

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
         * Use <code>ReturnValuesOnConditionCheckFailure</code> to get the item attributes if the <code>PutItem</code>
         * condition fails. For <code>ReturnValuesOnConditionCheckFailure</code>, the valid values are: NONE and
         * ALL_OLD.
         *
         * @param returnValuesOnConditionCheckFailure What values to return on condition check failure.
         * @return a builder of this type
         */
        public Builder<T> returnValuesOnConditionCheckFailure(
            ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure) {
            this.returnValuesOnConditionCheckFailure = returnValuesOnConditionCheckFailure == null ? null :
                                                       returnValuesOnConditionCheckFailure.toString();
            return this;
        }

        /**
         * Use <code>ReturnValuesOnConditionCheckFailure</code> to get the item attributes if the <code>PutItem</code>
         * condition fails. For <code>ReturnValuesOnConditionCheckFailure</code>, the valid values are: NONE and
         * ALL_OLD.
         *
         * @param returnValuesOnConditionCheckFailure What values to return on condition check failure.
         * @return a builder of this type
         */
        public Builder<T> returnValuesOnConditionCheckFailure(String returnValuesOnConditionCheckFailure) {
            this.returnValuesOnConditionCheckFailure = returnValuesOnConditionCheckFailure;
            return this;
        }

        public TransactPutItemEnhancedRequest<T> build() {
            return new TransactPutItemEnhancedRequest<>(this);
        }
    }
}
