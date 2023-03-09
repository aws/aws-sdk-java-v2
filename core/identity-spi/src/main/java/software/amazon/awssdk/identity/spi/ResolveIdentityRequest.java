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

package software.amazon.awssdk.identity.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * A request to resolve an {@link Identity}.
 *
 * The Identity may be determined for each request based on properties of the request (e.g. different credentials per bucket
 * for S3).
 *
 * @see IdentityProvider
 */
@SdkProtectedApi
@Immutable
@ThreadSafe
public final class ResolveIdentityRequest {

    private final Map<IdentityProperty<?>, ?> properties;

    private ResolveIdentityRequest(Map<IdentityProperty<?>, Object> properties) {
        this.properties = new HashMap<>(properties);
    }

    public static Builder builder() {
        return new Builder();
    }

    public <T> T property(IdentityProperty<T> property) {
        return (T) properties.get(property);
    }

    @Override
    public String toString() {
        return ToString.builder("ResolveIdentityRequest")
                       .add("properties", properties)
                       .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResolveIdentityRequest that = (ResolveIdentityRequest) o;
        return properties.equals(that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(properties);
    }

    public static final class Builder implements SdkBuilder<Builder, ResolveIdentityRequest> {
        private final Map<IdentityProperty<?>, Object> properties = new HashMap<>();

        private Builder() {
        }

        public <T> Builder putProperty(IdentityProperty<T> key, T value) {
            this.properties.put(key, value);
            return this;
        }

        public ResolveIdentityRequest build() {
            return new ResolveIdentityRequest(properties);
        }
    }
}
