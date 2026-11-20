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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.models;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbImmutable(builder = ImmutableRecordForUpdateExpressions.Builder.class)
public final class ImmutableRecordForUpdateExpressions {
    private final String id;
    private final Long counterValue;

    private ImmutableRecordForUpdateExpressions(Builder builder) {
        this.id = builder.id;
        this.counterValue = builder.counterValue;
    }

    public static Builder builder() {
        return new Builder();
    }

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public Long getCounterValue() {
        return counterValue;
    }

    public static final class Builder {
        private String id;
        private Long counterValue;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder counterValue(Long counterValue) {
            this.counterValue = counterValue;
            return this;
        }

        public ImmutableRecordForUpdateExpressions build() {
            return new ImmutableRecordForUpdateExpressions(this);
        }
    }
}
