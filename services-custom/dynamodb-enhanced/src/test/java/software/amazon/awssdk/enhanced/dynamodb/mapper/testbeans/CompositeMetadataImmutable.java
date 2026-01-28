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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbImmutable(builder = CompositeMetadataImmutable.Builder.class)
public class CompositeMetadataImmutable {
    private final String id;
    private final String sort;
    private final String gsiPk1;
    private final String gsiPk2;
    private final String gsiSk1;
    private final String gsiSk2;

    private CompositeMetadataImmutable(Builder b) {
        this.id = b.id;
        this.sort = b.sort;
        this.gsiPk1 = b.gsiPk1;
        this.gsiPk2 = b.gsiPk2;
        this.gsiSk1 = b.gsiSk1;
        this.gsiSk2 = b.gsiSk2;
    }

    @DynamoDbPartitionKey
    public String id() {
        return id;
    }

    @DynamoDbSortKey
    public String sort() {
        return sort;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.FIRST)
    public String gsiPk1() {
        return gsiPk1;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.SECOND)
    public String gsiPk2() {
        return gsiPk2;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi1", order = Order.FIRST)
    public String gsiSk1() {
        return gsiSk1;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi1", order = Order.SECOND)
    public String gsiSk2() {
        return gsiSk2;
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
        CompositeMetadataImmutable that = (CompositeMetadataImmutable) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(sort, that.sort) &&
               Objects.equals(gsiPk1, that.gsiPk1) &&
               Objects.equals(gsiPk2, that.gsiPk2) &&
               Objects.equals(gsiSk1, that.gsiSk1) &&
               Objects.equals(gsiSk2, that.gsiSk2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sort, gsiPk1, gsiPk2, gsiSk1, gsiSk2);
    }

    public static final class Builder {
        private String id;
        private String sort;
        private String gsiPk1;
        private String gsiPk2;
        private String gsiSk1;
        private String gsiSk2;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder sort(String sort) {
            this.sort = sort;
            return this;
        }

        public Builder gsiPk1(String gsiPk1) {
            this.gsiPk1 = gsiPk1;
            return this;
        }

        public Builder gsiPk2(String gsiPk2) {
            this.gsiPk2 = gsiPk2;
            return this;
        }

        public Builder gsiSk1(String gsiSk1) {
            this.gsiSk1 = gsiSk1;
            return this;
        }

        public Builder gsiSk2(String gsiSk2) {
            this.gsiSk2 = gsiSk2;
            return this;
        }

        public CompositeMetadataImmutable build() {
            return new CompositeMetadataImmutable(this);
        }
    }
}