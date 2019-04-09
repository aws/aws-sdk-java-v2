/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS;

import io.netty.handler.ssl.SslProvider;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.http.nio.netty.internal.AwaitCloseChannelPoolMap.SimpleChannelPoolAwareChannelPool;

public class AwaitCloseChannelPoolMapTest {

    private static AwaitCloseChannelPoolMap channelPoolMap;


    @BeforeClass
    public static void setup() {
        channelPoolMap = AwaitCloseChannelPoolMap.builder()
                                                 .sdkChannelOptions(new SdkChannelOptions())
                                                 .sdkEventLoopGroup(SdkEventLoopGroup.builder().build())
                                                 .configuration(new NettyConfiguration(GLOBAL_HTTP_DEFAULTS))
                                                 .protocol(Protocol.HTTP1_1)
                                                 .maxStreams(100)
                                                 .sslProvider(SslProvider.OPENSSL)
                                                 .build();
    }

    @Test
    public void close_underlyingPoolsShouldBeClosed() throws ExecutionException, InterruptedException {

        int numberOfChannelPools = 5;
        List<SimpleChannelPoolAwareChannelPool> channelPools = new ArrayList<>();

        for (int i = 0; i < numberOfChannelPools; i++) {
            channelPools.add(
                channelPoolMap.get(URI.create("http://" + RandomStringUtils.randomAlphabetic(2) + i + "localhost:" + numberOfChannelPools)));
        }

        assertThat(channelPoolMap.pools().size()).isEqualTo(numberOfChannelPools);

        channelPoolMap.close();
        channelPools.forEach(channelPool -> {
            assertThat(channelPool.underlyingSimpleChannelPool().closeFuture()).isDone();
            assertThat(channelPool.underlyingSimpleChannelPool().closeFuture().join()).isTrue();
        });
    }

}
