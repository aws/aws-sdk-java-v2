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
     * Returns true if this endpoint was specified as an override by the customer, or false if it was determined
     * automatically by the SDK.
     */
    boolean isEndpointOverridden();
}
