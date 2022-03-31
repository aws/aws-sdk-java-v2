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

package software.amazon.awssdk.auth.credentials.internal;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.regions.util.ResourcesEndpointProvider;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class StaticResourcesEndpointProvider implements ResourcesEndpointProvider {
    private final URI endpoint;
    private final Map<String, String> headers;

    public StaticResourcesEndpointProvider(URI endpoint,
                                           Map<String, String> additionalHeaders) {
        this.endpoint = Validate.paramNotNull(endpoint, "endpoint");
        this.headers = ResourcesEndpointProvider.super.headers();
        if (additionalHeaders != null) {
            this.headers.putAll(additionalHeaders);
        }
    }

    @Override
    public URI endpoint() throws IOException {
        return endpoint;
    }

    @Override
    public Map<String, String> headers() {
        return Collections.unmodifiableMap(headers);
    }
}
