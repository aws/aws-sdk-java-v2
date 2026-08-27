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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Regression coverage for the NPE reported in
 * <a href="https://github.com/aws/aws-sdk-java-v2/issues/7271">#7271</a>: when a request carries
 * {@code Expect: 100-continue}, {@link HttpStreamsClientHandler} defers subscribing the {@link HandlerSubscriber} to the
 * body until the server answers {@code 100 Continue}. A {@code channelWritabilityChanged} event fired during that window
 * used to dereference the still-null subscription and throw.
 */
public class HandlerSubscriberExpectContinueTest {

    private EmbeddedChannel channel;
    private ExceptionCapturingHandler exceptionCapture;

    @BeforeEach
    public void setup() {
        channel = new EmbeddedChannel(new HttpStreamsClientHandler());
        exceptionCapture = new ExceptionCapturingHandler();
        channel.pipeline().addLast(exceptionCapture);
    }

    @AfterEach
    public void teardown() {
        channel.finishAndReleaseAll();
    }

    @Test
    public void channelWritabilityChanged_whenSubscriptionPending_doesNotRouteExceptionToPipeline() {
        writeExpectContinueRequest(new SingleChunkPublisher("body"));

        channel.pipeline().fireChannelWritabilityChanged();

        assertThat(exceptionCapture.captured()).isNull();
    }

    @Test
    public void channelWritabilityChanged_whenSubscriptionPending_bodyStreamsAfter100Continue() {
        SingleChunkPublisher body = new SingleChunkPublisher("body");
        writeExpectContinueRequest(body);

        channel.pipeline().fireChannelWritabilityChanged();
        deliver100Continue();

        assertThat(body.subscribed()).isTrue();
        assertThat(streamedBody()).isEqualTo("body");
        assertThat(exceptionCapture.captured()).isNull();
    }

    private void writeExpectContinueRequest(Publisher<HttpContent> body) {
        DefaultStreamedHttpRequest request =
            new DefaultStreamedHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, "/", body);
        HttpUtil.set100ContinueExpected(request, true);
        channel.writeOutbound(request);
    }

    private void deliver100Continue() {
        HttpResponse continueResponse =
            new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        channel.writeInbound(continueResponse);
        channel.runPendingTasks();
    }

    private String streamedBody() {
        StringBuilder body = new StringBuilder();
        Object msg;
        while ((msg = channel.readOutbound()) != null) {
            if (msg instanceof HttpContent) {
                body.append(((HttpContent) msg).content().toString(UTF_8));
            }
        }
        return body.toString();
    }

    private static final class ExceptionCapturingHandler extends ChannelInboundHandlerAdapter {
        private final AtomicReference<Throwable> captured = new AtomicReference<>();

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            captured.compareAndSet(null, cause);
        }

        Throwable captured() {
            return captured.get();
        }
    }

    private static final class SingleChunkPublisher implements Publisher<HttpContent> {
        private final byte[] payload;
        private final AtomicBoolean subscribed = new AtomicBoolean(false);

        private SingleChunkPublisher(String payload) {
            this.payload = payload.getBytes(UTF_8);
        }

        boolean subscribed() {
            return subscribed.get();
        }

        @Override
        public void subscribe(Subscriber<? super HttpContent> subscriber) {
            subscribed.set(true);
            subscriber.onSubscribe(new Subscription() {
                private boolean delivered;

                @Override
                public void request(long n) {
                    if (n <= 0 || delivered) {
                        return;
                    }
                    delivered = true;
                    ByteBuf buf = Unpooled.wrappedBuffer(payload);
                    subscriber.onNext(new DefaultHttpContent(buf));
                    subscriber.onComplete();
                }

                @Override
                public void cancel() {
                    delivered = true;
                }
            });
        }
    }
}
