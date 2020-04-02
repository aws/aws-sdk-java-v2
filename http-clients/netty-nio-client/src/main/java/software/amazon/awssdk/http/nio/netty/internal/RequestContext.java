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

package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

@SdkInternalApi
public final class RequestContext {

    private final ChannelPool channelPool;
    private final EventLoopGroup eventLoopGroup;
    private final AsyncExecuteRequest executeRequest;
    private final NettyConfiguration configuration;

    public RequestContext(ChannelPool channelPool,
                          EventLoopGroup eventLoopGroup,
                          AsyncExecuteRequest executeRequest,
                          NettyConfiguration configuration) {
        this.channelPool = channelPool;
        this.eventLoopGroup = eventLoopGroup;
        this.executeRequest = executeRequest;
        this.configuration = configuration;
    }

    public ChannelPool channelPool() {
        return channelPool;
    }

    public EventLoopGroup eventLoopGroup() {
        return eventLoopGroup;
    }

    public AsyncExecuteRequest executeRequest() {
        return executeRequest;
    }

    /**
     * Convenience method to retrieve the {@link SdkAsyncHttpResponseHandler} contained in the {@link AsyncExecuteRequest}
     * returned by {@link #executeRequest}.
     *
     * @return The response handler for this request.
     */
    public SdkAsyncHttpResponseHandler handler() {
        return executeRequest().responseHandler();
    }

    public NettyConfiguration configuration() {
        return configuration;
    }
}
