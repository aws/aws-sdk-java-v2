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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.model;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TransactableWriteOperation;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

@SdkPublicApi
public final class ConditionCheck<T> implements TransactableWriteOperation<T> {
    private final Key key;
    private final Expression conditionExpression;

    private ConditionCheck(Key key, Expression conditionExpression) {
        this.key = key;
        this.conditionExpression = conditionExpression;
    }

    public static <T> ConditionCheck<T> create(Key key, Expression conditionExpression) {
        return new ConditionCheck<>(key, conditionExpression);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().key(key).conditionExpression(conditionExpression);
    }

    @Override
    public TransactWriteItem generateTransactWriteItem(TableSchema<T> tableSchema,
                                                       OperationContext operationContext,
                                                       MapperExtension mapperExtension) {
        software.amazon.awssdk.services.dynamodb.model.ConditionCheck conditionCheck =
            software.amazon.awssdk.services.dynamodb.model.ConditionCheck
                .builder()
                .tableName(operationContext.tableName())
                .key(key.keyMap(tableSchema, operationContext.indexName()))
                .conditionExpression(conditionExpression.expression())
                .expressionAttributeNames(conditionExpression.expressionNames())
                .expressionAttributeValues(conditionExpression.expressionValues())
                .build();

        return TransactWriteItem.builder()
                                .conditionCheck(conditionCheck)
                                .build();
    }

    public Key key() {
        return key;
    }

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

        ConditionCheck<?> that = (ConditionCheck<?>) o;

        if (key != null ? ! key.equals(that.key) : that.key != null) {
            return false;
        }
        return conditionExpression != null ? conditionExpression.equals(that.conditionExpression) :
            that.conditionExpression == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (conditionExpression != null ? conditionExpression.hashCode() : 0);
        return result;
    }

    public static final class Builder  {
        private Key key;
        private Expression conditionExpression;

        private Builder() {
        }

        public Builder key(Key key) {
            this.key = key;
            return this;
        }

        public Builder conditionExpression(Expression conditionExpression) {
            this.conditionExpression = conditionExpression;
            return this;
        }

        public <T> ConditionCheck<T> build() {
            return new ConditionCheck<>(key, conditionExpression);
        }
    }
}
