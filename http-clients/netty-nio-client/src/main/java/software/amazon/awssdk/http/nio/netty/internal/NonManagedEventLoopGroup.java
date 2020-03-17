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

import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.SUCCEEDED_FUTURE;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Decorator around {@link EventLoopGroup} that prevents it from being shutdown. Used when the customer passes in a
 * custom {@link EventLoopGroup} that may be shared and thus is not managed by the SDK.
 */
@SdkInternalApi
public final class NonManagedEventLoopGroup extends DelegatingEventLoopGroup {

    public NonManagedEventLoopGroup(EventLoopGroup delegate) {
        super(delegate);
    }

    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        return SUCCEEDED_FUTURE;
    }
}
