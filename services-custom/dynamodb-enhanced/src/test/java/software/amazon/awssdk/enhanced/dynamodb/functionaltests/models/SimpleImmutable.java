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

import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbImmutable(builder = SimpleImmutable.Builder.class)
public class SimpleImmutable {

    private final String id;

    private final String sort;

    private final String stringAttribute;

    private SimpleImmutable(Builder builder) {
        id = builder.id;
        sort = builder.sort;
        stringAttribute = builder.stringAttribute;
    }

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    @DynamoDbSortKey
    public String getSort() {
        return sort;
    }

    public String getStringAttribute() {
        return stringAttribute;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SimpleImmutable that = (SimpleImmutable) o;
        return Objects.equals(id, that.id) && Objects.equals(sort, that.sort) && Objects.equals(stringAttribute, that.stringAttribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sort, stringAttribute);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String sort;
        private String stringAttribute;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder sort(String sort) {
            this.sort = sort;
            return this;
        }

        public Builder stringAttribute(String stringAttribute) {
            this.stringAttribute = stringAttribute;
            return this;
        }

        public SimpleImmutable build() {
            return new SimpleImmutable(this);
        }
    }
}
