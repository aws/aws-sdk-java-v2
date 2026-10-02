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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

@DynamoDbImmutable(builder = CrossIndexImmutable.Builder.class)
public class CrossIndexImmutable {
    private final String id;
    private final String attr1;
    private final String attr2;
    private final String attr3;

    private CrossIndexImmutable(Builder b) {
        this.id = b.id;
        this.attr1 = b.attr1;
        this.attr2 = b.attr2;
        this.attr3 = b.attr3;
    }

    @DynamoDbPartitionKey
    public String id() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.FIRST)
    @DynamoDbSecondarySortKey(indexNames = "gsi2", order = Order.FIRST)
    public String attr1() {
        return attr1;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.SECOND)
    public String attr2() {
        return attr2;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi2", order = Order.FIRST)
    public String attr3() {
        return attr3;
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
        CrossIndexImmutable that = (CrossIndexImmutable) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(attr1, that.attr1) &&
               Objects.equals(attr2, that.attr2) &&
               Objects.equals(attr3, that.attr3);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, attr1, attr2, attr3);
    }

    public static final class Builder {
        private String id;
        private String attr1;
        private String attr2;
        private String attr3;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder attr1(String attr1) {
            this.attr1 = attr1;
            return this;
        }

        public Builder attr2(String attr2) {
            this.attr2 = attr2;
            return this;
        }

        public Builder attr3(String attr3) {
            this.attr3 = attr3;
            return this;
        }

        public CrossIndexImmutable build() {
            return new CrossIndexImmutable(this);
        }
    }
}