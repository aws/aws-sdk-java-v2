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

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.EXECUTE_FUTURE_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.EXECUTION_ID_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.IN_USE;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.KEEP_ALIVE;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.LAST_HTTP_CONTENT_RECEIVED_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.REQUEST_CONTEXT_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.RESPONSE_COMPLETE_KEY;

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
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutException;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.http2.FlushOnReadHandler;
import software.amazon.awssdk.http.nio.netty.internal.http2.Http2StreamExceptionHandler;
import software.amazon.awssdk.http.nio.netty.internal.http2.Http2ToHttpInboundAdapter;
import software.amazon.awssdk.http.nio.netty.internal.http2.HttpToHttp2OutboundAdapter;
import software.amazon.awssdk.http.nio.netty.internal.nrs.HttpStreamsClientHandler;
import software.amazon.awssdk.http.nio.netty.internal.nrs.StreamedHttpRequest;
import software.amazon.awssdk.http.nio.netty.internal.utils.ChannelUtils;
import software.amazon.awssdk.metrics.MetricCollector;

@SdkInternalApi
public final class NettyRequestExecutor {
    private static final Logger log = LoggerFactory.getLogger(NettyRequestExecutor.class);
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
        context.channelPool().acquire(channelFuture);
        channelFuture.addListener((GenericFutureListener) this::makeRequestListener);
        return executeFuture;
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
                        if (ch.attr(IN_USE).get()) {
                            ch.pipeline().fireExceptionCaught(new FutureCancelledException(executionId, t));
                        } else {
                            ch.close().addListener(closeFuture -> context.channelPool().release(ch));
                        }
                    });
                } catch (Throwable exc) {
                    log.warn("Unable to add a task to cancel the request to channel's EventLoop", exc);
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
            log.debug("HTTP request metric collection did not finish in time, so results may be incomplete.");
            metricsFuture.cancel(false);
            return;
        }

        metricsFuture.exceptionally(t -> {
            log.debug("HTTP request metric collection failed, so results may be incomplete.", t);
            return null;
        });
    }

    private void makeRequestListener(Future<Channel> channelFuture) {
        if (channelFuture.isSuccess()) {
            channel = channelFuture.getNow();
            configureChannel();
            if (tryConfigurePipeline()) {
                makeRequest();
            }
        } else {
            handleFailure(() -> "Failed to create connection to " + endpoint(), channelFuture.cause());
        }
    }

    private void configureChannel() {
        channel.attr(EXECUTION_ID_KEY).set(executionId);
        channel.attr(EXECUTE_FUTURE_KEY).set(executeFuture);
        channel.attr(REQUEST_CONTEXT_KEY).set(context);
        channel.attr(RESPONSE_COMPLETE_KEY).set(false);
        channel.attr(LAST_HTTP_CONTENT_RECEIVED_KEY).set(false);
        channel.attr(IN_USE).set(true);
        channel.config().setOption(ChannelOption.AUTO_READ, false);
    }

    private boolean tryConfigurePipeline() {
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
                String errorMsg = "Unknown protocol: " + protocol;
                closeAndRelease(channel);
                handleFailure(() -> errorMsg, new RuntimeException(errorMsg));
                return false;
        }

        pipeline.addLast(LastHttpContentHandler.create());
        if (Protocol.HTTP2.equals(protocol)) {
            pipeline.addLast(FlushOnReadHandler.getInstance());
        }
        pipeline.addLast(new HttpStreamsClientHandler());
        pipeline.addLast(ResponseHandler.getInstance());

        // It's possible that the channel could become inactive between checking it out from the pool, and adding our response
        // handler (which will monitor for it going inactive from now on).
        // Make sure it's active here, or the request will never complete: https://github.com/aws/aws-sdk-java-v2/issues/1207
        if (!channel.isActive()) {
            String errorMessage = "Channel was closed before it could be written to.";
            closeAndRelease(channel);
            handleFailure(() -> errorMessage, new IOException(errorMessage));
            return false;
        }

        return true;
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
                       handleFailure(() -> "Failed to make request to " + endpoint(), wireCall.cause());
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

    private void handleFailure(Supplier<String> msg, Throwable cause) {
        log.debug(msg.get(), cause);
        cause = decorateException(cause);
        context.handler().onError(cause);
        executeFuture.completeExceptionally(cause);
    }

    private Throwable decorateException(Throwable originalCause) {
        if (isAcquireTimeoutException(originalCause)) {
            return new Throwable(getMessageForAcquireTimeoutException(), originalCause);
        } else if (isTooManyPendingAcquiresException(originalCause)) {
            return new Throwable(getMessageForTooManyAcquireOperationsError(), originalCause);
        } else if (originalCause instanceof ReadTimeoutException) {
            return new IOException("Read timed out", originalCause);
        } else if (originalCause instanceof WriteTimeoutException) {
            return new IOException("Write timed out", originalCause);
        } else if (originalCause instanceof ClosedChannelException) {
            return new IOException(getMessageForClosedChannel(), originalCause);
        }

        return originalCause;
    }

    private boolean isAcquireTimeoutException(Throwable originalCause) {
        String message = originalCause.getMessage();
        return originalCause instanceof TimeoutException &&
                message != null &&
                message.contains("Acquire operation took longer");
    }

    private boolean isTooManyPendingAcquiresException(Throwable originalCause) {
        String message = originalCause.getMessage();
        return originalCause instanceof IllegalStateException &&
               message != null &&
               originalCause.getMessage().contains("Too many outstanding acquire operations");
    }

    private String getMessageForAcquireTimeoutException() {
        return "Acquire operation took longer than the configured maximum time. This indicates that a request cannot get a "
                + "connection from the pool within the specified maximum time. This can be due to high request rate.\n"

                + "Consider taking any of the following actions to mitigate the issue: increase max connections, "
                + "increase acquire timeout, or slowing the request rate.\n"

                + "Increasing the max connections can increase client throughput (unless the network interface is already "
                + "fully utilized), but can eventually start to hit operation system limitations on the number of file "
                + "descriptors used by the process. If you already are fully utilizing your network interface or cannot "
                + "further increase your connection count, increasing the acquire timeout gives extra time for requests to "
                + "acquire a connection before timing out. If the connections doesn't free up, the subsequent requests "
                + "will still timeout.\n"

                + "If the above mechanisms are not able to fix the issue, try smoothing out your requests so that large "
                + "traffic bursts cannot overload the client, being more efficient with the number of times you need to "
                + "call AWS, or by increasing the number of hosts sending requests.";
    }

    private String getMessageForTooManyAcquireOperationsError() {
        return "Maximum pending connection acquisitions exceeded. The request rate is too high for the client to keep up.\n"

                + "Consider taking any of the following actions to mitigate the issue: increase max connections, "
                + "increase max pending acquire count, decrease pool lease timeout, or slowing the request rate.\n"

                + "Increasing the max connections can increase client throughput (unless the network interface is already "
                + "fully utilized), but can eventually start to hit operation system limitations on the number of file "
                + "descriptors used by the process. If you already are fully utilizing your network interface or cannot "
                + "further increase your connection count, increasing the pending acquire count allows extra requests to be "
                + "buffered by the client, but can cause additional request latency and higher memory usage. If your request"
                + " latency or memory usage is already too high, decreasing the lease timeout will allow requests to fail "
                + "more quickly, reducing the number of pending connection acquisitions, but likely won't decrease the total "
                + "number of failed requests.\n"

                + "If the above mechanisms are not able to fix the issue, try smoothing out your requests so that large "
                + "traffic bursts cannot overload the client, being more efficient with the number of times you need to call "
                + "AWS, or by increasing the number of hosts sending requests.";
    }

    private String getMessageForClosedChannel() {
        return "The channel was closed. This may have been done by the client (e.g. because the request was aborted), " +
               "by the service (e.g. because the request took too long or the client tried to write on a read-only socket), " +
               "or by an intermediary party (e.g. because the channel was idle for too long).";
    }

    /**
     * Close and release the channel back to the pool.
     *
     * @param channel The channel.
     */
    private void closeAndRelease(Channel channel) {
        log.trace("closing and releasing channel {}", channel.id().asLongText());
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
     * <p />
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
                        done = true;
                        subscriber.onComplete();
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
                    log.warn("Unable  to parse 'Content-Length' header. Treating it as non existent.");
                }
            }
            return Optional.empty();
        }
    }
}
