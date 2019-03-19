/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.regions.util;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.internal.util.UserAgentUtils;

/**
 * <p>
 * Abstract class to return an endpoint URI from which the resources can be loaded.
 * </p>
 * <p>
 * By default, the request won't be retried if the request fails while computing endpoint.
 * </p>
 */
@SdkProtectedApi
@FunctionalInterface
public interface ResourcesEndpointProvider {
    /**
     * Returns the URI that contains the credentials.
     * @return
     *         URI to retrieve the credentials.
     *
     * @throws IOException
     *                 If any problems are encountered while connecting to the
     *                 service to retrieve the endpoint.
     */
    URI endpoint() throws IOException;

    /**
     * Allows the extending class to provide a custom retry policy.
     * The default behavior is not to retry.
     */
    default ResourcesEndpointRetryPolicy retryPolicy() {
        return ResourcesEndpointRetryPolicy.NO_RETRY;
    }

    /**
     * Allows passing additional headers to the request
     */
    default Map<String, String> headers() {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("User-Agent", UserAgentUtils.getUserAgent());
        requestHeaders.put("Accept", "*/*");
        requestHeaders.put("Connection", "keep-alive");

        return requestHeaders;
    }

}
