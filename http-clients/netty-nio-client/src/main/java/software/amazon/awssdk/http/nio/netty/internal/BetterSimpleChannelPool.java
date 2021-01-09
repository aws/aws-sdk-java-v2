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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.SimpleChannelPool;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Extension of {@link SimpleChannelPool} to add an asynchronous close method
 */
@SdkInternalApi
public final class BetterSimpleChannelPool extends SimpleChannelPool {
    private final CompletableFuture<Boolean> closeFuture;

    BetterSimpleChannelPool(Bootstrap bootstrap, ChannelPoolHandler handler) {
        super(bootstrap, handler);
        closeFuture = new CompletableFuture<>();
    }

    @Override
    public void close() {
        super.close();
        closeFuture.complete(true);
    }

    CompletableFuture<Boolean> closeFuture() {
        return closeFuture;
    }
}