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

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnoreNulls;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbImmutable(builder = NestedImmutableIgnoreNulls.Builder.class)
public class NestedImmutableIgnoreNulls {
    private final String id;
    private final AbstractImmutable innerBean1;
    private final AbstractImmutable innerBean2;

    private NestedImmutableIgnoreNulls(Builder b) {
        this.id = b.id;
        this.innerBean1 = b.innerBean1;
        this.innerBean2 = b.innerBean2;
    }

    @DynamoDbPartitionKey
    public String id() {
        return this.id;
    }

    @DynamoDbIgnoreNulls
    public AbstractImmutable innerBean1() {
        return innerBean1;
    }

    public AbstractImmutable innerBean2() {
        return innerBean2;
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

        NestedImmutableIgnoreNulls that = (NestedImmutableIgnoreNulls) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (innerBean2 != null ? !innerBean2.equals(that.innerBean2) : that.innerBean2 != null) {
            return false;
        }
        return innerBean1 != null ? innerBean1.equals(that.innerBean1) : that.innerBean1 == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (innerBean2 != null ? innerBean2.hashCode() : 0);
        result = 31 * result + (innerBean1 != null ? innerBean1.hashCode() : 0);
        return result;
    }

    public static final class Builder {
        private AbstractImmutable innerBean1;
        private String id;
        private AbstractImmutable innerBean2;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder innerBean1(AbstractImmutable innerBean1) {
            this.innerBean1 = innerBean1;
            return this;
        }

        public Builder innerBean2(AbstractImmutable innerBean2) {
            this.innerBean2 = innerBean2;
            return this;
        }

        public NestedImmutableIgnoreNulls build() {
            return new NestedImmutableIgnoreNulls(this);
        }
    }
}
