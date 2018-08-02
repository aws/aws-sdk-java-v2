/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.REQUEST_CONTEXT_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.RESPONSE_COMPLETE_KEY;

import com.typesafe.netty.http.HttpStreamsClientHandler;
import com.typesafe.netty.http.StreamedHttpResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.nio.netty.internal.http2.Http2ResetSendingSubscription;
import software.amazon.awssdk.utils.FunctionalUtils.UnsafeRunnable;
import software.amazon.awssdk.utils.async.DelegatingSubscription;

@Sharable
@SdkInternalApi
public class ResponseHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger log = LoggerFactory.getLogger(ResponseHandler.class);

    /**
     * {@link AttributeKey} to keep track of whether we should close the connection after this request
     * has completed.
     */
    private static final AttributeKey<Boolean> KEEP_ALIVE = AttributeKey.newInstance("KeepAlive");

    @Override
    protected void channelRead0(ChannelHandlerContext channelContext, HttpObject msg) throws Exception {
        RequestContext requestContext = channelContext.channel().attr(REQUEST_CONTEXT_KEY).get();

        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            SdkHttpResponse sdkResponse = SdkHttpFullResponse.builder()
                                                             .headers(fromNettyHeaders(response.headers()))
                                                             .statusCode(response.status().code())
                                                             .statusText(response.status().reasonPhrase())
                                                             .build();
            channelContext.channel().attr(KEEP_ALIVE).set(HttpUtil.isKeepAlive(response));
            requestContext.handler().headersReceived(sdkResponse);
        }

        if (msg instanceof StreamedHttpResponse) {
            requestContext.handler().onStream(new PublisherAdapter((StreamedHttpResponse) msg, channelContext, requestContext));
        } else if (msg instanceof FullHttpResponse) {
            // Be prepared to take care of (ignore) a trailing LastHttpResponse
            // from the HttpClientCodec if there is one.
            channelContext.pipeline().replace(HttpStreamsClientHandler.class,
                    channelContext.name() + "-LastHttpContentSwallower", new LastHttpContentSwallower());

            ByteBuf fullContent = ((FullHttpResponse) msg).content();
            final ByteBuffer bb = copyToByteBuffer(fullContent);
            fullContent.release();
            requestContext.handler().onStream(new FullResponseContentPublisher(channelContext, bb));
            Subscriber<? super ByteBuffer> subscriber = channelContext.channel().attr(ChannelAttributeKey.SUBSCRIBER_KEY).get();
            try {
                subscriber.onComplete();
                requestContext.handler().complete();
            } catch (RuntimeException e) {
                subscriber.onError(e);
                requestContext.handler().exceptionOccurred(e);
                throw e;
            } finally {
                finalizeRequest(requestContext, channelContext);
            }
        }
    }

    private static void finalizeRequest(RequestContext requestContext, ChannelHandlerContext channelContext) {
        channelContext.channel().attr(RESPONSE_COMPLETE_KEY).set(true);
        if (!channelContext.channel().attr(KEEP_ALIVE).get()) {
            closeAndRelease(channelContext);
        } else {
            requestContext.channelPool().release(channelContext.channel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();
        log.error("Exception processing request: {}", requestContext.sdkRequest(), cause);
        runAndLogError("SdkHttpResponseHandler threw an exception",
            () -> requestContext.handler().exceptionOccurred(cause));
        runAndLogError("Could not release channel back to the pool", () -> closeAndRelease(ctx));
    }

    @Override
    public void channelInactive(ChannelHandlerContext handlerCtx) throws Exception {
        RequestContext requestCtx = handlerCtx.channel().attr(REQUEST_CONTEXT_KEY).get();
        boolean responseCompleted = handlerCtx.channel().attr(RESPONSE_COMPLETE_KEY).get();
        if (!responseCompleted) {
            runAndLogError("SdkHttpResponseHandler threw an exception when calling exceptionOccurred",
                () -> requestCtx.handler().exceptionOccurred(new IOException("Server failed to send complete response")));
            runAndLogError("Could not release channel",
                () -> requestCtx.channelPool().release(handlerCtx.channel()));
        }
    }

    /**
     * Close the channel and release it back into the pool.
     *
     * @param ctx Context for channel
     */
    private static void closeAndRelease(ChannelHandlerContext ctx) {
        RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();
        ctx.channel().close();
        requestContext.channelPool().release(ctx.channel());
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

    private static Map<String, List<String>> fromNettyHeaders(HttpHeaders headers) {
        return headers.entries().stream()
                .collect(groupingBy(Map.Entry::getKey,
                        mapping(Map.Entry::getValue, Collectors.toList())));
    }

    private static ByteBuffer copyToByteBuffer(ByteBuf byteBuf) {
        ByteBuffer bb = ByteBuffer.allocate(byteBuf.readableBytes());
        byteBuf.getBytes(byteBuf.readerIndex(), bb);
        bb.flip();
        return bb;
    }

    private static class PublisherAdapter implements Publisher<ByteBuffer> {
        private final StreamedHttpResponse response;
        private final ChannelHandlerContext channelContext;
        private final RequestContext requestContext;
        private final AtomicBoolean isCancelled = new AtomicBoolean(false);

        private PublisherAdapter(StreamedHttpResponse response, ChannelHandlerContext channelContext,
                                 RequestContext requestContext) {
            this.response = response;
            this.channelContext = channelContext;
            this.requestContext = requestContext;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            response.subscribe(new Subscriber<HttpContent>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscriber.onSubscribe(new OnCancelSubscription(resolveSubscription(subscription),
                                                                    this::onCancel));
                }

                private Subscription resolveSubscription(Subscription subscription) {
                    // For HTTP2 we send a RST_STREAM frame on cancel to stop the service from sending more data
                    if (Protocol.HTTP2.equals(ChannelAttributeKey.getProtocolNow(channelContext.channel()))) {
                        return new Http2ResetSendingSubscription(channelContext, subscription);
                    } else {
                        // TODO I believe the behavior for H1 is to finish reading the data. Do we want to do this
                        // or abort the connection?
                        return subscription;
                    }
                }

                private void onCancel() {
                    try {
                        isCancelled.set(true);
                        requestContext.handler().exceptionOccurred(
                            new RuntimeException("Subscriber cancelled before all events were published"));
                    } finally {
                        runAndLogError("Could not release channel back to the pool",
                            () -> closeAndRelease(channelContext));
                        runAndLogError("Could not release channel back to the pool",
                            () -> closeAndRelease(channelContext));
                    }
                }

                @Override
                public void onNext(HttpContent httpContent) {
                    // Needed to prevent use-after-free bug if the subscriber's onNext is asynchronous
                    ByteBuffer b = copyToByteBuffer(httpContent.content());
                    httpContent.release();
                    subscriber.onNext(b);
                    channelContext.read();
                }

                @Override
                public void onError(Throwable t) {
                    if (isCancelled.get()) {
                        return;
                    }
                    try {
                        runAndLogError(String.format("Subscriber %s threw an exception in onError.", subscriber.toString()),
                            () -> subscriber.onError(t));
                        requestContext.handler().exceptionOccurred(t);
                    } finally {
                        runAndLogError("Could not release channel back to the pool",
                            () -> closeAndRelease(channelContext));
                    }
                }

                @Override
                public void onComplete() {
                    // For HTTP/2 it's possible to get an onComplete after we cancel due to the channel becoming
                    // inactive. We guard against that here and just ignore the signal (see HandlerPublisher)
                    if (isCancelled.get()) {
                        return;
                    }
                    try {
                        runAndLogError(String.format("Subscriber %s threw an exception in onComplete.", subscriber.toString()),
                                       subscriber::onComplete);
                        requestContext.handler().complete();
                    } finally {
                        finalizeRequest(requestContext, channelContext);
                    }
                }
            });
        }
    }

    /**
     * Decorator around a {@link Subscription} to notify if a cancellation occurs.
     */
    private static class OnCancelSubscription extends DelegatingSubscription {

        private final Runnable onCancel;

        private OnCancelSubscription(Subscription subscription, Runnable onCancel) {
            super(subscription);
            this.onCancel = onCancel;
        }

        @Override
        public void cancel() {
            onCancel.run();
            super.cancel();
        }
    }

    static class FullResponseContentPublisher implements Publisher<ByteBuffer> {
        private final ChannelHandlerContext channelContext;
        private final ByteBuffer fullContent;
        private boolean running = true;
        private Subscriber<? super ByteBuffer> subscriber;

        FullResponseContentPublisher(ChannelHandlerContext channelContext, ByteBuffer fullContent) {
            this.channelContext = channelContext;
            this.fullContent = fullContent;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            if (this.subscriber != null) {
                subscriber.onComplete();
                return;
            }
            this.subscriber = subscriber;
            channelContext.channel().attr(ChannelAttributeKey.SUBSCRIBER_KEY)
                    .set(subscriber);

            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long l) {
                    if (l <= 0 && running) {
                        running = false;
                        subscriber.onError(new IllegalArgumentException("Demand must be positive!"));
                    } else if (running) {
                        running = false;
                        subscriber.onNext(fullContent);
                        subscriber.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    running = false;
                }
            });

        }
    }

    private static class LastHttpContentSwallower extends SimpleChannelInboundHandler<HttpObject> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, HttpObject obj) throws Exception {
            if (obj instanceof LastHttpContent) {
                // Queue another read to make up for the one we just ignored
                ctx.read();
            } else {
                ctx.fireChannelRead(obj);
            }
            // Remove self from pipeline since we only care about potentially
            // ignoring the very first message
            ctx.pipeline().remove(this);
        }
    }
}
