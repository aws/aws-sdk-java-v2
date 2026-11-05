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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.EXECUTE_FUTURE_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.KEEP_ALIVE;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PROTOCOL_FUTURE;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.REQUEST_CONTEXT_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.RESPONSE_CONTENT_LENGTH;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.RESPONSE_DATA_READ;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.Flowable;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.BeforeMethod;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.internal.nrs.DefaultStreamedHttpResponse;
import software.amazon.awssdk.http.nio.netty.internal.nrs.StreamedHttpResponse;

/**
 * TCK verification for the Netty response publisher adapter.
 */
public class PublisherAdapterTckTest extends PublisherVerification<ByteBuffer> {
    private CompletableFuture<Void> executeFuture;
    private MockChannel channel;
    private ChannelHandlerContext channelContext;
    private RequestContext requestContext;

    public PublisherAdapterTckTest() {
        super(new TestEnvironment());
    }

    @BeforeMethod
    public void methodSetup() throws Exception {
        executeFuture = new CompletableFuture<>();
        channel = new MockChannel();
        channel.attr(PROTOCOL_FUTURE).set(CompletableFuture.completedFuture(Protocol.HTTP1_1));
        channel.attr(KEEP_ALIVE).set(true);

        SdkChannelPool channelPool = mock(SdkChannelPool.class);
        EventLoopGroup eventLoopGroup = mock(EventLoopGroup.class);
        SdkAsyncHttpResponseHandler responseHandler = mock(SdkAsyncHttpResponseHandler.class);
        requestContext = new RequestContext(channelPool,
                                            eventLoopGroup,
                                            AsyncExecuteRequest.builder()
                                                               .request(SdkHttpRequest.builder()
                                                                                      .uri(URI.create("https://localhost"))
                                                                                      .method(SdkHttpMethod.GET)
                                                                                      .build())
                                                               .responseHandler(responseHandler)
                                                               .build(),
                                            null);

        channel.attr(REQUEST_CONTEXT_KEY).set(requestContext);
        channel.attr(EXECUTE_FUTURE_KEY).set(executeFuture);
        channelContext = mock(ChannelHandlerContext.class);
        when(channelContext.channel()).thenReturn(channel);
    }

    @Override
    public long maxElementsFromPublisher() {
        return 32;
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        Flowable<HttpContent> content = Flowable.range(0, Math.toIntExact(elements))
                                                .map(i -> new DefaultHttpContent(Unpooled.wrappedBuffer(new byte[] {i.byteValue()})));
        StreamedHttpResponse response = new DefaultStreamedHttpResponse(HttpVersion.HTTP_1_1,
                                                                         HttpResponseStatus.OK,
                                                                         content);
        return new ResponseHandler.PublisherAdapter(response, channelContext, requestContext, executeFuture);
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        channel.attr(RESPONSE_CONTENT_LENGTH).set(1L);
        channel.attr(RESPONSE_DATA_READ).set(0L);
        StreamedHttpResponse response = new DefaultStreamedHttpResponse(HttpVersion.HTTP_1_1,
                                                                         HttpResponseStatus.OK,
                                                                         Flowable.empty());
        return new ResponseHandler.PublisherAdapter(response, channelContext, requestContext, executeFuture);
    }
}