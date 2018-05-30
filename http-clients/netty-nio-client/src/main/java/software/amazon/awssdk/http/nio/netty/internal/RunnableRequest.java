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

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.REQUEST_CONTEXT_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.RESPONSE_COMPLETE_KEY;

import com.typesafe.netty.http.HttpStreamsClientHandler;
import com.typesafe.netty.http.StreamedHttpRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
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
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.nio.netty.internal.http2.Http2ToHttpInboundAdapter;
import software.amazon.awssdk.http.nio.netty.internal.http2.HttpToHttp2OutboundAdapter;
import software.amazon.awssdk.http.nio.netty.internal.utils.ChannelUtils;
import software.amazon.awssdk.utils.FunctionalUtils.UnsafeRunnable;

public final class RunnableRequest implements AbortableRunnable {

    private static final Logger log = LoggerFactory.getLogger(RunnableRequest.class);
    private final RequestContext context;
    private volatile Channel channel;

    public RunnableRequest(RequestContext context) {
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

        runOrFail(() -> {
            configurePipeline();
            writeRequest(request);
        },
            () -> "Failed to make request to " + endpoint());
    }

    private void configurePipeline() {
        Protocol protocol = ChannelAttributeKeys.getProtocolNow(channel);
        if (Protocol.HTTP2.equals(protocol)) {
            channel.pipeline().addLast(new Http2ToHttpInboundAdapter());
            channel.pipeline().addLast(new HttpToHttp2OutboundAdapter());
        } else if (!Protocol.HTTP1_1.equals(protocol)) {
            throw new RuntimeException("Unknown protocol: " + protocol);
        }
        channel.config().setOption(ChannelOption.AUTO_READ, false);
        channel.pipeline().addLast(new HttpStreamsClientHandler());
        channel.pipeline().addLast(new ResponseHandler());
    }

    private void writeRequest(HttpRequest request) {
        channel.pipeline().addFirst(new WriteTimeoutHandler(context.configuration().writeTimeoutMillis(),
                                                            TimeUnit.MILLISECONDS));

        channel.writeAndFlush(new StreamedRequest(request, context.sdkRequestProvider(), channel))
               .addListener(wireCall -> {
                   // Done writing so remove the idle write timeout handler
                   ChannelUtils.removeIfExists(channel.pipeline(), WriteTimeoutHandler.class);
                   if (wireCall.isSuccess()) {
                       // Starting read so add the idle read timeout handler, removed when channel is released
                       channel.pipeline().addFirst(new ReadTimeoutHandler(context.configuration().readTimeoutMillis(),
                                                                          TimeUnit.MILLISECONDS));
                       // Auto-read is turned off so trigger an explicit read to give control to HttpStreamsClientHandler
                       channel.read();
                   } else {
                       handleFailure(() -> "Failed to make request to " + endpoint(), wireCall.cause());
                   }
               });
    }

    private URI endpoint() {
        return context.sdkRequest().getUri();
    }

    private void runOrFail(Runnable runnable, Supplier<String> errorMsgSupplier) {
        try {
            runnable.run();
        } catch (Exception e) {
            handleFailure(errorMsgSupplier, e);
        }
    }

    private void handleFailure(Supplier<String> msg, Throwable cause) {
        log.error(msg.get(), cause);
        Throwable throwable = decorateException(cause);
        runAndLogError("Exception thrown from AsyncResponseHandler",
            () -> context.handler().exceptionOccurred(throwable));
        if (channel != null) {
            runAndLogError("Unable to release channel back to the pool.",
                () -> closeAndRelease(channel));
        }
    }

    private Throwable decorateException(Throwable originalCause) {
        if (isAcquireTimeoutException(originalCause)) {
            return new Throwable(getMessageForAcquireTimeoutException(), originalCause);
        } else if (isTooManyPendingAcquiresException(originalCause)) {
            return new Throwable(getMessageForTooManyAcquireOperationsError(), originalCause);
        } else if (originalCause instanceof ReadTimeoutException) {
            // wrap it with IOException to be retried by SDK
            return new IOException("Read timed out", originalCause);
        } else if (originalCause instanceof WriteTimeoutException) {
            return new IOException("Write timed out", originalCause);
        }

        return originalCause;
    }

    private boolean isAcquireTimeoutException(Throwable originalCause) {
        return originalCause instanceof TimeoutException && originalCause.getMessage().contains("Acquire operation took longer");
    }

    private boolean isTooManyPendingAcquiresException(Throwable originalCause) {
        return originalCause instanceof IllegalStateException &&
               originalCause.getMessage().contains("Too many outstanding acquire operations");
    }

    private String getMessageForAcquireTimeoutException() {
        return "Acquire operation took longer than the configured maximum time. This indicates that a request cannot get a "
               + "connection from the pool within the specified maximum time. This can be due to high request rate.\n" +
               "Consider taking any of the following actions to mitigate the issue: increase max connections, "
               + "increase acquire timeout, or slowing the request rate.\n" +
               "Increasing the max connections can increase client throughput (unless the network interface is already "
               + "fully utilized), but can eventually start to hit operation system limitations on the number of file "
               + "descriptors used by the process. If you already are fully utilizing your network interface or cannot "
               + "further increase your connection count, increasing the acquire timeout gives extra time for requests to "
               + "acquire a connection before timing out. If the connections doesn't free up, the subsequent requests "
               + "will still timeout.\n" +
               "If the above mechanisms are not able to fix the issue, try smoothing out your requests so that large "
               + "traffic bursts cannot overload the client, being more efficient with the number of times you need to "
               + "call AWS, or by increasing the number of hosts sending requests.";

    }

    private String getMessageForTooManyAcquireOperationsError() {
        return "Maximum pending connection acquisitions exceeded. The request rate is too high for the client to keep up.\n" +
               "Consider taking any of the following actions to mitigate the issue: increase max connections, "
               + "increase max pending acquire count, decrease pool lease timeout, or slowing the request rate.\n" +
               "Increasing the max connections can increase client throughput (unless the network interface is already "
               + "fully utilized), but can eventually start to hit operation system limitations on the number of file "
               + "descriptors used by the process. If you already are fully utilizing your network interface or cannot "
               + "further increase your connection count, increasing the pending acquire count allows extra requests to be "
               + "buffered by the client, but can cause additional request latency and higher memory usage. If your request"
               + " latency or memory usage is already too high, decreasing the lease timeout will allow requests to fail "
               + "more quickly, reducing the number of pending connection acquisitions, but likely won't decrease the total "
               + "number of failed requests.\n" +
               "If the above mechanisms are not able to fix the issue, try smoothing out your requests so that large "
               + "traffic bursts cannot overload the client, being more efficient with the number of times you need to call "
               + "AWS, or by increasing the number of hosts sending requests.";

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
     * software.amazon.awssdk.http.async.SdkHttpRequestProvider}) to a publisher of {@link HttpContent}.
     */
    private class StreamedRequest extends DelegateHttpRequest implements StreamedHttpRequest {

        private final Publisher<ByteBuffer> publisher;
        private final Channel channel;

        StreamedRequest(HttpRequest request, Publisher<ByteBuffer> publisher, Channel channel) {
            super(request);
            this.publisher = publisher;
            this.channel = channel;
        }

        @Override
        public void subscribe(Subscriber<? super HttpContent> subscriber) {
            publisher.subscribe(new Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscriber.onSubscribe(subscription);
                }

                @Override
                public void onNext(ByteBuffer byteBuffer) {
                    ByteBuf buffer = channel.alloc().buffer(byteBuffer.remaining());
                    buffer.writeBytes(byteBuffer);
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
            });
        }
    }
}
