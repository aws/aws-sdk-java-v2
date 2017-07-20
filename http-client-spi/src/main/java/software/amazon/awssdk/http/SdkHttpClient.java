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

package software.amazon.awssdk.http;

/**
 * Generic interface to take a representation of an HTTP request, make the HTTP call, and return a representation of an
 * HTTP response.
 *
 * <p>Implementations MUST be thread safe.</p>
 */
public interface SdkHttpClient extends AutoCloseable, ConfigurationProvider {

    /**
     * Create a {@link AbortableCallable} that can be used to execute the HTTP request.
     *
     * @param request        Representation of an HTTP request.
     * @param requestContext Contains any extra dependencies needed like metrics object.
     * @return Task that can execute an HTTP request and can be aborted.
     */
    AbortableCallable<SdkHttpFullResponse> prepareRequest(SdkHttpFullRequest request, SdkRequestContext requestContext);

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
