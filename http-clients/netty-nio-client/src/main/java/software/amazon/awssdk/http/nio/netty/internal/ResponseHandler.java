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
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.REQUEST_CONTEXT_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.RESPONSE_COMPLETE_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.RESPONSE_CONTENT_LENGTH;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.RESPONSE_DATA_READ;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.RESPONSE_STATUS_CODE;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.STREAMING_COMPLETE_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.utils.ExceptionHandlingUtils.tryCatch;
import static software.amazon.awssdk.http.nio.netty.internal.utils.ExceptionHandlingUtils.tryCatchFinally;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.ReferenceCountUtil;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkCancellationException;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.internal.http2.Http2ResetSendingSubscription;
import software.amazon.awssdk.http.nio.netty.internal.nrs.HttpStreamsClientHandler;
import software.amazon.awssdk.http.nio.netty.internal.nrs.StreamedHttpResponse;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyClientLogger;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils;
import software.amazon.awssdk.utils.FunctionalUtils.UnsafeRunnable;
import software.amazon.awssdk.utils.async.DelegatingSubscription;

@Sharable
@SdkInternalApi
public class ResponseHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final NettyClientLogger log = NettyClientLogger.getLogger(ResponseHandler.class);

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
            channelContext.channel().attr(RESPONSE_STATUS_CODE).set(response.status().code());
            channelContext.channel().attr(RESPONSE_CONTENT_LENGTH).set(responseContentLength(response));
            channelContext.channel().attr(KEEP_ALIVE).set(shouldKeepAlive(response));
            requestContext.handler().onHeaders(sdkResponse);
        }

        CompletableFuture<Void> ef = executeFuture(channelContext);
        if (msg instanceof StreamedHttpResponse) {
            requestContext.handler().onStream(
                    new DataCountingPublisher(channelContext,
                                              new PublisherAdapter((StreamedHttpResponse) msg, channelContext,
                                                                   requestContext, ef)));
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
                requestContext.handler().onStream(new DataCountingPublisher(channelContext,
                                                                            new FullResponseContentPublisher(channelContext,
                                                                                                             bb, ef)));

                try {
                    validateResponseContentLength(channelContext);
                    finalizeResponse(requestContext, channelContext);
                } catch (IOException e) {
                    exceptionCaught(channelContext, e);
                }
            } finally {
                Optional.ofNullable(fullContent).ifPresent(ByteBuf::release);
            }
        }
    }

    private Long responseContentLength(HttpResponse response) {
        String length = response.headers().get(HttpHeaderNames.CONTENT_LENGTH);
        if (length == null) {
            return null;
        }

        return Long.parseLong(length);
    }

    private static void validateResponseContentLength(ChannelHandlerContext ctx) throws IOException {
        if (!shouldValidateResponseContentLength(ctx)) {
            return;
        }

        Long contentLengthHeader = ctx.channel().attr(RESPONSE_CONTENT_LENGTH).get();
        Long actualContentLength = ctx.channel().attr(RESPONSE_DATA_READ).get();

        if (contentLengthHeader == null) {
            return;
        }

        if (actualContentLength == null) {
            actualContentLength = 0L;
        }

        if (actualContentLength.equals(contentLengthHeader)) {
            return;
        }

        throw new IOException("Response had content-length of " + contentLengthHeader + " bytes, but only received "
                              + actualContentLength + " bytes before the connection was closed.");
    }

    private static boolean shouldValidateResponseContentLength(ChannelHandlerContext ctx) {
        RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();

        // HEAD requests may return Content-Length without a payload
        if (requestContext.executeRequest().request().method() == SdkHttpMethod.HEAD) {
            return false;
        }

        // 304 responses may contain Content-Length without a payload
        Integer responseStatusCode = ctx.channel().attr(RESPONSE_STATUS_CODE).get();
        if (responseStatusCode != null && responseStatusCode == HttpResponseStatus.NOT_MODIFIED.code()) {
            return false;
        }

        return true;
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
        log.debug(ctx.channel(), () -> "Exception processing request: " + requestContext.executeRequest().request(), cause);
        Throwable throwable = NettyUtils.decorateException(ctx.channel(), cause);
        executeFuture(ctx).completeExceptionally(throwable);
        runAndLogError(ctx.channel(), () -> "Fail to execute SdkAsyncHttpResponseHandler#onError",
                       () -> requestContext.handler().onError(throwable));
        runAndLogError(ctx.channel(), () -> "Could not release channel back to the pool", () -> closeAndRelease(ctx));
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
    private static void runAndLogError(Channel ch, Supplier<String> errorMsg, UnsafeRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error(ch, errorMsg, e);
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
                    if (ChannelAttributeKey.getProtocolNow(channelContext.channel()) == Protocol.HTTP2) {
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
                        log.warn(channelContext.channel(), () -> "Subscriber cancelled before all events were published");
                        executeFuture.completeExceptionally(e);
                    } finally {
                        runAndLogError(channelContext.channel(), () -> "Could not release channel back to the pool",
                            () -> closeAndRelease(channelContext));
                    }
                }

                @Override
                public void onNext(HttpContent httpContent) {
                    // isDone may be true if the subscriber cancelled
                    if (isDone.get()) {
                        ReferenceCountUtil.release(httpContent);
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
                        runAndLogError(channelContext.channel(),
                                       () -> String.format("Subscriber %s threw an exception in onError.", subscriber),
                                       () -> subscriber.onError(t));
                        notifyError(t);
                    } finally {
                        runAndLogError(channelContext.channel(), () -> "Could not release channel back to the pool",
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
                        validateResponseContentLength(channelContext);
                        try {
                            runAndLogError(channelContext.channel(),
                                           () -> String.format("Subscriber %s threw an exception in onComplete.", subscriber),
                                           subscriber::onComplete);
                        } finally {
                            finalizeResponse(requestContext, channelContext);
                        }
                    } catch (IOException e) {
                        notifyError(e);
                    }
                }

                private void notifyError(Throwable throwable) {
                    SdkAsyncHttpResponseHandler handler = requestContext.handler();
                    runAndLogError(channelContext.channel(),
                                   () -> String.format("SdkAsyncHttpResponseHandler %s threw an exception in onError.", handler),
                                   () -> handler.onError(throwable));
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
                            if (fullContent.hasRemaining()) {
                                subscriber.onNext(fullContent);
                            }
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

    private void notifyIfResponseNotCompleted(ChannelHandlerContext handlerCtx) {
        RequestContext requestCtx = handlerCtx.channel().attr(REQUEST_CONTEXT_KEY).get();
        Boolean responseCompleted = handlerCtx.channel().attr(RESPONSE_COMPLETE_KEY).get();
        Boolean isStreamingComplete = handlerCtx.channel().attr(STREAMING_COMPLETE_KEY).get();
        handlerCtx.channel().attr(KEEP_ALIVE).set(false);

        if (!Boolean.TRUE.equals(responseCompleted) && !Boolean.TRUE.equals(isStreamingComplete)) {
            IOException err = new IOException(NettyUtils.closedChannelMessage(handlerCtx.channel()));
            runAndLogError(handlerCtx.channel(), () -> "Fail to execute SdkAsyncHttpResponseHandler#onError",
                           () -> requestCtx.handler().onError(err));
            executeFuture(handlerCtx).completeExceptionally(err);
            runAndLogError(handlerCtx.channel(), () -> "Could not release channel", () -> closeAndRelease(handlerCtx));
        }
    }

    private static final class DataCountingPublisher implements Publisher<ByteBuffer> {
        private final ChannelHandlerContext ctx;
        private final Publisher<ByteBuffer> delegate;

        private DataCountingPublisher(ChannelHandlerContext ctx, Publisher<ByteBuffer> delegate) {
            this.ctx = ctx;
            this.delegate = delegate;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            delegate.subscribe(new Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscriber.onSubscribe(subscription);
                }

                @Override
                public void onNext(ByteBuffer byteBuffer) {
                    Long responseDataSoFar = ctx.channel().attr(RESPONSE_DATA_READ).get();
                    if (responseDataSoFar == null) {
                        responseDataSoFar = 0L;
                    }

                    ctx.channel().attr(RESPONSE_DATA_READ).set(responseDataSoFar + byteBuffer.remaining());
                    subscriber.onNext(byteBuffer);
                }

                @Override
                public void onError(Throwable throwable) {
                    subscriber.onError(throwable);
                }

                @Override
                public void onComplete() {
                    subscriber.onComplete();
                }
            });
        }
    }
}