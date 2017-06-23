/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.retry.internal.CredentialsEndpointRetryPolicy;

/**
 * <p>
 * Abstract class to return an endpoint URI from which the credentials can be loaded.
 * </p>
 * <p>
 * By default, the request won't be retried if the request fails while computing endpoint.
 * </p>
 */
@SdkInternalApi
public abstract class CredentialsEndpointProvider {
    /**
     * Returns the URI that contains the credentials.
     * @return
     *         URI to retrieve the credentials.
     *
     * @throws URISyntaxException
     *                 If the endpoint string could not be parsed as a URI reference.
     *
     * @throws IOException
     *                 If any problems are encountered while connecting to the
     *                 service to retrieve the endpoint.
     */
    public abstract URI getCredentialsEndpoint() throws URISyntaxException, IOException;

    /**
     * Allows the extending class to provide a custom retry policy.
     * The default behavior is not to retry.
     */
    public CredentialsEndpointRetryPolicy getRetryPolicy() {
        return CredentialsEndpointRetryPolicy.NO_RETRY;
    }
}
