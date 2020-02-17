/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
public final class UpdateItemEnhancedRequest<T> {

    private final T item;
    private final Boolean ignoreNulls;
    private final Expression conditionExpression;

    private UpdateItemEnhancedRequest(Builder<T> builder) {
        this.item = builder.item;
        this.ignoreNulls = builder.ignoreNulls;
        this.conditionExpression = builder.conditionExpression;
    }

    public static <T> Builder<T> builder(Class<? extends T> itemClass) {
        return new Builder<>();
    }

    public Builder<T> toBuilder() {
        return new Builder<T>().item(item).ignoreNulls(ignoreNulls);
    }

    public T item() {
        return item;
    }

    public Boolean ignoreNulls() {
        return ignoreNulls;
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

    public static final class Builder<T> {
        private T item;
        private Boolean ignoreNulls;
        private Expression conditionExpression;

        private Builder() {
        }

        public Builder<T> ignoreNulls(Boolean ignoreNulls) {
            this.ignoreNulls = ignoreNulls;
            return this;
        }

        public Builder<T> conditionExpression(Expression conditionExpression) {
            this.conditionExpression = conditionExpression;
            return this;
        }

        public Builder<T> item(T item) {
            this.item = item;
            return this;
        }

        public UpdateItemEnhancedRequest<T> build() {
            return new UpdateItemEnhancedRequest<>(this);
        }
    }
}
