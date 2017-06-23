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

package software.amazon.awssdk.http.nio.netty.internal;

import com.typesafe.netty.HandlerSubscriber;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.EventExecutor;
import java.nio.ByteBuffer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;

/**
 * Subscriber that writes events directly to the Netty {@link Channel}.
 */
public class NettyHttpContentSubscriber extends HandlerSubscriber<HttpContent> {
    private final Channel channel;

    public NettyHttpContentSubscriber(Channel channel) {
        super(channel.eventLoop());
        this.channel = channel;
    }

    @Override
    protected void complete() {
        EventExecutor executor = channel.eventLoop();
        executor.execute(() -> channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
                .addListener((ChannelFutureListener) future -> removeFromPipeline()));
    }

    @Override
    protected void error(Throwable error) {
        removeFromPipeline();
    }

    private void removeFromPipeline() {
        channel.pipeline().remove(this);
    }

    /**
     * Adapt this subscriber to one that subcribes to {@link ByteBuffer} events. Used to subscribe to
     * {@link software.amazon.awssdk.http.async.SdkHttpRequestProvider} publisher.
     *
     * @return Adapted Subscriber.
     */
    public Subscriber<ByteBuffer> adapt() {
        return new SubscriberAdapter(this);
    }

    /**
     * Adapts {@link HttpContent} subscriber to a {@link ByteBuffer} one.
     */
    private class SubscriberAdapter implements Subscriber<ByteBuffer> {

        private final Subscriber<HttpContent> subscriber;

        private SubscriberAdapter(Subscriber<HttpContent> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscriber.onSubscribe(s);
        }

        @ReviewBeforeRelease("Unpooled vs channel.alloc? channel.alloc seems to be preferred but we should test" +
                             " this more thoroughly under high concurrency.")
        @Override
        public void onNext(ByteBuffer data) {
            final ByteBuf buffer = channel.alloc().buffer(data.limit());
            buffer.writeBytes(data);
            HttpContent content = new DefaultHttpContent(buffer);
            subscriber.onNext(content);
        }

        @Override
        public void onError(Throwable t) {
            subscriber.onError(t);
        }

        @Override
        public void onComplete() {
            subscriber.onComplete();
        }
    }
}
