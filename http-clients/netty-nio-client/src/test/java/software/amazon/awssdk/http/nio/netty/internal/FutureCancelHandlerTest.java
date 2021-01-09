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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.EXECUTION_ID_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.REQUEST_CONTEXT_KEY;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.util.DefaultAttributeMap;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

/**
 * Unit tests for {@link FutureCancelHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class FutureCancelHandlerTest {

    private FutureCancelHandler handler = FutureCancelHandler.getInstance();

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private Channel channel;

    @Mock
    private SdkChannelPool channelPool;

    private RequestContext requestContext;

    @Mock
    private SdkAsyncHttpResponseHandler responseHandler;

    @Mock
    private EventLoopGroup eventLoopGroup;

    @Before
    public void methodSetup() {
        requestContext = new RequestContext(channelPool,
                                            eventLoopGroup,
                                            AsyncExecuteRequest.builder().responseHandler(responseHandler).build(),
                                            null);

        DefaultAttributeMap attrMap = new DefaultAttributeMap();
        attrMap.attr(EXECUTION_ID_KEY).set(1L);
        attrMap.attr(REQUEST_CONTEXT_KEY).set(requestContext);

        when(ctx.channel()).thenReturn(channel);
        when(channel.attr(EXECUTION_ID_KEY)).thenReturn(attrMap.attr(EXECUTION_ID_KEY));
        when(channel.attr(REQUEST_CONTEXT_KEY)).thenReturn(attrMap.attr(REQUEST_CONTEXT_KEY));
    }

    @Test
    public void surfacesCancelExceptionAsIOException() {
        FutureCancelledException cancelledException = new FutureCancelledException(1L, new CancellationException());
        ArgumentCaptor<Throwable> exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);

        handler.exceptionCaught(ctx, cancelledException);

        verify(ctx).fireExceptionCaught(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue()).isInstanceOf(IOException.class);
    }

    @Test
    public void forwardsExceptionIfNotCancelledException() {
        ArgumentCaptor<Throwable> exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);

        Throwable err = new RuntimeException("some other exception");
        handler.exceptionCaught(ctx, err);

        verify(ctx).fireExceptionCaught(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue()).isEqualTo(err);
    }
}
