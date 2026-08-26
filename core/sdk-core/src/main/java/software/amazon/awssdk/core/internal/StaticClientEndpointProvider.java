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
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link ClientEndpointProvider} that uses static values.
 *
 * @see ClientEndpointProvider#create(URI, boolean)
 */
@SdkInternalApi
public class StaticClientEndpointProvider implements ClientEndpointProvider {
    private final URI clientEndpoint;
    private final boolean isEndpointOverridden;

    /**
     * A sanitized form of {@link #clientEndpoint} with the query and user-info components stripped, formatted as a
     * string.  This is the value that endpoint rules receive via the {@code SDK::Endpoint} built-in.  Computed once at
     * construction so that every call to {@code endpointBuiltIn()} returns the same {@link String} reference, enabling
     * identity ({@code ==}) comparison inside the endpoint-provider cache key check.
     * <p>
     * {@code null} when {@link #isEndpointOverridden} is {@code false}.
     */
    private final String sanitizedEndpointString;

    public StaticClientEndpointProvider(URI clientEndpoint, boolean isEndpointOverridden) {
        this.clientEndpoint = Validate.paramNotNull(clientEndpoint, "clientEndpoint");
        this.isEndpointOverridden = isEndpointOverridden;
        Validate.paramNotNull(clientEndpoint.getScheme(), "The URI scheme of endpointOverride");
        this.sanitizedEndpointString = isEndpointOverridden ? sanitizeEndpoint(clientEndpoint) : null;
    }

    /**
     * Strips the query and user-info components from the given endpoint URI and returns the result as a string.
     * This matches the transformation performed by the rules engine's {@code ParseURL} function, which rejects
     * URIs with query parameters.
     * <p>
     * This is the single definition of that transformation: {@link ClientEndpointProvider#sanitizedEndpointString()}
     * delegates here so that a provider which recomputes the value per call and one which caches it at construction
     * cannot drift apart. If they drifted, the value used as an endpoint cache key would no longer be the value the
     * rules engine actually resolved against.
     */
    public static String sanitizeEndpoint(URI endpoint) {
        return FunctionalUtils.invokeSafely(
            () -> new URI(endpoint.getScheme(), null, endpoint.getHost(), endpoint.getPort(),
                          endpoint.getPath(), null, endpoint.getFragment()).toString());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the same {@link String} reference on every call, because the value is computed once at construction.
     * That lets a generated endpoint provider settle its {@code SDK::Endpoint} cache-key check with an identity
     * ({@code ==}) comparison instead of falling through to {@code equals}.
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
