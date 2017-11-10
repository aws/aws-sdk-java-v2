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

package software.amazon.awssdk.http.nio.netty.h2;

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.REQUEST_CONTEXT_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.RESPONSE_COMPLETE_KEY;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.concurrent.Future;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.nio.netty.internal.RequestContext;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.FunctionalUtils.UnsafeRunnable;

public final class H2RunnableRequest implements AbortableRunnable {

    private static final Logger log = LoggerFactory.getLogger(H2RunnableRequest.class);
    private final RequestContext context;
    private volatile Channel channel;

    public H2RunnableRequest(RequestContext context) {
        this.context = context;
    }

    @Override
    public void run() {
        context.channelPool().acquire().addListener((Future<Channel> channelFuture) -> {
            if (channelFuture.isSuccess()) {
                try {
                    channel = channelFuture.getNow();
                    channel.attr(REQUEST_CONTEXT_KEY).set(context);
                    channel.attr(RESPONSE_COMPLETE_KEY).set(false);
                    makeRequest(context.nettyRequest());
                } catch (Exception e) {
                    handleFailure(() -> "Failed to make request to " + endpoint(), e);
                }
            } else {
                handleFailure(() -> "Failed to create connection to " + endpoint(), channelFuture.cause());
            }
        });
    }

    @Override
    public void abort() {
        if (channel != null) {
            closeAndRelease(channel);
        }
    }

    private void makeRequest(HttpRequest request) {
        log.debug("Writing request: {}", request);
        DefaultFullHttpRequest fullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, request.method(), request.uri(),
                                                                            dumpToBytes(context.sdkRequestProvider()), request.headers(), new DefaultHttpHeaders());
        channel.writeAndFlush(fullHttpRequest)
               .addListener(wireCall -> {
                   if (!wireCall.isSuccess()) {
                       handleFailure(() -> "Failed to make request to " + endpoint(), wireCall.cause());
                   }
               });
    }

    private URI endpoint() {
        return context.sdkRequest().getUri();
    }

    private void handleFailure(Supplier<String> msg, Throwable cause) {
        log.error(msg.get(), cause);
        runAndLogError("Exception thrown from AsyncResponseHandler",
                       () -> context.handler().exceptionOccurred(cause));
        if (channel != null) {
            runAndLogError("Unable to release channel back to the pool.",
                           () -> closeAndRelease(channel));
        }
    }

    private static void closeAndRelease(Channel channel) {
        RequestContext requestCtx = channel.attr(REQUEST_CONTEXT_KEY).get();
        channel.close().addListener(ignored -> requestCtx.channelPool().release(channel));
    }

    /**
     * Runs a given {@link UnsafeRunnable} and logs an error without throwing.
     *
     * @param errorMsg Message to log with exception thrown.
     * @param runnable Action to perform.
     */
    private static void runAndLogError(String errorMsg, UnsafeRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error(errorMsg, e);
        }
    }

    public static ByteBuf dumpToBytes(Publisher<ByteBuffer> publisher) {
        CountDownLatch latch = new CountDownLatch(1);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        publisher.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                invokeSafely(() -> baos.write(BinaryUtils.copyBytesFrom(byteBuffer)));
            }

            @Override
            public void onError(Throwable t) {
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        invokeSafely((UnsafeRunnable) latch::await);
        byte[] bytes = baos.toByteArray();
        return Unpooled.buffer(bytes.length).writeBytes(bytes);
    }

}
