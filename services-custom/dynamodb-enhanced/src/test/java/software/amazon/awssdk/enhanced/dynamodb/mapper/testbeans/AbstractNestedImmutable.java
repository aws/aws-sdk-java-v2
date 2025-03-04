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

@DynamoDbImmutable(builder = AbstractNestedImmutable.Builder.class)
public class AbstractNestedImmutable {
    private final String attribute2;
    private final AbstractNestedImmutable abstractNestedImmutableOne;

    private AbstractNestedImmutable(Builder b) {
        this.attribute2 = b.attribute2;
        this.abstractNestedImmutableOne = b.abstractNestedImmutableOne;
    }

    public String attribute2() {
        return attribute2;
    }

    public AbstractNestedImmutable abstractNestedImmutableOne() {
        return abstractNestedImmutableOne;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractNestedImmutable that = (AbstractNestedImmutable) o;

        if (!Objects.equals(attribute2, that.attribute2)) {
            return false;
        }
        return Objects.equals(abstractNestedImmutableOne, that.abstractNestedImmutableOne);
    }

    @Override
    public int hashCode() {
        int result = attribute2 != null ? attribute2.hashCode() : 0;
        result = 31 * result + (abstractNestedImmutableOne != null ? abstractNestedImmutableOne.hashCode() : 0);
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String attribute2;
        private AbstractNestedImmutable abstractNestedImmutableOne;

        public Builder attribute2(String attribute2) {
            this.attribute2 = attribute2;
            return this;
        }

        public Builder abstractNestedImmutableOne(AbstractNestedImmutable abstractNestedImmutableOne) {
            this.abstractNestedImmutableOne = abstractNestedImmutableOne;
            return this;
        }

        public AbstractNestedImmutable build() {
            return new AbstractNestedImmutable(this);
        }
    }
}