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

@SdkPublicApi
public final class PutItemEnhancedRequest<T> {

    private final T item;
    private final Expression conditionExpression;

    private PutItemEnhancedRequest(Builder<T> builder) {
        this.item = builder.item;
        this.conditionExpression = builder.conditionExpression;
    }

    public static <T> PutItemEnhancedRequest<T> create(T item) {
        return new Builder<T>().item(item).build();
    }

    public static <T> Builder<T> builder(Class<? extends T> itemClass) {
        return new Builder<>();
    }

    public Builder<T> toBuilder() {
        return new Builder<T>().item(item).conditionExpression(conditionExpression);
    }

    public T item() {
        return item;
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

        PutItemEnhancedRequest<?> putItem = (PutItemEnhancedRequest<?>) o;

        return item != null ? item.equals(putItem.item) : putItem.item == null;
    }

    @Override
    public int hashCode() {
        return item != null ? item.hashCode() : 0;
    }

    public static final class Builder<T> {
        private T item;
        private Expression conditionExpression;

        private Builder() {
        }

        public Builder<T> item(T item) {
            this.item = item;
            return this;
        }

        public Builder<T> conditionExpression(Expression conditionExpression) {
            this.conditionExpression = conditionExpression;
            return this;
        }

        public PutItemEnhancedRequest<T> build() {
            return new PutItemEnhancedRequest<>(this);
        }
    }
}
