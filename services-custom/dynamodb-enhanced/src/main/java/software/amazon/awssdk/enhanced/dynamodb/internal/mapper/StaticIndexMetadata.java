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

package software.amazon.awssdk.enhanced.dynamodb.internal.mapper;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.IndexMetadata;
import software.amazon.awssdk.enhanced.dynamodb.KeyAttributeMetadata;

@SdkInternalApi
public class StaticIndexMetadata implements IndexMetadata {
    private final String name;
    private final KeyAttributeMetadata partitionKey;
    private final KeyAttributeMetadata sortKey;

    private StaticIndexMetadata(Builder b) {
        this.name = b.name;
        this.partitionKey = b.partitionKey;
        this.sortKey = b.sortKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builderFrom(IndexMetadata index) {
        return index == null ? builder() : builder().name(index.name())
                                                    .partitionKey(index.partitionKey().orElse(null))
                                                    .sortKey(index.sortKey().orElse(null));
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Optional<KeyAttributeMetadata> partitionKey() {
        return Optional.ofNullable(this.partitionKey);
    }

    @Override
    public Optional<KeyAttributeMetadata> sortKey() {
        return Optional.ofNullable(this.sortKey);
    }

    public static class Builder {
        private String name;
        private KeyAttributeMetadata partitionKey;
        private KeyAttributeMetadata sortKey;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder partitionKey(KeyAttributeMetadata partitionKey) {
            this.partitionKey = partitionKey;
            return this;
        }

        public Builder sortKey(KeyAttributeMetadata sortKey) {
            this.sortKey = sortKey;
            return this;
        }

        public StaticIndexMetadata build() {
            return new StaticIndexMetadata(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StaticIndexMetadata that = (StaticIndexMetadata) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (partitionKey != null ? !partitionKey.equals(that.partitionKey) : that.partitionKey != null) {
            return false;
        }
        return sortKey != null ? sortKey.equals(that.sortKey) : that.sortKey == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (partitionKey != null ? partitionKey.hashCode() : 0);
        result = 31 * result + (sortKey != null ? sortKey.hashCode() : 0);
        return result;
    }
}
