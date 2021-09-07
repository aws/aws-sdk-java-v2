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
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;

/**
 * Defines parameters used to delete an item from a DynamoDb table using the
 * {@link DynamoDbEnhancedClient#transactWriteItems(TransactWriteItemsEnhancedRequest)} or
 * {@link DynamoDbEnhancedAsyncClient#transactWriteItems(TransactWriteItemsEnhancedRequest)}
 * operation.
 * <p>
 * A valid request object must contain a primary {@link Key} to reference the item to delete.
 *
 */
@SdkPublicApi
public final class TransactDeleteItemEnhancedRequest {

    private final Key key;
    private final Expression conditionExpression;
    private final String returnValuesOnConditionCheckFailure;

    private TransactDeleteItemEnhancedRequest(Builder builder) {
        this.key = builder.key;
        this.conditionExpression = builder.conditionExpression;
        this.returnValuesOnConditionCheckFailure = builder.returnValuesOnConditionCheckFailure;
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
        return builder().key(key)
                        .conditionExpression(conditionExpression)
                        .returnValuesOnConditionCheckFailure(returnValuesOnConditionCheckFailure);
    }

    /**
     * Returns the primary {@link Key} for the item to delete.
     */
    public Key key() {
        return key;
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

        TransactDeleteItemEnhancedRequest that = (TransactDeleteItemEnhancedRequest) o;

        if (!Objects.equals(key, that.key)) {
            return false;
        }
        if (!Objects.equals(conditionExpression, that.conditionExpression)) {
            return false;
        }
        return Objects.equals(returnValuesOnConditionCheckFailure, that.returnValuesOnConditionCheckFailure);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(key);
        result = 31 * result + Objects.hashCode(conditionExpression);
        result = 31 * result + Objects.hashCode(returnValuesOnConditionCheckFailure);
        return result;
    }

    /**
     * A builder that is used to create a request with the desired parameters.
     * <p>
     * <b>Note</b>: A valid request builder must define a {@link Key}.
     */
    public static final class Builder {
        private Key key;
        private Expression conditionExpression;
        private String returnValuesOnConditionCheckFailure;

        private Builder() {
        }

        /**
         * Sets the primary {@link Key} that will be used to match the item to delete.
         *
         * @param key the primary key to use in the request.
         * @return a builder of this type
         */
        public Builder key(Key key) {
            this.key = key;
            return this;
        }

        /**
         * Sets the primary {@link Key} that will be used to match the item to delete
         * on the builder by accepting a consumer of {@link Key.Builder}.
         *
         * @param keyConsumer a {@link Consumer} of {@link Key}
         * @return a builder of this type
         */
        public Builder key(Consumer<Key.Builder> keyConsumer) {
            Key.Builder builder = Key.builder();
            keyConsumer.accept(builder);
            return key(builder.build());
        }

        /**
         * Defines a logical expression on an item's attribute values which, if evaluating to true,
         * will allow the delete operation to succeed. If evaluating to false, the operation will not succeed.
         * <p>
         * See {@link Expression} for condition syntax and examples.
         *
         * @param conditionExpression a condition written as an {@link Expression}
         * @return a builder of this type
         */
        public Builder conditionExpression(Expression conditionExpression) {
            this.conditionExpression = conditionExpression;
            return this;
        }

        /**
         * Use <code>ReturnValuesOnConditionCheckFailure</code> to get the item attributes if the <code>Delete</code>
         * condition fails. For <code>ReturnValuesOnConditionCheckFailure</code>, the valid values are: NONE and
         * ALL_OLD.
         *
         * @param returnValuesOnConditionCheckFailure What values to return on condition check failure.
         * @return a builder of this type
         */
        public Builder returnValuesOnConditionCheckFailure(
            ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure) {
            this.returnValuesOnConditionCheckFailure = returnValuesOnConditionCheckFailure == null ? null :
                                                       returnValuesOnConditionCheckFailure.toString();
            return this;
        }

        /**
         * Use <code>ReturnValuesOnConditionCheckFailure</code> to get the item attributes if the <code>Delete</code>
         * condition fails. For <code>ReturnValuesOnConditionCheckFailure</code>, the valid values are: NONE and
         * ALL_OLD.
         *
         * @param returnValuesOnConditionCheckFailure What values to return on condition check failure.
         * @return a builder of this type
         */
        public Builder returnValuesOnConditionCheckFailure(String returnValuesOnConditionCheckFailure) {
            this.returnValuesOnConditionCheckFailure = returnValuesOnConditionCheckFailure;
            return this;
        }


        public TransactDeleteItemEnhancedRequest build() {
            return new TransactDeleteItemEnhancedRequest(this);
        }
    }
}