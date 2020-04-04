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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.EXECUTE_FUTURE_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.KEEP_ALIVE;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.LAST_HTTP_CONTENT_RECEIVED_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.REQUEST_CONTEXT_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.RESPONSE_COMPLETE_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.utils.ExceptionHandlingUtils.tryCatch;
import static software.amazon.awssdk.http.nio.netty.internal.utils.ExceptionHandlingUtils.tryCatchFinally;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkCancellationException;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.internal.http2.Http2ResetSendingSubscription;
import software.amazon.awssdk.http.nio.netty.internal.nrs.HttpStreamsClientHandler;
import software.amazon.awssdk.http.nio.netty.internal.nrs.StreamedHttpResponse;
import software.amazon.awssdk.utils.FunctionalUtils.UnsafeRunnable;
import software.amazon.awssdk.utils.async.DelegatingSubscription;

@Sharable
@SdkInternalApi
public class ResponseHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger log = LoggerFactory.getLogger(ResponseHandler.class);

    private static final ResponseHandler INSTANCE = new ResponseHandler();

    private ResponseHandler() {
    }

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
            channelContext.channel().attr(KEEP_ALIVE).set(shouldKeepAlive(response));
            requestContext.handler().onHeaders(sdkResponse);
        }

        CompletableFuture<Void> ef = executeFuture(channelContext);
        if (msg instanceof StreamedHttpResponse) {
            requestContext.handler().onStream(
                    new PublisherAdapter((StreamedHttpResponse) msg, channelContext, requestContext, ef));
        } else if (msg instanceof FullHttpResponse) {
            ByteBuf fullContent = null;
            try {
                // Be prepared to take care of (ignore) a trailing LastHttpResponse
                // from the HttpClientCodec if there is one.
                channelContext.pipeline().replace(HttpStreamsClientHandler.class,
                                                  channelContext.name() + "-LastHttpContentSwallower",
                                                  LastHttpContentSwallower.getInstance());

                fullContent = ((FullHttpResponse) msg).content();
                ByteBuffer bb = copyToByteBuffer(fullContent);
                requestContext.handler().onStream(new FullResponseContentPublisher(channelContext, bb, ef));
                finalizeResponse(requestContext, channelContext);
            } finally {
                Optional.ofNullable(fullContent).ifPresent(ByteBuf::release);
            }
        }
    }

    /**
     * Finalize the response by completing the execute future and release the channel pool being used.
     *
     * @param requestContext the request context
     * @param channelContext the channel context
     */
    private static void finalizeResponse(RequestContext requestContext, ChannelHandlerContext channelContext) {
        channelContext.channel().attr(RESPONSE_COMPLETE_KEY).set(true);
        executeFuture(channelContext).complete(null);
        if (!channelContext.channel().attr(KEEP_ALIVE).get()) {
            closeAndRelease(channelContext);
        } else {
            requestContext.channelPool().release(channelContext.channel());
        }
    }

    private boolean shouldKeepAlive(HttpResponse response) {
        if (HttpStatusFamily.of(response.status().code()) == HttpStatusFamily.SERVER_ERROR) {
            return false;
        }
        return HttpUtil.isKeepAlive(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();
        log.debug("Exception processing request: {}", requestContext.executeRequest().request(), cause);
        Throwable throwable = wrapException(cause);
        executeFuture(ctx).completeExceptionally(throwable);
        runAndLogError("Fail to execute SdkAsyncHttpResponseHandler#onError", () -> requestContext.handler().onError(throwable));
        runAndLogError("Could not release channel back to the pool", () -> closeAndRelease(ctx));
    }

    @Override
    public void channelInactive(ChannelHandlerContext handlerCtx) {
        notifyIfResponseNotCompleted(handlerCtx);
    }

    public static ResponseHandler getInstance() {
        return INSTANCE;
    }

    /**
     * Close the channel and release it back into the pool.
     *
     * @param ctx Context for channel
     */
    private static void closeAndRelease(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        channel.attr(KEEP_ALIVE).set(false);
        RequestContext requestContext = channel.attr(REQUEST_CONTEXT_KEY).get();
        ctx.close();
        requestContext.channelPool().release(channel);
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

    private static CompletableFuture<Void> executeFuture(ChannelHandlerContext ctx) {
        return ctx.channel().attr(EXECUTE_FUTURE_KEY).get();
    }

    static class PublisherAdapter implements Publisher<ByteBuffer> {
        private final StreamedHttpResponse response;
        private final ChannelHandlerContext channelContext;
        private final RequestContext requestContext;
        private final CompletableFuture<Void> executeFuture;
        private final AtomicBoolean isDone = new AtomicBoolean(false);

        PublisherAdapter(StreamedHttpResponse response, ChannelHandlerContext channelContext,
                         RequestContext requestContext, CompletableFuture<Void> executeFuture) {
            this.response = response;
            this.channelContext = channelContext;
            this.requestContext = requestContext;
            this.executeFuture = executeFuture;
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
                        return subscription;
                    }
                }

                private void onCancel() {
                    if (!isDone.compareAndSet(false, true)) {
                        return;
                    }
                    try {
                        SdkCancellationException e = new SdkCancellationException(
                                "Subscriber cancelled before all events were published");
                        log.warn("Subscriber cancelled before all events were published");
                        executeFuture.completeExceptionally(e);
                    } finally {
                        runAndLogError("Could not release channel back to the pool",
                            () -> closeAndRelease(channelContext));
                    }
                }

                @Override
                public void onNext(HttpContent httpContent) {
                    // isDone may be true if the subscriber cancelled
                    if (isDone.get()) {
                        return;
                    }

                    // Needed to prevent use-after-free bug if the subscriber's onNext is asynchronous
                    ByteBuffer byteBuffer =
                        tryCatchFinally(() -> copyToByteBuffer(httpContent.content()),
                                        this::onError,
                                        httpContent::release);


                    //As per reactive-streams rule 2.13, we should not call subscriber#onError when
                    //exception is thrown from subscriber#onNext
                    if (byteBuffer != null) {
                        tryCatch(() -> subscriber.onNext(byteBuffer),
                                 this::notifyError);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    if (!isDone.compareAndSet(false, true)) {
                        return;
                    }
                    try {
                        runAndLogError(String.format("Subscriber %s threw an exception in onError.", subscriber.toString()),
                            () -> subscriber.onError(t));
                        notifyError(t);
                    } finally {
                        runAndLogError("Could not release channel back to the pool",
                            () -> closeAndRelease(channelContext));
                    }
                }

                @Override
                public void onComplete() {
                    // For HTTP/2 it's possible to get an onComplete after we cancel due to the channel becoming
                    // inactive. We guard against that here and just ignore the signal (see HandlerPublisher)
                    if (!isDone.compareAndSet(false, true)) {
                        return;
                    }
                    try {
                        runAndLogError(String.format("Subscriber %s threw an exception in onComplete.", subscriber.toString()),
                                       subscriber::onComplete);
                    } finally {
                        finalizeResponse(requestContext, channelContext);
                    }
                }

                private void notifyError(Throwable throwable) {
                    SdkAsyncHttpResponseHandler handler = requestContext.handler();
                    runAndLogError(
                        String.format("SdkAsyncHttpResponseHandler %s threw an exception in onError.", handler), () ->
                            handler.onError(throwable));
                    executeFuture.completeExceptionally(throwable);
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
        private final CompletableFuture<Void> executeFuture;
        private boolean running = true;
        private Subscriber<? super ByteBuffer> subscriber;

        FullResponseContentPublisher(ChannelHandlerContext channelContext, ByteBuffer fullContent,
                                     CompletableFuture<Void> executeFuture) {
            this.channelContext = channelContext;
            this.fullContent = fullContent;
            this.executeFuture = executeFuture;
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
                    if (running) {
                        running = false;
                        if (l <= 0) {
                            subscriber.onError(new IllegalArgumentException("Demand must be positive!"));
                        } else {
                            subscriber.onNext(fullContent);
                            subscriber.onComplete();
                            executeFuture.complete(null);
                        }
                    }
                }

                @Override
                public void cancel() {
                    running = false;
                }
            });

        }
    }

    private Throwable wrapException(Throwable originalCause) {
        if (originalCause instanceof ReadTimeoutException) {
            return new IOException("Read timed out", originalCause);
        } else if (originalCause instanceof WriteTimeoutException) {
            return new IOException("Write timed out", originalCause);
        }

        return originalCause;
    }

    private void notifyIfResponseNotCompleted(ChannelHandlerContext handlerCtx) {
        RequestContext requestCtx = handlerCtx.channel().attr(REQUEST_CONTEXT_KEY).get();
        Boolean responseCompleted = handlerCtx.channel().attr(RESPONSE_COMPLETE_KEY).get();
        Boolean lastHttpContentReceived = handlerCtx.channel().attr(LAST_HTTP_CONTENT_RECEIVED_KEY).get();
        handlerCtx.channel().attr(KEEP_ALIVE).set(false);

        if (!Boolean.TRUE.equals(responseCompleted) && !Boolean.TRUE.equals(lastHttpContentReceived)) {
            IOException err = new IOException("Server failed to send complete response");
            runAndLogError("Fail to execute SdkAsyncHttpResponseHandler#onError", () -> requestCtx.handler().onError(err));
            executeFuture(handlerCtx).completeExceptionally(err);
            runAndLogError("Could not release channel", () -> closeAndRelease(handlerCtx));
        }
    }
}