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

package software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans;

import software.amazon.awssdk.enhanced.dynamodb.mapper.Order;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@DynamoDbImmutable(builder = MixedOrderingImmutable.Builder.class)
public class MixedOrderingImmutable {
    private final String id;
    private final String key1;
    private final String key2;

    private MixedOrderingImmutable(Builder b) {
        this.id = b.id;
        this.key1 = b.key1;
        this.key2 = b.key2;
    }

    @DynamoDbPartitionKey
    public String id() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.FIRST)
    public String key1() {
        return key1;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1")
    public String key2() {
        return key2;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String key1;
        private String key2;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder key1(String key1) {
            this.key1 = key1;
            return this;
        }

        public Builder key2(String key2) {
            this.key2 = key2;
            return this;
        }

        public MixedOrderingImmutable build() {
            return new MixedOrderingImmutable(this);
        }
    }
}