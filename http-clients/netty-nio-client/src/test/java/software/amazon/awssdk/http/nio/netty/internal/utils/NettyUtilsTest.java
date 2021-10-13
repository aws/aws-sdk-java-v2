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

package software.amazon.awssdk.http.nio.netty.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.channel.Channel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import javax.net.ssl.SSLEngine;
import org.junit.Test;
import software.amazon.awssdk.http.nio.netty.internal.MockChannel;

public class NettyUtilsTest {
    @Test
    public void testGetOrCreateAttributeKey_calledTwiceWithSameName_returnsSameInstance() {
        String attr = "NettyUtilsTest.Foo";
        AttributeKey<String> fooAttr = NettyUtils.getOrCreateAttributeKey(attr);
        assertThat(NettyUtils.getOrCreateAttributeKey(attr)).isSameAs(fooAttr);
    }

    @Test
    public void newSslHandler_sslEngineShouldBeConfigured() throws Exception {
        SslContext sslContext = SslContextBuilder.forClient().build();
        Channel channel = null;
        try {
            channel = new MockChannel();
            SslHandler sslHandler = NettyUtils.newSslHandler(sslContext, channel.alloc(), "localhost", 80);
            SSLEngine engine = sslHandler.engine();
            assertThat(engine.getSSLParameters().getEndpointIdentificationAlgorithm()).isEqualTo("HTTPS");
        } finally {
            if (channel != null) {
                channel.close();
            }
        }
    }

    @Test
    public void doInEventLoop_inEventLoop_doesNotSubmit() {
        EventExecutor mockExecutor = mock(EventExecutor.class);
        when(mockExecutor.inEventLoop()).thenReturn(true);

        NettyUtils.doInEventLoop(mockExecutor, () -> {});
        verify(mockExecutor, never()).submit(any(Runnable.class));
    }

    @Test
    public void doInEventLoop_notInEventLoop_submits() {
        EventExecutor mockExecutor = mock(EventExecutor.class);
        when(mockExecutor.inEventLoop()).thenReturn(false);

        NettyUtils.doInEventLoop(mockExecutor, () -> {});
        verify(mockExecutor).submit(any(Runnable.class));
    }
}
