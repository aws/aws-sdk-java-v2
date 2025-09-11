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

import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbImmutable(builder = FakeVersionedImmutableItem.Builder.class)
public class FakeVersionedImmutableItem {
    private final String id;
    private final Long version;

    private FakeVersionedImmutableItem(Builder builder) {
        this.id = builder.id;
        this.version = builder.version;
    }

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    @DynamoDbVersionAttribute(startAt = 4, incrementBy = 5)
    public Long getVersion() {
        return version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private Long version;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder version(Long version) {
            this.version = version;
            return this;
        }

        public FakeVersionedImmutableItem build() {
            return new FakeVersionedImmutableItem(this);
        }
    }
}
