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

package software.amazon.awssdk.http.apache.async;

import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * An extension of Apache's HttpAsyncClient that exposes the connection manager
 * associated with the client.
 */
@SdkInternalApi
interface ConnectionManagerAwareHttpClient extends HttpAsyncClient {
    /**
     * Returns the {@link AsyncClientConnectionManager} associated with the http client.
     */
    PoolingAsyncClientConnectionManager getAsyncClientConnectionManager();
}
