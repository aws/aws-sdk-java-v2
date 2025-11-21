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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

@DynamoDbImmutable(builder = MixedFlattenedImmutable.Builder.class)
public class MixedFlattenedImmutable {
    private final String id;
    private final String rootKey1;
    private final String rootKey2;
    private final FlattenedKeys flattenedKeys;

    private MixedFlattenedImmutable(Builder b) {
        this.id = b.id;
        this.rootKey1 = b.rootKey1;
        this.rootKey2 = b.rootKey2;
        this.flattenedKeys = b.flattenedKeys;
    }

    @DynamoDbPartitionKey
    public String id() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "mixed_gsi", order = Order.FIRST)
    public String rootKey1() {
        return rootKey1;
    }

    @DynamoDbSecondarySortKey(indexNames = "mixed_gsi", order = Order.FIRST)
    public String rootKey2() {
        return rootKey2;
    }

    @DynamoDbFlatten
    public FlattenedKeys flattenedKeys() {
        return flattenedKeys;
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
        MixedFlattenedImmutable that = (MixedFlattenedImmutable) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(rootKey1, that.rootKey1) &&
               Objects.equals(rootKey2, that.rootKey2) &&
               Objects.equals(flattenedKeys, that.flattenedKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, rootKey1, rootKey2, flattenedKeys);
    }

    public static final class Builder {
        private String id;
        private String rootKey1;
        private String rootKey2;
        private FlattenedKeys flattenedKeys;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder rootKey1(String rootKey1) {
            this.rootKey1 = rootKey1;
            return this;
        }

        public Builder rootKey2(String rootKey2) {
            this.rootKey2 = rootKey2;
            return this;
        }

        public Builder flattenedKeys(FlattenedKeys flattenedKeys) {
            this.flattenedKeys = flattenedKeys;
            return this;
        }

        public MixedFlattenedImmutable build() {
            return new MixedFlattenedImmutable(this);
        }
    }

    @DynamoDbImmutable(builder = FlattenedKeys.Builder.class)
    public static class FlattenedKeys {
        private final String flatKey1;
        private final String flatKey2;

        private FlattenedKeys(Builder b) {
            this.flatKey1 = b.flatKey1;
            this.flatKey2 = b.flatKey2;
        }

        @DynamoDbSecondaryPartitionKey(indexNames = "mixed_gsi", order = Order.SECOND)
        public String flatKey1() {
            return flatKey1;
        }

        @DynamoDbSecondarySortKey(indexNames = "mixed_gsi", order = Order.SECOND)
        public String flatKey2() {
            return flatKey2;
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
            FlattenedKeys that = (FlattenedKeys) o;
            return Objects.equals(flatKey1, that.flatKey1) &&
                   Objects.equals(flatKey2, that.flatKey2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(flatKey1, flatKey2);
        }

        public static final class Builder {
            private String flatKey1;
            private String flatKey2;

            private Builder() {
            }

            public Builder flatKey1(String flatKey1) {
                this.flatKey1 = flatKey1;
                return this;
            }

            public Builder flatKey2(String flatKey2) {
                this.flatKey2 = flatKey2;
                return this;
            }

            public FlattenedKeys build() {
                return new FlattenedKeys(this);
            }
        }
    }
}