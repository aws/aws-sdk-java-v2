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


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.EXECUTE_FUTURE_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PROTOCOL_FUTURE;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.REQUEST_CONTEXT_KEY;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.internal.nrs.DefaultStreamedHttpResponse;
import software.amazon.awssdk.http.nio.netty.internal.nrs.StreamedHttpResponse;

@RunWith(MockitoJUnitRunner.class)
public class PublisherAdapterTest {

    @Mock
    private ChannelHandlerContext ctx;

    private MockChannel channel;

    @Mock
    private SdkChannelPool channelPool;

    @Mock
    private EventLoopGroup eventLoopGroup;

    @Mock
    private SdkAsyncHttpResponseHandler responseHandler;

    private HttpContent fullHttpResponse;

    private RequestContext requestContext;

    private CompletableFuture<Void> executeFuture;
    private ResponseHandler nettyResponseHandler;

    @Before
    public void setUp() throws Exception {
        executeFuture = new CompletableFuture<>();
        fullHttpResponse = mock(DefaultHttpContent.class);

        when(fullHttpResponse.content()).thenReturn(new EmptyByteBuf(ByteBufAllocator.DEFAULT));
        requestContext = new RequestContext(channelPool,
                                            eventLoopGroup,
                                            AsyncExecuteRequest.builder().responseHandler(responseHandler).build(),
                                            null);

        channel = new MockChannel();
        channel.attr(PROTOCOL_FUTURE).set(CompletableFuture.completedFuture(Protocol.HTTP1_1));
        channel.attr(REQUEST_CONTEXT_KEY).set(requestContext);
        channel.attr(EXECUTE_FUTURE_KEY).set(executeFuture);
        when(ctx.channel()).thenReturn(channel);

        nettyResponseHandler = ResponseHandler.getInstance();
        DefaultHttpResponse defaultFullHttpResponse = mock(DefaultHttpResponse.class);
        when(defaultFullHttpResponse.headers()).thenReturn(EmptyHttpHeaders.INSTANCE);
        when(defaultFullHttpResponse.status()).thenReturn(HttpResponseStatus.CREATED);
        when(defaultFullHttpResponse.protocolVersion()).thenReturn(HttpVersion.HTTP_1_1);
        nettyResponseHandler.channelRead0(ctx, defaultFullHttpResponse);
    }

    @Test
    public void successfulStreaming_shouldNotInvokeChannelRead() {
        Flowable<HttpContent> testPublisher = Flowable.just(fullHttpResponse);

        StreamedHttpResponse streamedHttpResponse = new DefaultStreamedHttpResponse(HttpVersion.HTTP_1_1,
                                                                                    HttpResponseStatus.ACCEPTED,
                                                                                    testPublisher);



        ResponseHandler.PublisherAdapter publisherAdapter = new ResponseHandler.PublisherAdapter(streamedHttpResponse,
                                                                                                 ctx,
                                                                                                 requestContext,
                                                                                                 executeFuture
        );
        TestSubscriber subscriber = new TestSubscriber();

        publisherAdapter.subscribe(subscriber);

        verify(ctx, times(0)).read();
        verify(ctx, times(0)).close();
        assertThat(subscriber.isCompleted).isEqualTo(true);
        verify(channelPool).release(channel);
        executeFuture.join();
        assertThat(executeFuture).isCompleted();
    }

    @Test
    public void errorOccurred_shouldInvokeResponseHandler() {
        RuntimeException exception = new RuntimeException("boom");
        Flowable<HttpContent> testPublisher = Flowable.error(exception);

        StreamedHttpResponse streamedHttpResponse = new DefaultStreamedHttpResponse(HttpVersion.HTTP_1_1,
                                                                                    HttpResponseStatus.ACCEPTED,
                                                                                    testPublisher);



        ResponseHandler.PublisherAdapter publisherAdapter = new ResponseHandler.PublisherAdapter(streamedHttpResponse,
                                                                                                 ctx,
                                                                                                 requestContext,
                                                                                                 executeFuture
        );
        TestSubscriber subscriber = new TestSubscriber();

        publisherAdapter.subscribe(subscriber);

        verify(ctx, times(0)).read();
        verify(ctx).close();
        assertThat(subscriber.errorOccurred).isEqualTo(true);
        verify(channelPool).release(channel);
        assertThat(executeFuture).isCompletedExceptionally();
        verify(responseHandler).onError(exception);
    }

    @Test
    public void subscriptionCancelled_upstreamPublisherCallsOnNext_httpContentReleased() {
        HttpContent firstContent = mock(HttpContent.class);
        when(firstContent.content()).thenReturn(Unpooled.EMPTY_BUFFER);

        HttpContent[] contentToIgnore = new HttpContent[8];
        for (int i = 0; i < contentToIgnore.length; ++i) {
            contentToIgnore[i] = mock(HttpContent.class);
            when(contentToIgnore[i].content()).thenReturn(Unpooled.EMPTY_BUFFER);
        }

        Publisher<HttpContent> publisher = subscriber -> subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long l) {
                // We ignore any cancel signal and just publish all the content
                subscriber.onNext(firstContent);

                for (int i = 0; i < l && i < contentToIgnore.length; ++i) {
                    subscriber.onNext(contentToIgnore[i]);
                }
            }

            @Override
            public void cancel() {
                // no-op
            }
        });

        DefaultStreamedHttpResponse streamedResponse = new DefaultStreamedHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK, publisher);

        Subscriber<ByteBuffer> subscriber = new Subscriber<ByteBuffer>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                subscription.cancel();
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        };

        ResponseHandler.PublisherAdapter publisherAdapter = new ResponseHandler.PublisherAdapter(streamedResponse, ctx,
                requestContext, executeFuture);

        publisherAdapter.subscribe(subscriber);

        // First one should be accessed as normal
        verify(firstContent).content();
        verify(firstContent).release();

        for (int i = 0; i < contentToIgnore.length; ++i) {
            verify(contentToIgnore[i]).release();
            verifyNoMoreInteractions(contentToIgnore[i]);
        }
    }

    static final class TestSubscriber implements Subscriber<ByteBuffer> {

        private Subscription subscription;
        private boolean isCompleted = false;
        private boolean errorOccurred = false;

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            subscription.request(1);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            subscription.request(1);
        }

        @Override
        public void onError(Throwable t) {
            errorOccurred = true;
        }

        @Override
        public void onComplete() {
            isCompleted = true;
        }
    }
}
