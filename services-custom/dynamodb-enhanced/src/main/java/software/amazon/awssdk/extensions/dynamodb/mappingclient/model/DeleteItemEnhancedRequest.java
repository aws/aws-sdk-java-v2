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
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;

@SdkPublicApi
public final class DeleteItemEnhancedRequest {

    private final Key key;
    private final Expression conditionExpression;

    private DeleteItemEnhancedRequest(Builder builder) {
        this.key = builder.key;
        this.conditionExpression = builder.conditionExpression;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().key(key).conditionExpression(conditionExpression);
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

        DeleteItemEnhancedRequest that = (DeleteItemEnhancedRequest) o;

        return key != null ? key.equals(that.key) : that.key == null;
    }

    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }

    public static final class Builder {
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

        public DeleteItemEnhancedRequest build() {
            return new DeleteItemEnhancedRequest(this);
        }
    }
}
