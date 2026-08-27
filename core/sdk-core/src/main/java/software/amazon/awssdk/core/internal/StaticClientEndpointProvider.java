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

package software.amazon.awssdk.core.internal;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link ClientEndpointProvider} that uses static values.
 *
 * @see ClientEndpointProvider#create(URI, boolean)
 */
@SdkInternalApi
public final class StaticClientEndpointProvider implements ClientEndpointProvider {
    private final URI clientEndpoint;
    private final boolean isEndpointOverridden;

    /**
     * A sanitized form of {@link #clientEndpoint} with the query and user-info components stripped, formatted as a
     * string.  This is the value that endpoint rules receive via the {@code SDK::Endpoint} built-in.  Computed once at
     * construction so that every call to {@code endpointBuiltIn()} returns the same {@link String} reference.
     * <p>
     * {@code null} when {@link #isEndpointOverridden} is {@code false}.
     */
    private final String sanitizedEndpointString;

    public StaticClientEndpointProvider(URI clientEndpoint, boolean isEndpointOverridden) {
        this.clientEndpoint = Validate.paramNotNull(clientEndpoint, "clientEndpoint");
        this.isEndpointOverridden = isEndpointOverridden;
        Validate.paramNotNull(clientEndpoint.getScheme(), "The URI scheme of endpointOverride");
        // Calls the interface's implementation rather than repeating the transformation here, so the cached value cannot
        // drift from what a provider that does not override the method produces. Safe from a constructor: this is a
        // non-virtual call, the two accessors it reads are assigned above, and the class is final so neither can be
        // overridden to observe partial construction.
        this.sanitizedEndpointString = ClientEndpointProvider.super.sanitizedEndpointString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the same {@link String} reference on every call, because the value is computed once at construction.
     * Avoids additional allocations and expensive URI creation per request.
     */
    @Override
    public String sanitizedEndpointString() {
        return sanitizedEndpointString;
    }

    @Override
    public URI clientEndpoint() {
        return this.clientEndpoint;
    }

    @Override
    public boolean isEndpointOverridden() {
        return this.isEndpointOverridden;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StaticClientEndpointProvider that = (StaticClientEndpointProvider) o;

        if (isEndpointOverridden != that.isEndpointOverridden) {
            return false;
        }
        return clientEndpoint.equals(that.clientEndpoint);
    }

    @Override
    public int hashCode() {
        int result = clientEndpoint.hashCode();
        result = 31 * result + (isEndpointOverridden ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("ClientEndpointProvider")
                       .add("clientEndpoint", clientEndpoint)
                       .add("isEndpointOverridden", isEndpointOverridden)
                       .build();
    }
}
