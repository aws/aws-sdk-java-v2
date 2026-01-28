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

import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.mapper.Order;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@DynamoDbImmutable(builder = OrderPreservationImmutable.Builder.class)
public class OrderPreservationImmutable {
    private final String id;
    private final String key1;
    private final String key2;
    private final String key3;
    private final String key4;

    private OrderPreservationImmutable(Builder b) {
        this.id = b.id;
        this.key1 = b.key1;
        this.key2 = b.key2;
        this.key3 = b.key3;
        this.key4 = b.key4;
    }

    @DynamoDbPartitionKey
    public String id() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.FOURTH)
    public String key1() {
        return key1;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.SECOND)
    public String key2() {
        return key2;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.FIRST)
    public String key3() {
        return key3;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.THIRD)
    public String key4() {
        return key4;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderPreservationImmutable that = (OrderPreservationImmutable) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(key1, that.key1) &&
               Objects.equals(key2, that.key2) &&
               Objects.equals(key3, that.key3) &&
               Objects.equals(key4, that.key4);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, key1, key2, key3, key4);
    }

    public static final class Builder {
        private String id;
        private String key1;
        private String key2;
        private String key3;
        private String key4;

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

        public Builder key3(String key3) {
            this.key3 = key3;
            return this;
        }

        public Builder key4(String key4) {
            this.key4 = key4;
            return this;
        }

        public OrderPreservationImmutable build() {
            return new OrderPreservationImmutable(this);
        }
    }
}