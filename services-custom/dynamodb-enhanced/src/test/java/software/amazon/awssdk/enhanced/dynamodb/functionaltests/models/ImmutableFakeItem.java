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

@DynamoDbImmutable(builder = ImmutableFakeItem.Builder.class)
public class ImmutableFakeItem {
    private final String id;
    private final String attribute;

    private ImmutableFakeItem(Builder b) {
        this.id = b.id;
        this.attribute = b.attribute;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String attribute() {
        return attribute;
    }

    @DynamoDbPartitionKey
    public String id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ImmutableFakeItem that = (ImmutableFakeItem) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        return attribute != null ? attribute.equals(that.attribute) : that.attribute == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        return result;
    }

    public static final class Builder {
        private String id;
        private String attribute;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder attribute(String attribute) {
            this.attribute = attribute;
            return this;
        }

        public ImmutableFakeItem build() {
            return new ImmutableFakeItem(this);
        }
    }
}
