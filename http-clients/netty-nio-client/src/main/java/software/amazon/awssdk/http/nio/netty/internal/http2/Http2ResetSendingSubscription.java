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

package software.amazon.awssdk.http.nio.netty.internal.http2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.Http2Error;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.async.DelegatingSubscription;

/**
 * Wrapper around a {@link Subscription} to send a RST_STREAM frame on cancel.
 */
@SdkInternalApi
public class Http2ResetSendingSubscription extends DelegatingSubscription {

    private final ChannelHandlerContext ctx;

    public Http2ResetSendingSubscription(ChannelHandlerContext ctx, Subscription delegate) {
        super(delegate);
        this.ctx = ctx;
    }

    @Override
    public void cancel() {
        ctx.write(new DefaultHttp2ResetFrame(Http2Error.CANCEL));
        super.cancel();
    }
}
