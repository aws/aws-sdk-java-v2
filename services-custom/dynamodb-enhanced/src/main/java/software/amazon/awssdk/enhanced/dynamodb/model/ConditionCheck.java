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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.TransactableWriteOperation;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

/**
 * Use ConditionCheck as a part of the composite operation transactGetItems (for example
 * {@link DynamoDbEnhancedClient#transactGetItems(TransactGetItemsEnhancedRequest)}) to determine
 * if the other actions that are part of the same transaction should take effect.
 * <p>
 * A valid ConditionCheck object should contain a reference to the primary key of the table that finds items with a matching key,
 * together with a condition (of type {@link Expression}) to evaluate the primary key.
 *
 * @param <T> The type of the modelled object.
 */
@SdkPublicApi
public final class ConditionCheck<T> implements TransactableWriteOperation<T> {
    private final Key key;
    private final Expression conditionExpression;
    private final String returnValuesOnConditionCheckFailure;

    private ConditionCheck(Builder builder) {
        this.key = builder.key;
        this.conditionExpression = builder.conditionExpression;
        this.returnValuesOnConditionCheckFailure = builder.returnValuesOnConditionCheckFailure;
    }

    /**
     * Creates a newly initialized builder for this object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a builder initialized with all existing values on the object.
     */
    public Builder toBuilder() {
        return new Builder().key(key)
                            .conditionExpression(conditionExpression)
                            .returnValuesOnConditionCheckFailure(returnValuesOnConditionCheckFailure);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConditionCheck<?> that = (ConditionCheck<?>) o;

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
     * Returns the primary {@link Key} that the condition is valid for, or null if it doesn't exist.
     */
    public Key key() {
        return key;
    }

    /**
     * Returns the condition {@link Expression} set on this object, or null if it doesn't exist.
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
    public TransactWriteItem generateTransactWriteItem(TableSchema<T> tableSchema,
                                                       OperationContext operationContext,
                                                       DynamoDbEnhancedClientExtension dynamoDbEnhancedClientExtension) {
        software.amazon.awssdk.services.dynamodb.model.ConditionCheck conditionCheck =
            software.amazon.awssdk.services.dynamodb.model.ConditionCheck
                .builder()
                .tableName(operationContext.tableName())
                .key(key.keyMap(tableSchema, operationContext.indexName()))
                .conditionExpression(conditionExpression.expression())
                .expressionAttributeNames(conditionExpression.expressionNames())
                .expressionAttributeValues(conditionExpression.expressionValues())
                .returnValuesOnConditionCheckFailure(returnValuesOnConditionCheckFailure)
                .build();

        return TransactWriteItem.builder()
                                .conditionCheck(conditionCheck)
                                .build();
    }

    /**
     * A builder that is used to create a condition check with the desired parameters.
     * <p>
     * A valid builder must define both a {@link Key} and an {@link Expression}.
     */
    public static final class Builder  {
        private Key key;
        private Expression conditionExpression;
        private String returnValuesOnConditionCheckFailure;

        private Builder() {
        }

        /**
         * Sets the primary {@link Key} that will be used together with the condition expression.
         *
         * @param key the primary key to use in the operation.
         * @return a builder of this type
         */
        public Builder key(Key key) {
            this.key = key;
            return this;
        }

        /**
         * Sets the primary {@link Key} that will be used together with the condition expression
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
         * Defines a logical expression on the attributes of table items that match the supplied primary key value(s).
         * If the expression evaluates to true, the transaction operation succeeds. If the expression evaluates to false,
         * the transaction will not succeed.
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
         * Use <code>ReturnValuesOnConditionCheckFailure</code> to get the item attributes if the <code>ConditionCheck</code>
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
         * Use <code>ReturnValuesOnConditionCheckFailure</code> to get the item attributes if the <code>ConditionCheck</code>
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

        public <T> ConditionCheck<T> build() {
            return new ConditionCheck<T>(this);
        }
    }
}
