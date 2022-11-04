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

import static software.amazon.awssdk.http.HttpMetric.CONCURRENCY_ACQUIRE_DURATION;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.CHANNEL_DIAGNOSTICS;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.EXECUTE_FUTURE_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.EXECUTION_ID_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.IN_USE;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.KEEP_ALIVE;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.REQUEST_CONTEXT_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.RESPONSE_COMPLETE_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.RESPONSE_CONTENT_LENGTH;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.RESPONSE_DATA_READ;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.STREAMING_COMPLETE_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.NettyRequestMetrics.measureTimeTaken;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.Attribute;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.http2.FlushOnReadHandler;
import software.amazon.awssdk.http.nio.netty.internal.http2.Http2StreamExceptionHandler;
import software.amazon.awssdk.http.nio.netty.internal.http2.Http2ToHttpInboundAdapter;
import software.amazon.awssdk.http.nio.netty.internal.http2.HttpToHttp2OutboundAdapter;
import software.amazon.awssdk.http.nio.netty.internal.nrs.HttpStreamsClientHandler;
import software.amazon.awssdk.http.nio.netty.internal.nrs.StreamedHttpRequest;
import software.amazon.awssdk.http.nio.netty.internal.utils.ChannelUtils;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyClientLogger;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils;
import software.amazon.awssdk.metrics.MetricCollector;

@SdkInternalApi
public final class NettyRequestExecutor {
    private static final NettyClientLogger log = NettyClientLogger.getLogger(NettyRequestExecutor.class);
    private static final RequestAdapter REQUEST_ADAPTER_HTTP2 = new RequestAdapter(Protocol.HTTP2);
    private static final RequestAdapter REQUEST_ADAPTER_HTTP1_1 = new RequestAdapter(Protocol.HTTP1_1);
    private static final AtomicLong EXECUTION_COUNTER = new AtomicLong(0L);
    private final long executionId = EXECUTION_COUNTER.incrementAndGet();
    private final RequestContext context;
    private CompletableFuture<Void> executeFuture;
    private Channel channel;
    private RequestAdapter requestAdapter;

