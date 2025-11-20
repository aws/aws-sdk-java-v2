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

import static java.util.Comparator.comparingInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.IndexMetadata;
import software.amazon.awssdk.enhanced.dynamodb.KeyAttributeMetadata;

@SdkInternalApi
public class StaticIndexMetadata implements IndexMetadata {
    private final String name;
    private final List<KeyAttributeMetadata> partitionKeys;
    private final List<KeyAttributeMetadata> sortKeys;

    private StaticIndexMetadata(Builder b) {
        this.name = b.name;
        this.partitionKeys = Collections.unmodifiableList(
            b.partitionKeys.stream()
                           .sorted(comparingInt(key -> key.order().getIndex()))
                           .collect(Collectors.toList())
        );
        this.sortKeys = Collections.unmodifiableList(
            b.sortKeys.stream()
                      .sorted(comparingInt(key -> key.order().getIndex()))
                      .collect(Collectors.toList())
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builderFrom(IndexMetadata index) {
        if (index == null) {
            return builder();
        }
        return builder().name(index.name())
                        .partitionKeys(index.partitionKeys())
                        .sortKeys(index.sortKeys());
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public List<KeyAttributeMetadata> partitionKeys() {
        return this.partitionKeys;
    }

    @Override
    public List<KeyAttributeMetadata> sortKeys() {
        return this.sortKeys;
    }

    @NotThreadSafe
    public static class Builder {
        private String name;
        private List<KeyAttributeMetadata> partitionKeys = new ArrayList<>();
        private List<KeyAttributeMetadata> sortKeys = new ArrayList<>();

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder partitionKeys(List<KeyAttributeMetadata> partitionKeys) {
            this.partitionKeys = new ArrayList<>(partitionKeys);
            return this;
        }

        public Builder sortKeys(List<KeyAttributeMetadata> sortKeys) {
            this.sortKeys = new ArrayList<>(sortKeys);
            return this;
        }

        public Builder addPartitionKey(KeyAttributeMetadata partitionKey) {
            this.partitionKeys.add(partitionKey);
            return this;
        }

        public Builder addSortKey(KeyAttributeMetadata sortKey) {
            this.sortKeys.add(sortKey);
            return this;
        }

        public List<KeyAttributeMetadata> getPartitionKeys() {
            return new ArrayList<>(this.partitionKeys);
        }

        public List<KeyAttributeMetadata> getSortKeys() {
            return new ArrayList<>(this.sortKeys);
        }

        // Backward compatibility methods
        public Builder partitionKey(KeyAttributeMetadata partitionKey) {
            this.partitionKeys = new ArrayList<>();
            if (partitionKey != null) {
                this.partitionKeys.add(partitionKey);
            }
            return this;
        }

        public Builder sortKey(KeyAttributeMetadata sortKey) {
            this.sortKeys = new ArrayList<>();
            if (sortKey != null) {
                this.sortKeys.add(sortKey);
            }
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
        if (!partitionKeys.equals(that.partitionKeys)) {
            return false;
        }
        return sortKeys.equals(that.sortKeys);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = listHashCode(partitionKeys, result);
        result = listHashCode(sortKeys, result);
        return result;
    }

    private static int listHashCode(List<KeyAttributeMetadata> list, int hash) {
        int result = hash;
        for (KeyAttributeMetadata key : list) {
            result = 31 * result + key.hashCode();
        }
        return result;
    }
}
