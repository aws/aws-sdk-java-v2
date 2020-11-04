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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbImmutable(builder = SimpleImmutable.Builder.class)
public class SimpleImmutable {
    private final String id;
    private final Integer integerAttribute;

    private SimpleImmutable(Builder b) {
        this.id = b.id;
        this.integerAttribute = b.integerAttribute;
    }

    @DynamoDbPartitionKey
    public String id() {
        return this.id;
    }

    public Integer integerAttribute() {
        return integerAttribute;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleImmutable that = (SimpleImmutable) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(integerAttribute, that.integerAttribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, integerAttribute);
    }

    public static final class Builder {
        private String id;
        private Integer integerAttribute;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder integerAttribute(Integer integerAttribute) {
            this.integerAttribute = integerAttribute;
            return this;
        }

        public SimpleImmutable build() {
            return new SimpleImmutable(this);
        }
    }
}
