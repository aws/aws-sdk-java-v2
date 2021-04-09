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

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPreserveEmptyObject;

@DynamoDbImmutable(builder = NestedImmutable.Builder.class)
public class NestedImmutable {
    private final String id;
    private final Integer integerAttribute;
    private final AbstractImmutable innerBean;

    private NestedImmutable(Builder b) {
        this.id = b.id;
        this.integerAttribute = b.integerAttribute;
        this.innerBean = b.innerBean;
    }

    @DynamoDbPartitionKey
    public String id() {
        return this.id;
    }

    public Integer integerAttribute() {
        return integerAttribute;
    }

    @DynamoDbPreserveEmptyObject
    public AbstractImmutable innerBean() {
        return innerBean;
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

        NestedImmutable that = (NestedImmutable) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (integerAttribute != null ? !integerAttribute.equals(that.integerAttribute) : that.integerAttribute != null) {
            return false;
        }
        return innerBean != null ? innerBean.equals(that.innerBean) : that.innerBean == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (integerAttribute != null ? integerAttribute.hashCode() : 0);
        result = 31 * result + (innerBean != null ? innerBean.hashCode() : 0);
        return result;
    }

    public static final class Builder {
        private AbstractImmutable innerBean;
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

        public Builder innerBean(AbstractImmutable innerBean) {
            this.innerBean = innerBean;
            return this;
        }

        public NestedImmutable build() {
            return new NestedImmutable(this);
        }
    }
}
