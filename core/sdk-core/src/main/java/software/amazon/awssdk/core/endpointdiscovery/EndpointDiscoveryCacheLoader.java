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

package software.amazon.awssdk.core.endpointdiscovery;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkClientException;

@SdkProtectedApi
public interface EndpointDiscoveryCacheLoader {
    CompletableFuture<EndpointDiscoveryEndpoint> discoverEndpoint(EndpointDiscoveryRequest endpointDiscoveryRequest);

    default URI toUri(String address, URI defaultEndpoint) {
        try {
            return new URI(defaultEndpoint.getScheme(), address, defaultEndpoint.getPath(), defaultEndpoint.getFragment());
        } catch (URISyntaxException e) {
            throw SdkClientException.builder().message("Unable to construct discovered endpoint").cause(e).build();
        }
    }
}
