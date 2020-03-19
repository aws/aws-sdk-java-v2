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

package software.amazon.awssdk.http.nio.netty.internal.nrs;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Handler that converts written {@link StreamedHttpRequest} messages into {@link HttpRequest} messages
 * followed by {@link HttpContent} messages and reads {@link HttpResponse} messages followed by
 * {@link HttpContent} messages and produces {@link StreamedHttpResponse} messages.
 *
 * This allows request and response bodies to be handled using reactive streams.
 *
 * There are two types of messages that this handler accepts for writing, {@link StreamedHttpRequest} and
 * {@link FullHttpRequest}. Writing any other messages may potentially lead to HTTP message mangling.
 *
 * There are two types of messages that this handler will send down the chain, {@link StreamedHttpResponse},
 * and {@link FullHttpResponse}. If {@link io.netty.channel.ChannelOption#AUTO_READ} is false for the channel,
 * then any {@link StreamedHttpResponse} messages <em>must</em> be subscribed to consume the body, otherwise
 * it's possible that no read will be done of the messages.
 *
 * As long as messages are returned in the order that they arrive, this handler implicitly supports HTTP
 * pipelining.
 *
 * This class contains source imported from https://github.com/playframework/netty-reactive-streams,
 * licensed under the Apache License 2.0, available at the time of the fork (1/31/2020) here:
 * https://github.com/playframework/netty-reactive-streams/blob/master/LICENSE.txt
 *
 * All original source licensed under the Apache License 2.0 by playframework. All modifications are
 * licensed under the Apache License 2.0 by Amazon Web Services.
 */
@SdkInternalApi
public class HttpStreamsClientHandler extends HttpStreamsHandler<HttpResponse, HttpRequest> {

    private int inFlight = 0;
    private int withServer = 0;
    private ChannelPromise closeOnZeroInFlight = null;
    private Subscriber<HttpContent> awaiting100Continue;
    private StreamedHttpMessage awaiting100ContinueMessage;
    private boolean ignoreResponseBody = false;

    public HttpStreamsClientHandler() {
        super(HttpResponse.class, HttpRequest.class);
    }

    @Override
    protected boolean hasBody(HttpResponse response) {
        if (response.status().code() >= 100 && response.status().code() < 200) {
            return false;
        }

        if (response.status().equals(HttpResponseStatus.NO_CONTENT) ||
            response.status().equals(HttpResponseStatus.NOT_MODIFIED)) {
            return false;
        }

        if (HttpUtil.isTransferEncodingChunked(response)) {
            return true;
        }


        if (HttpUtil.isContentLengthSet(response)) {
            return HttpUtil.getContentLength(response) > 0;
        }

        return true;
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise future) throws Exception {
        if (inFlight == 0) {
            ctx.close(future);
        } else {
            closeOnZeroInFlight = future;
        }
    }

    @Override
    protected void consumedInMessage(ChannelHandlerContext ctx) {
        inFlight--;
        withServer--;
        if (inFlight == 0 && closeOnZeroInFlight != null) {
            ctx.close(closeOnZeroInFlight);
        }
    }

    @Override
    protected void receivedOutMessage(ChannelHandlerContext ctx) {
        inFlight++;
    }

    @Override
    protected void sentOutMessage(ChannelHandlerContext ctx) {
        withServer++;
    }

    @Override
    protected HttpResponse createEmptyMessage(HttpResponse response) {
        return new EmptyHttpResponse(response);
    }

    @Override
    protected HttpResponse createStreamedMessage(HttpResponse response, Publisher<HttpContent> stream) {
        return new DelegateStreamedHttpResponse(response, stream);
    }

    @Override
    protected void subscribeSubscriberToStream(StreamedHttpMessage msg, Subscriber<HttpContent> subscriber) {
        if (HttpUtil.is100ContinueExpected(msg)) {
            awaiting100Continue = subscriber;
            awaiting100ContinueMessage = msg;
        } else {
            super.subscribeSubscriberToStream(msg, subscriber);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof HttpResponse && awaiting100Continue != null && withServer == 0) {
            HttpResponse response = (HttpResponse) msg;
            if (response.status().equals(HttpResponseStatus.CONTINUE)) {
                super.subscribeSubscriberToStream(awaiting100ContinueMessage, awaiting100Continue);
                awaiting100Continue = null;
                awaiting100ContinueMessage = null;
                if (msg instanceof FullHttpResponse) {
                    ReferenceCountUtil.release(msg);
                } else {
                    ignoreResponseBody = true;
                }
            } else {
                awaiting100ContinueMessage.subscribe(new CancelledSubscriber<HttpContent>());
                awaiting100ContinueMessage = null;
                awaiting100Continue.onSubscribe(new NoOpSubscription());
                awaiting100Continue.onComplete();
                awaiting100Continue = null;
                super.channelRead(ctx, msg);
            }
        } else if (ignoreResponseBody && msg instanceof HttpContent) {

            ReferenceCountUtil.release(msg);
            if (msg instanceof LastHttpContent) {
                ignoreResponseBody = false;
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

    private static class NoOpSubscription implements Subscription {
        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
        }
    }
}
