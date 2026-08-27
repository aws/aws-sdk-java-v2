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

package software.amazon.awssdk.core;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.internal.StaticClientEndpointProvider;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.utils.FunctionalUtils;

/**
 * Client endpoint providers are responsible for resolving client-level endpoints. {@link EndpointProvider}s are
 * ultimately responsible for resolving the endpoint used for a request.
 * <p>
 * {@link EndpointProvider}s may choose to honor or completely ignore the client-level endpoint. Default endpoint
 * providers will ignore the client-level endpoint, unless {@link #isEndpointOverridden()} is true.
 */
@SdkProtectedApi
public interface ClientEndpointProvider {
    /**
     * Create a client endpoint provider that uses the provided URI and returns true from {@link #isEndpointOverridden()}.
     */
    static ClientEndpointProvider forEndpointOverride(URI uri) {
        return new StaticClientEndpointProvider(uri, true);
    }

    /**
     * Create a client endpoint provider that uses the provided static URI and override settings.
     */
    static ClientEndpointProvider create(URI uri, boolean isEndpointOverridden) {
        return new StaticClientEndpointProvider(uri, isEndpointOverridden);
    }

    /**
     * Retrieve the client endpoint from this provider.
     */
    URI clientEndpoint();

    /**
     * Returns the endpoint string to pass to the endpoint rules engine as the {@code SDK::Endpoint} built-in, with the
     * query and user-info components stripped because the rules engine's {@code ParseURL} rejects a URI carrying query
     * parameters.
     * <p>
     * This is the single definition of that transformation. {@link #create(URI, boolean)} returns an implementation that
     * calls it once and caches the result, which removes a URI construction and its string conversion from every
     * request; an implementation that does not override it recomputes per call. Both must produce the same string,
     * because it is what the rules engine resolves against and what a generated endpoint provider uses as part of its
     * cache key.
     * <p>
     * Returns {@code null} if the endpoint is not overridden.
     */
    default String sanitizedEndpointString() {
        if (!isEndpointOverridden()) {
            return null;
        }
        URI endpoint = clientEndpoint();
        return FunctionalUtils.invokeSafely(
            () -> new URI(endpoint.getScheme(), null, endpoint.getHost(), endpoint.getPort(),
                          endpoint.getPath(), null, endpoint.getFragment()).toString());
    }

    /**
     * Returns true if this endpoint was specified as an override by the customer, or false if it was determined
     * automatically by the SDK.
     */
    boolean isEndpointOverridden();
}
