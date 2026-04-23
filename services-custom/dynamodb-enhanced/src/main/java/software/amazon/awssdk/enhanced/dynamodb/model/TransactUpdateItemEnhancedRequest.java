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
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;

/**
 * Defines parameters used to update an item to a DynamoDb table using the
 * {@link DynamoDbEnhancedClient#transactWriteItems(TransactWriteItemsEnhancedRequest)} or
 * {@link DynamoDbEnhancedAsyncClient#transactWriteItems(TransactWriteItemsEnhancedRequest)}
 * operation.
 * <p>
 * A valid request object must contain the item that should be written to the table.
 *
 * @param <T> The type of the modelled object.
 */
@SdkPublicApi
@ThreadSafe
public class TransactUpdateItemEnhancedRequest<T> {

    private final T item;
    private final Boolean ignoreNulls;
    private final IgnoreNullsMode ignoreNullsMode;
    private final Expression conditionExpression;
    private final UpdateExpression updateExpression;
    private final UpdateExpressionMergeStrategy updateExpressionMergeStrategy;
    private final String returnValuesOnConditionCheckFailure;

    private TransactUpdateItemEnhancedRequest(Builder<T> builder) {
        this.item = builder.item;
        this.ignoreNulls = builder.ignoreNulls;
        this.ignoreNullsMode = builder.ignoreNullsMode;
        this.conditionExpression = builder.conditionExpression;
        this.updateExpression = builder.updateExpression;
        this.updateExpressionMergeStrategy = builder.updateExpressionMergeStrategy;
        this.returnValuesOnConditionCheckFailure = builder.returnValuesOnConditionCheckFailure;
    }

    /**
     * Creates a newly initialized builder for the request object.
     *
     * @param itemClass the class that items in this table map to
     * @param <T>       The type of the modelled object, corresponding to itemClass
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
                               .ignoreNullsMode(ignoreNullsMode)
                               .conditionExpression(conditionExpression)
                               .updateExpression(updateExpression)
                               .updateExpressionMergeStrategy(updateExpressionMergeStrategy)
                               .returnValuesOnConditionCheckFailure(returnValuesOnConditionCheckFailure);
    }

    /**
     * Returns the item for this update operation request.
     */
    public T item() {
        return item;
    }

    /**
     * Returns if the update operation should ignore attributes with null values, or false if it has not been set.
     * This is deprecated in favour of ignoreNullsMode()
     */
    @Deprecated
    public Boolean ignoreNulls() {
        return ignoreNulls;
    }

    /**
     * Returns the mode of update to be performed
     */
    public IgnoreNullsMode ignoreNullsMode() {
        return ignoreNullsMode;
    }

    /**
     * Returns the condition {@link Expression} set on this request object, or null if it doesn't exist.
     */
    public Expression conditionExpression() {
        return conditionExpression;
    }

    /**
     * Returns the update expression {@link UpdateExpression} set on this request object, or null if it doesn't exist.
     */
    public UpdateExpression updateExpression() {
        return updateExpression;
    }

    /**
     * Returns how POJO, extension, and request update actions are merged. Defaults to
     * {@link UpdateExpressionMergeStrategy#LEGACY} when unset on the builder.
     */
    public UpdateExpressionMergeStrategy updateExpressionMergeStrategy() {
        return updateExpressionMergeStrategy == null
               ? UpdateExpressionMergeStrategy.LEGACY
               : updateExpressionMergeStrategy;
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

        TransactUpdateItemEnhancedRequest<?> that = (TransactUpdateItemEnhancedRequest<?>) o;

        if (!Objects.equals(item, that.item)) {
            return false;
        }
        if (!Objects.equals(ignoreNulls, that.ignoreNulls)) {
            return false;
        }
        if (!Objects.equals(conditionExpression, that.conditionExpression)) {
            return false;
        }
        if (!Objects.equals(updateExpression, that.updateExpression)) {
            return false;
        }
        if (updateExpressionMergeStrategy != that.updateExpressionMergeStrategy) {
            return false;
        }
        return Objects.equals(returnValuesOnConditionCheckFailure, that.returnValuesOnConditionCheckFailure);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(item);
        result = 31 * result + Objects.hashCode(ignoreNulls);
        result = 31 * result + Objects.hashCode(conditionExpression);
        result = 31 * result + Objects.hashCode(updateExpression);
        result = 31 * result + Objects.hashCode(updateExpressionMergeStrategy);
        result = 31 * result + Objects.hashCode(returnValuesOnConditionCheckFailure);
        return result;
    }

    /**
     * A builder that is used to create a request with the desired parameters.
     * <p>
     * <b>Note</b>: A valid request builder must define an item.
     */
    @NotThreadSafe
    public static final class Builder<T> {
        private T item;
        private Boolean ignoreNulls;
        private IgnoreNullsMode ignoreNullsMode;
        private Expression conditionExpression;
        private UpdateExpression updateExpression;
        private UpdateExpressionMergeStrategy updateExpressionMergeStrategy;
        private String returnValuesOnConditionCheckFailure;

        private Builder() {
        }

        /**
         * Sets if the update operation should ignore attributes with null values. By default, the value is false.
         * <p>
         * If set to true, any null values in the Java object will be ignored and not be updated on the persisted
         * record. This is commonly referred to as a 'partial update'.
         * If set to false, null values in the Java object will cause those attributes to be removed from the persisted
         * record on update.
         *
         * @param ignoreNulls the boolean value
         * @return a builder of this type
         */
        @Deprecated
        public Builder<T> ignoreNulls(Boolean ignoreNulls) {
            this.ignoreNulls = ignoreNulls;
            return this;
        }

        public Builder<T> ignoreNullsMode(IgnoreNullsMode ignoreNullsMode) {
            this.ignoreNullsMode = ignoreNullsMode;
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
         * Specifies custom update actions using DynamoDB's native update expression syntax. This expression is combined with
         * POJO-derived actions and extension-provided actions.
         * <p>
         * Use {@link #updateExpressionMergeStrategy(UpdateExpressionMergeStrategy)} to control how conflicts between these
         * sources are resolved ({@link UpdateExpressionMergeStrategy#LEGACY} vs
         * {@link UpdateExpressionMergeStrategy#PRIORITIZE_HIGHER_SOURCE}).
         *
         * @param updateExpression the update operations to perform
         * @return a builder of this type
         * @see UpdateExpressionMergeStrategy
         */
        public Builder<T> updateExpression(UpdateExpression updateExpression) {
            this.updateExpression = updateExpression;
            return this;
        }

        /**
         * Sets how update actions from POJO attributes, extensions, and this request's expression are combined. Defaults to
         * {@link UpdateExpressionMergeStrategy#LEGACY}. See {@link UpdateExpressionMergeStrategy} for behavior of each mode.
         *
         * @param updateExpressionMergeStrategy the merge strategy to use
         * @return a builder of this type
         * @see UpdateExpressionMergeStrategy
         */
        public Builder<T> updateExpressionMergeStrategy(
            UpdateExpressionMergeStrategy updateExpressionMergeStrategy) {
            this.updateExpressionMergeStrategy = updateExpressionMergeStrategy;
            return this;
        }

        /**
         * Use <code>ReturnValuesOnConditionCheckFailure</code> to get the item attributes if the <code>ConditionCheck</code>
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
         * Use <code>ReturnValuesOnConditionCheckFailure</code> to get the item attributes if the <code>ConditionCheck</code>
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

        public TransactUpdateItemEnhancedRequest<T> build() {
            return new TransactUpdateItemEnhancedRequest<>(this);
        }
    }
}