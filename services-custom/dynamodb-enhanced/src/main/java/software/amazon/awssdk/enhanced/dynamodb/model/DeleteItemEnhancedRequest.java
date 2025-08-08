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
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnItemCollectionMetrics;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;

/**
 * Defines parameters used to remove an item from a DynamoDb table using the deleteItem() operation (such as
 * {@link DynamoDbTable#deleteItem(DeleteItemEnhancedRequest)} or
 * {@link DynamoDbAsyncTable#deleteItem(DeleteItemEnhancedRequest)}).
 * <p>
 * A valid request object must contain a primary {@link Key} to reference the item to delete.
 */
@SdkPublicApi
@ThreadSafe
public final class DeleteItemEnhancedRequest {

    private final Key key;
    private final Expression conditionExpression;
    private final String returnConsumedCapacity;
    private final String returnItemCollectionMetrics;
    private final String returnValuesOnConditionCheckFailure;
    private final AwsRequestOverrideConfiguration overrideConfiguration;

    private DeleteItemEnhancedRequest(Builder builder) {
        this.key = builder.key;
        this.conditionExpression = builder.conditionExpression;
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
        this.returnItemCollectionMetrics = builder.returnItemCollectionMetrics;
        this.returnValuesOnConditionCheckFailure = builder.returnValuesOnConditionCheckFailure;
        this.overrideConfiguration = builder.overrideConfiguration;
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
                        .returnConsumedCapacity(returnConsumedCapacity)
                        .returnItemCollectionMetrics(returnItemCollectionMetrics)
                        .returnValuesOnConditionCheckFailure(returnValuesOnConditionCheckFailure)
                        .overrideConfiguration(overrideConfiguration);
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

    /**
     * Whether to return the item on condition check failure.
     *
     * @see DeleteItemRequest#returnValuesOnConditionCheckFailure()
     */
    public ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure() {
        return ReturnValuesOnConditionCheckFailure.fromValue(returnValuesOnConditionCheckFailure);
    }

    /**
     * Whether to return the item on condition check failure.
     * <p>
     * Similar to {@link #returnValuesOnConditionCheckFailure()} but return the value as a string. This is useful in situations
     * where the value is not defined in {@link ReturnValuesOnConditionCheckFailure}.
     */
    public String returnValuesOnConditionCheckFailureAsString() {
        return returnValuesOnConditionCheckFailure;
    }

    /**
     * Returns the override configuration to apply to the low-level {@link DeleteItemRequest}.
     * <p>
     * This can be used to customize the request, such as adding custom headers, MetricPublisher or AwsCredentialsProvider.
     * </p>
     *
     * @return the {@link AwsRequestOverrideConfiguration} to apply to the underlying service call.
     */
    public AwsRequestOverrideConfiguration overrideConfiguration() {
        return overrideConfiguration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeleteItemEnhancedRequest that = (DeleteItemEnhancedRequest) o;
        return Objects.equals(key, that.key)
               && Objects.equals(conditionExpression, that.conditionExpression)
               && Objects.equals(returnConsumedCapacity, that.returnConsumedCapacity)
               && Objects.equals(returnItemCollectionMetrics, that.returnItemCollectionMetrics)
               && Objects.equals(returnValuesOnConditionCheckFailure, that.returnValuesOnConditionCheckFailure)
               && Objects.equals(overrideConfiguration, that.overrideConfiguration);
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (conditionExpression != null ? conditionExpression.hashCode() : 0);
        result = 31 * result + (returnConsumedCapacity != null ? returnConsumedCapacity.hashCode() : 0);
        result = 31 * result + (returnItemCollectionMetrics != null ? returnItemCollectionMetrics.hashCode() : 0);
        result = 31 * result + (returnValuesOnConditionCheckFailure != null ? returnValuesOnConditionCheckFailure.hashCode() : 0);
        result = 31 * result + (overrideConfiguration != null ? overrideConfiguration.hashCode() : 0);
        return result;
    }

    /**
     * A builder that is used to create a request with the desired parameters.
     * <p>
     * <b>Note</b>: A valid request builder must define a {@link Key}.
     */
    @NotThreadSafe
    public static final class Builder {
        private Key key;
        private Expression conditionExpression;
        private String returnConsumedCapacity;
        private String returnItemCollectionMetrics;
        private String returnValuesOnConditionCheckFailure;
        private AwsRequestOverrideConfiguration overrideConfiguration;

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
         * Whether to return the capacity consumed by this operation.
         *
         * @see DeleteItemRequest.Builder#returnConsumedCapacity(ReturnConsumedCapacity)
         */
        public Builder returnConsumedCapacity(ReturnConsumedCapacity returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity == null ? null : returnConsumedCapacity.toString();
            return this;
        }

        /**
         * Whether to return the capacity consumed by this operation.
         *
         * @see DeleteItemRequest.Builder#returnConsumedCapacity(String)
         */
        public Builder returnConsumedCapacity(String returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity;
            return this;
        }

        /**
         * Whether to return the item collection metrics.
         *
         * @see DeleteItemRequest.Builder#returnItemCollectionMetrics(ReturnItemCollectionMetrics)
         */
        public Builder returnItemCollectionMetrics(ReturnItemCollectionMetrics returnItemCollectionMetrics) {
            this.returnItemCollectionMetrics = returnItemCollectionMetrics == null ? null :
                                               returnItemCollectionMetrics.toString();
            return this;
        }

        /**
         * Whether to return the item collection metrics.
         *
         * @see DeleteItemRequest.Builder#returnItemCollectionMetrics(String)
         */
        public Builder returnItemCollectionMetrics(String returnItemCollectionMetrics) {
            this.returnItemCollectionMetrics = returnItemCollectionMetrics;
            return this;
        }

        /**
         * Whether to return the item on condition check failure.
         *
         * @see DeleteItemRequest.Builder#returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure)
         */
        public Builder returnValuesOnConditionCheckFailure(
            ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure) {

            this.returnValuesOnConditionCheckFailure = returnValuesOnConditionCheckFailure == null ? null :
                                                       returnValuesOnConditionCheckFailure.toString();
            return this;
        }

        /**
         * Whether to return the item on condition check failure.
         *
         * @see DeleteItemRequest.Builder#returnValuesOnConditionCheckFailure(String)
         */
        public Builder returnValuesOnConditionCheckFailure(String returnValuesOnConditionCheckFailure) {
            this.returnValuesOnConditionCheckFailure = returnValuesOnConditionCheckFailure;
            return this;
        }

        /**
         * Sets the override configuration to apply to the low-level {@link DeleteItemRequest}.
         *
         * @see DeleteItemRequest.Builder#overrideConfiguration(AwsRequestOverrideConfiguration)
         * @return a builder of this type
         */
        public Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration) {
            this.overrideConfiguration = overrideConfiguration;
            return this;
        }

        /**
         * Sets the override configuration to apply to the low-level {@link DeleteItemRequest}.
         *
         * @see DeleteItemRequest.Builder#overrideConfiguration(Consumer)
         * @return a builder of this type
         */
        public Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> overrideConfigurationBuilder) {
            AwsRequestOverrideConfiguration.Builder builder = AwsRequestOverrideConfiguration.builder();
            overrideConfigurationBuilder.accept(builder);
            this.overrideConfiguration = builder.build();
            return this;
        }

        public DeleteItemEnhancedRequest build() {
            return new DeleteItemEnhancedRequest(this);
        }
    }
}
