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
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.Future;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.async.AbortableRunnable;
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
                    initializePerRequestHandlers();
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

    /**
     * Add any per-request handlers to the pipeline.
     */
    private void initializePerRequestHandlers() {
        channel.pipeline().addLast(new HttpStreamsClientHandler());
        channel.pipeline().addLast(new ResponseHandler());
    }

    @Override
    public void abort() {
        if (channel != null) {
            closeAndRelease(channel);
        }
    }

    private void makeRequest(HttpRequest request) {
        log.debug("Writing request: {}", request);
        channel.pipeline().addFirst(new WriteTimeoutHandler(context.configuration().writeTimeout()));
        channel.writeAndFlush(new StreamedRequest(request, context.sdkRequestProvider(), channel))
               .addListener(wireCall -> {
                   ChannelUtils.removeIfExists(channel.pipeline(), WriteTimeoutHandler.class);
                   if (wireCall.isSuccess()) {
                       channel.pipeline().addFirst(new ReadTimeoutHandler(context.configuration().readTimeout()));
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

    private void handleFailure(Supplier<String> msg, Throwable cause) {
        log.error(msg.get(), cause);
        runAndLogError("Exception thrown from AsyncResponseHandler",
            () -> context.handler().exceptionOccurred(modifyHighBurstTrafficException(cause)));
        if (channel != null) {
            runAndLogError("Unable to release channel back to the pool.", () -> closeAndRelease(channel));
        }
    }

    private Throwable modifyHighBurstTrafficException(Throwable originalCause) {
        String originalMessage = originalCause.getMessage();
        String newMessage = null;

        if (originalCause instanceof TimeoutException &&
            originalMessage.contains("Acquire operation took longer")) {
            newMessage = getMessageForAcquireTimeoutException();

        } else if (originalCause instanceof IllegalStateException &&
                   originalMessage.contains("Too many outstanding acquire operations")) {
            newMessage = getMessageForTooManyAcquireOperationsError();

        } else {
            return originalCause;
        }

        return new Throwable(newMessage, originalCause);
    }


    private String getMessageForAcquireTimeoutException() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder
            .append("Acquire operation took longer than the configured maximum time. This indicates that a request cannot get a "
                  + "connection from the pool within the specified maximum time. This can be due to high request rate.\n")

            .append("Consider taking any of the following actions to mitigate the issue: increase max connections, "
                  + "increase acquire timeout, or slowing the request rate.\n")

            .append("Increasing the max connections can increase client throughput (unless the network interface is already "
                    + "fully utilized), but can eventually start to hit operation system limitations on the number of file "
                    + "descriptors used by the process. If you already are fully utilizing your network interface or cannot "
                    + "further increase your connection count, increasing the acquire timeout gives extra time for requests to "
                    + "acquire a connection before timing out. If the connections doesn't free up, the subsequent requests "
                    + "will still timeout.\n")

            .append("If the above mechanisms are not able to fix the issue, try smoothing out your requests so that large "
                    + "traffic bursts cannot overload the client, being more efficient with the number of times you need to "
                    + "call AWS, or by increasing the number of hosts sending requests.");

        return stringBuilder.toString();
    }

    private String getMessageForTooManyAcquireOperationsError() {
        StringBuilder  stringBuilder = new StringBuilder();

        stringBuilder
            .append("Maximum pending connection acquisitions exceeded. The request rate is too high for the client to keep up.\n")

            .append("Consider taking any of the following actions to mitigate the issue: increase max connections, "
                  + "increase max pending acquire count, decrease pool lease timeout, or slowing the request rate.\n")

            .append("Increasing the max connections can increase client throughput (unless the network interface is already "
                    + "fully utilized), but can eventually start to hit operation system limitations on the number of file "
                    + "descriptors used by the process. If you already are fully utilizing your network interface or cannot "
                    + "further increase your connection count, increasing the pending acquire count allows extra requests to be "
                    + "buffered by the client, but can cause additional request latency and higher memory usage. If your request"
                    + " latency or memory usage is already too high, decreasing the lease timeout will allow requests to fail "
                    + "more quickly, reducing the number of pending connection acquisitions, but likely won't decrease the total "
                    + "number of failed requests.\n")

            .append("If the above mechanisms are not able to fix the issue, try smoothing out your requests so that large "
                    + "traffic bursts cannot overload the client, being more efficient with the number of times you need to call "
                    + "AWS, or by increasing the number of hosts sending requests.");

        return stringBuilder.toString();
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

        public HttpRequest setMethod(HttpMethod method) {
            this.request.setMethod(method);
            return this;
        }

        public HttpRequest setUri(String uri) {
            this.request.setUri(uri);
            return this;
        }

        public HttpMethod getMethod() {
            return this.request.getMethod();
        }

        @Override
        public HttpMethod method() {
            return request.method();
        }

        public String getUri() {
            return this.request.getUri();
        }

        @Override
        public String uri() {
            return request.uri();
        }


        public HttpVersion getProtocolVersion() {
            return this.request.getProtocolVersion();
        }

        @Override
        public HttpVersion protocolVersion() {
            return request.protocolVersion();
        }

        public HttpRequest setProtocolVersion(HttpVersion version) {
            this.request.setProtocolVersion(version);
            return this;
        }

        public HttpHeaders headers() {
            return this.request.headers();
        }

        public DecoderResult getDecoderResult() {
            return this.request.getDecoderResult();
        }

        @Override
        public DecoderResult decoderResult() {
            return request.decoderResult();
        }

        public void setDecoderResult(DecoderResult result) {
            this.request.setDecoderResult(result);
        }

        public String toString() {
            return this.getClass().getName() + "(" + this.request.toString() + ")";
        }
    }

    /**
     * Decorator around {@link StreamedHttpRequest} to adapt a publisher of {@link ByteBuffer} (i.e. {@link
     * software.amazon.awssdk.http.async.SdkHttpRequestProvider}) to a publisher of {@link HttpContent}.
     */
    private static class StreamedRequest extends DelegateHttpRequest implements StreamedHttpRequest {

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
