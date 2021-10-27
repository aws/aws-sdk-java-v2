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

package software.amazon.awssdk.regions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * A key used to look up a specific partition hostname or DNS suffix via
 * {@link PartitionMetadata#hostname(PartitionEndpointKey)} or {@link PartitionMetadata#dnsSuffix(PartitionEndpointKey)}.
 */
@SdkPublicApi
@Immutable
public final class PartitionEndpointKey {
    private final Set<EndpointTag> tags;

    private PartitionEndpointKey(DefaultBuilder builder) {
        this.tags = Collections.unmodifiableSet(new HashSet<>(Validate.paramNotNull(builder.tags, "tags")));
        Validate.noNullElements(builder.tags, "tags must not contain null.");
    }

    /**
     * Create a builder for a {@link PartitionEndpointKey}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    public Set<EndpointTag> tags() {
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PartitionEndpointKey that = (PartitionEndpointKey) o;

        return tags.equals(that.tags);
    }

    @Override
    public int hashCode() {
        return tags.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("PartitionEndpointKey")
                       .add("tags", tags)
                       .toString();
    }

    @SdkPublicApi
    @Mutable
    public interface Builder {
        /**
         * Configure the tags associated with the partition endpoint that should be retrieved.
         */
        Builder tags(Collection<EndpointTag> tags);

        /**
         * Configure the tags associated with the partition endpoint that should be retrieved.
         */
        Builder tags(EndpointTag... tags);

        /**
         * Create a {@link PartitionEndpointKey} from the configuration on this builder.
         */
        PartitionEndpointKey build();
    }

    private static class DefaultBuilder implements Builder {
        private List<EndpointTag> tags = Collections.emptyList();

        @Override
        public Builder tags(Collection<EndpointTag> tags) {
            this.tags = new ArrayList<>(tags);
            return this;
        }

        @Override
        public Builder tags(EndpointTag... tags) {
            this.tags = Arrays.asList(tags);
            return this;
        }

        public List<EndpointTag> getTags() {
            return Collections.unmodifiableList(tags);
        }

        public void setTags(Collection<EndpointTag> tags) {
            this.tags = new ArrayList<>(tags);
        }

        @Override
        public PartitionEndpointKey build() {
            return new PartitionEndpointKey(this);
        }
    }
}
