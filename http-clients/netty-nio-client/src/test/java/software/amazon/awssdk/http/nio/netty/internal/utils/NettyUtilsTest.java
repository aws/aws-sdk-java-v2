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

import io.netty.channel.Channel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
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
}