    public NettyRequestExecutor(RequestContext context) {
        this.context = context;
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> execute() {
        Promise<Channel> channelFuture = context.eventLoopGroup().next().newPromise();
        executeFuture = createExecutionFuture(channelFuture);
        acquireChannel(channelFuture);
        channelFuture.addListener((GenericFutureListener) this::makeRequestListener);
        return executeFuture;
    }

    private void acquireChannel(Promise<Channel> channelFuture) {
        NettyRequestMetrics.ifMetricsAreEnabled(context.metricCollector(), metrics -> {
            measureTimeTaken(channelFuture, duration -> {
                metrics.reportMetric(CONCURRENCY_ACQUIRE_DURATION, duration);
            });
        });
        context.channelPool().acquire(channelFuture);
    }

    /**
     * Convenience method to create the execution future and set up the cancellation logic.
     *
     * @param channelPromise The Netty future holding the channel.
     *
     * @return The created execution future.
     */
    private CompletableFuture<Void> createExecutionFuture(Promise<Channel> channelPromise) {
        CompletableFuture<Void> metricsFuture = initiateMetricsCollection();

        CompletableFuture<Void> future = new CompletableFuture<>();
        future.whenComplete((r, t) -> {
            verifyMetricsWereCollected(metricsFuture);

            if (t == null) {
                return;
            }

            if (!channelPromise.tryFailure(t)) {
                // Couldn't fail promise, it's already done
                if (!channelPromise.isSuccess()) {
                    return;
                }
                Channel ch = channelPromise.getNow();
                try {
                    ch.eventLoop().submit(() -> {
                        Attribute<Long> executionIdKey = ch.attr(EXECUTION_ID_KEY);
                        if (ch.attr(IN_USE) != null && ch.attr(IN_USE).get() && executionIdKey != null) {
                            ch.pipeline().fireExceptionCaught(new FutureCancelledException(this.executionId, t));
                        } else {
                            ch.close().addListener(closeFuture -> context.channelPool().release(ch));
                        }
                    });
                } catch (Throwable exc) {
                    log.warn(ch, () -> "Unable to add a task to cancel the request to channel's EventLoop", exc);
                }
            }
        });

        return future;
    }

    private CompletableFuture<Void> initiateMetricsCollection() {
        MetricCollector metricCollector = context.metricCollector();
        if (!NettyRequestMetrics.metricsAreEnabled(metricCollector)) {
            return null;
        }
        return context.channelPool().collectChannelPoolMetrics(metricCollector);
    }

    private void verifyMetricsWereCollected(CompletableFuture<Void> metricsFuture) {
        if (metricsFuture == null) {
            return;
        }

        if (!metricsFuture.isDone()) {
            log.debug(null, () -> "HTTP request metric collection did not finish in time, so results may be incomplete.");
            metricsFuture.cancel(false);
            return;
        }

        metricsFuture.exceptionally(t -> {
            log.debug(null, () -> "HTTP request metric collection failed, so results may be incomplete.", t);
            return null;
        });
    }

    private void makeRequestListener(Future<Channel> channelFuture) {
        if (channelFuture.isSuccess()) {
            channel = channelFuture.getNow();
            NettyUtils.doInEventLoop(channel.eventLoop(), () -> {
                try {
                    configureChannel();
                    configurePipeline();
                    makeRequest();
                } catch (Throwable t) {
                    closeAndRelease(channel);
                    handleFailure(channel, () -> "Failed to initiate request to " + endpoint(), t);
                }
            });
        } else {
            handleFailure(channel, () -> "Failed to create connection to " + endpoint(), channelFuture.cause());
        }
    }

    private void configureChannel() {
        channel.attr(EXECUTION_ID_KEY).set(executionId);
        channel.attr(EXECUTE_FUTURE_KEY).set(executeFuture);
        channel.attr(REQUEST_CONTEXT_KEY).set(context);
        channel.attr(RESPONSE_COMPLETE_KEY).set(false);
        channel.attr(STREAMING_COMPLETE_KEY).set(false);
        channel.attr(RESPONSE_CONTENT_LENGTH).set(null);
        channel.attr(RESPONSE_DATA_READ).set(null);
        channel.attr(CHANNEL_DIAGNOSTICS).get().incrementRequestCount();
        channel.config().setOption(ChannelOption.AUTO_READ, false);
    }

    private void configurePipeline() throws IOException {
        Protocol protocol = ChannelAttributeKey.getProtocolNow(channel);
        ChannelPipeline pipeline = channel.pipeline();

        switch (protocol) {
            case HTTP2:
                pipeline.addLast(new Http2ToHttpInboundAdapter());
                pipeline.addLast(new HttpToHttp2OutboundAdapter());
                pipeline.addLast(Http2StreamExceptionHandler.create());
                requestAdapter = REQUEST_ADAPTER_HTTP2;
                break;
            case HTTP1_1:
                requestAdapter = REQUEST_ADAPTER_HTTP1_1;
                break;
            default:
                throw new IOException("Unknown protocol: " + protocol);
        }

        if (protocol == Protocol.HTTP2) {
            pipeline.addLast(FlushOnReadHandler.getInstance());
        }
        pipeline.addLast(new HttpStreamsClientHandler());
        pipeline.addLast(ResponseHandler.getInstance());

        // It's possible that the channel could become inactive between checking it out from the pool, and adding our response
        // handler (which will monitor for it going inactive from now on).
        // Make sure it's active here, or the request will never complete: https://github.com/aws/aws-sdk-java-v2/issues/1207
        if (!channel.isActive()) {
            throw new IOException(NettyUtils.closedChannelMessage(channel));
        }
    }

    private void makeRequest() {
        HttpRequest request = requestAdapter.adapt(context.executeRequest().request());
        writeRequest(request);
    }

    private void writeRequest(HttpRequest request) {
        channel.pipeline().addFirst(new WriteTimeoutHandler(context.configuration().writeTimeoutMillis(),
                                                            TimeUnit.MILLISECONDS));
        StreamedRequest streamedRequest = new StreamedRequest(request,
                                                              context.executeRequest().requestContentPublisher());
        channel.writeAndFlush(streamedRequest)
               .addListener(wireCall -> {
                   // Done writing so remove the idle write timeout handler
                   ChannelUtils.removeIfExists(channel.pipeline(), WriteTimeoutHandler.class);
                   if (wireCall.isSuccess()) {
                       NettyRequestMetrics.publishHttp2StreamMetrics(context.metricCollector(), channel);

                       if (context.executeRequest().fullDuplex()) {
                           return;
                       }

                       channel.pipeline().addFirst(new ReadTimeoutHandler(context.configuration().readTimeoutMillis(),
                                                                          TimeUnit.MILLISECONDS));
                       channel.read();
                   } else {
                       // TODO: Are there cases where we can keep the channel open?
                       closeAndRelease(channel);
                       handleFailure(channel, () -> "Failed to make request to " + endpoint(), wireCall.cause());
                   }
               });

        if (shouldExplicitlyTriggerRead()) {

            // Should only add an one-time ReadTimeoutHandler to 100 Continue request.
            if (is100ContinueExpected()) {
                channel.pipeline().addFirst(new OneTimeReadTimeoutHandler(Duration.ofMillis(context.configuration()
                        .readTimeoutMillis())));
            } else {
                channel.pipeline().addFirst(new ReadTimeoutHandler(context.configuration().readTimeoutMillis(),
                                                                   TimeUnit.MILLISECONDS));
            }

            channel.read();
        }
    }

    /**
     * It should explicitly trigger Read for the following situations:
     *
     * - FullDuplex calls need to start reading at the same time we make the request.
     * - Request with "Expect: 100-continue" header should read the 100 continue response.
     *
     * @return true if it should explicitly read from channel
     */
    private boolean shouldExplicitlyTriggerRead() {
        return context.executeRequest().fullDuplex() || is100ContinueExpected();
    }

    private boolean is100ContinueExpected() {
        return context.executeRequest()
                      .request()
                      .firstMatchingHeader("Expect")
                      .filter(b -> b.equalsIgnoreCase("100-continue"))
                      .isPresent();
    }

    private URI endpoint() {
        return context.executeRequest().request().getUri();
    }

    private void handleFailure(Channel channel, Supplier<String> msgSupplier, Throwable cause) {
        log.debug(channel, msgSupplier, cause);
        cause = NettyUtils.decorateException(channel, cause);
        context.handler().onError(cause);
        executeFuture.completeExceptionally(cause);
    }

    /**
     * Close and release the channel back to the pool.
     *
     * @param channel The channel.
     */
    private void closeAndRelease(Channel channel) {
        log.trace(channel, () -> String.format("closing and releasing channel %s", channel.id().asLongText()));
        channel.attr(KEEP_ALIVE).set(false);
        channel.close();
        context.channelPool().release(channel);
    }

    /**
     * Just delegates to {@link HttpRequest} for all methods.
     */
    static class DelegateHttpRequest implements HttpRequest {
        protected final HttpRequest request;

        DelegateHttpRequest(HttpRequest request) {
            this.request = request;
        }

        @Override
        public HttpRequest setMethod(HttpMethod method) {
            this.request.setMethod(method);
            return this;
        }

        @Override
        public HttpRequest setUri(String uri) {
            this.request.setUri(uri);
            return this;
        }

        @Override
        public HttpMethod getMethod() {
            return this.request.method();
        }

        @Override
        public HttpMethod method() {
            return request.method();
        }

        @Override
        public String getUri() {
            return this.request.uri();
        }

        @Override
        public String uri() {
            return request.uri();
        }

        @Override
        public HttpVersion getProtocolVersion() {
            return this.request.protocolVersion();
        }

        @Override
        public HttpVersion protocolVersion() {
            return request.protocolVersion();
        }

        @Override
        public HttpRequest setProtocolVersion(HttpVersion version) {
            this.request.setProtocolVersion(version);
            return this;
        }

        @Override
        public HttpHeaders headers() {
            return this.request.headers();
        }

        @Override
        public DecoderResult getDecoderResult() {
            return this.request.decoderResult();
        }

        @Override
        public DecoderResult decoderResult() {
            return request.decoderResult();
        }

        @Override
        public void setDecoderResult(DecoderResult result) {
            this.request.setDecoderResult(result);
        }

        @Override
        public String toString() {
            return this.getClass().getName() + "(" + this.request.toString() + ")";
        }
    }

    /**
     * Decorator around {@link StreamedHttpRequest} to adapt a publisher of {@link ByteBuffer} (i.e. {@link
     * software.amazon.awssdk.http.async.SdkHttpContentPublisher}) to a publisher of {@link HttpContent}.
     * <p>
     * This publisher also prevents the adapted publisher from publishing more content to the subscriber than
     * the specified 'Content-Length' of the request.
     */
    private static class StreamedRequest extends DelegateHttpRequest implements StreamedHttpRequest {

        private final Publisher<ByteBuffer> publisher;
        private final Optional<Long> requestContentLength;
        private long written = 0L;
        private boolean done;
        private Subscription subscription;

        StreamedRequest(HttpRequest request, Publisher<ByteBuffer> publisher) {
            super(request);
            this.publisher = publisher;
            this.requestContentLength = contentLength(request);
        }

        @Override
        public void subscribe(Subscriber<? super HttpContent> subscriber) {
            publisher.subscribe(new Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    StreamedRequest.this.subscription = subscription;
                    subscriber.onSubscribe(subscription);
                }

                @Override
                public void onNext(ByteBuffer contentBytes) {
                    if (done) {
                        return;
                    }

                    try {
                        int newLimit = clampedBufferLimit(contentBytes.remaining());
                        contentBytes.limit(newLimit);
                        ByteBuf contentByteBuf = Unpooled.wrappedBuffer(contentBytes);
                        HttpContent content = new DefaultHttpContent(contentByteBuf);

                        subscriber.onNext(content);
                        written += newLimit;

                        if (!shouldContinuePublishing()) {
                            done = true;
                            subscription.cancel();
                            subscriber.onComplete();
                        }
                    } catch (Throwable t) {
                        onError(t);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    if (!done) {
                        done = true;
                        subscription.cancel();
                        subscriber.onError(t);
                    }
                }

                @Override
                public void onComplete() {
                    if (!done) {
                        Long expectedContentLength = requestContentLength.orElse(null);
                        if (expectedContentLength != null && written < expectedContentLength) {
                            onError(new IllegalStateException("Request content was only " + written + " bytes, but the specified "
                                                              + "content-length was " + expectedContentLength + " bytes."));
                        } else {
                            done = true;
                            subscriber.onComplete();
                        }
                    }
                }
            });
        }

        private int clampedBufferLimit(int bufLen) {
            return requestContentLength.map(cl ->
                (int) Math.min(cl - written, bufLen)
            ).orElse(bufLen);
        }

        private boolean shouldContinuePublishing() {
            return requestContentLength.map(cl -> written < cl).orElse(true);
        }

        private static Optional<Long> contentLength(HttpRequest request) {
            String value = request.headers().get("Content-Length");
            if (value != null) {
                try {
                    return Optional.of(Long.parseLong(value));
                } catch (NumberFormatException e) {
                    log.warn(null, () -> "Unable  to parse 'Content-Length' header. Treating it as non existent.");
                }
            }
            return Optional.empty();
        }
    }
}
