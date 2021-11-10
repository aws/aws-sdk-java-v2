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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * A ServiceEndpointKey uniquely identifies a service endpoint, and can be used to look up endpoints via
 * {@link ServiceMetadata#endpointFor(ServiceEndpointKey)}.
 *
 * <p>An endpoint is uniquely identified by the {@link Region} of that service and the {@link EndpointTag}s associated with that
 * endpoint. For example, the {@link EndpointTag#FIPS} endpoint in {@link Region#US_WEST_2}.
 *
 * <p>This can be created via {@link #builder()}.
 */
@SdkPublicApi
@Immutable
public final class ServiceEndpointKey {
    private final Region region;
    private final Set<EndpointTag> tags;

    private ServiceEndpointKey(DefaultBuilder builder) {
        this.region = Validate.paramNotNull(builder.region, "region");
        this.tags = Collections.unmodifiableSet(new LinkedHashSet<>(Validate.paramNotNull(builder.tags, "tags")));
        Validate.noNullElements(builder.tags, "tags must not contain null.");
    }

    /**
     * Create a builder for {@link ServiceEndpointKey}s.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Retrieve the region associated with the endpoint.
     */
    public Region region() {
        return region;
    }

    /**
     * Retrieve the tags associated with the endpoint (or the empty set, to use the default endpoint).
     */
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

        ServiceEndpointKey that = (ServiceEndpointKey) o;

        if (!region.equals(that.region)) {
            return false;
        }
        return tags.equals(that.tags);
    }

    @Override
    public int hashCode() {
        int result = region.hashCode();
        result = 31 * result + tags.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("ServiceEndpointKey")
                       .add("region", region)
                       .add("tags", tags)
                       .toString();
    }

    @SdkPublicApi
    @Mutable
    public interface Builder {
        /**
         * Configure the region associated with the endpoint that should be loaded.
         */
        Builder region(Region region);

        /**
         * Configure the tags associated with the endpoint that should be loaded.
         */
        Builder tags(Collection<EndpointTag> tags);

        /**
         * Configure the tags associated with the endpoint that should be loaded.
         */
        Builder tags(EndpointTag... tags);

        /**
         * Build a {@link ServiceEndpointKey} using the configuration on this builder.
         */
        ServiceEndpointKey build();
    }

    private static class DefaultBuilder implements Builder {
        private Region region;
        private List<EndpointTag> tags = Collections.emptyList();

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        public Region getRegion() {
            return region;
        }

        public void setRegion(Region region) {
            this.region = region;
        }

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
        public ServiceEndpointKey build() {
            return new ServiceEndpointKey(this);
        }
    }
}
