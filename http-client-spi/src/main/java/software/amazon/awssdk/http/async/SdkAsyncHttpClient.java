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

package software.amazon.awssdk.http.async;

import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.http.ConfigurationProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkRequestContext;

public interface SdkAsyncHttpClient extends AutoCloseable, ConfigurationProvider {

    /**
     * Create an {@link AbortableRunnable} that can be used to execute the HTTP request.
     *
     * @param request         HTTP request (without content).
     * @param context         Request context containing additional dependencies like metrics.
     * @param requestProvider Representation of an HTTP requestProvider.
     * @param handler         The handler that will be called when data is received.
     * @return Task that can execute an HTTP requestProvider and can be aborted.
     */
    @ReviewBeforeRelease("Should we wrap this in a container for more flexibility?")
    AbortableRunnable prepareRequest(SdkHttpRequest request,
                                     SdkRequestContext context,
                                     SdkHttpRequestProvider requestProvider,
                                     SdkHttpResponseHandler handler);

    /**
     * Each HTTP client implementation should return a well-formed client name
     * that allows requests to be identifiable back to the client that made the request.
     * The client name should include the backing implementation as well as the Sync or Async
     * to identify the transmission type of the request. Client names should only include
     * alphanumeric characters. Examples of well formed client names include, ApacheSync, for
     * requests using Apache's synchronous http client or NettyNioAsync for Netty's asynchronous
     * http client.
     *
     * @return String containing the name of the client
     */
    String clientName();
}
