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

package software.amazon.awssdk.http.nio.netty.fault;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.utils.AttributeMap;

import software.amazon.awssdk.http.H1ServerBehaviorTestBase;


/**
 * Testing the scenario where h1 server sends 5xx errors.
 */
public class H1ServerErrorTest extends H1ServerBehaviorTestBase {
    private SdkAsyncHttpClient netty;

    @Override
    protected SdkAsyncHttpClient getTestClient() { return netty; }

    @Before
    public void setup() throws Exception {
        super.setup();

        netty = NettyNioAsyncHttpClient.builder()
                                       .eventLoopGroup(SdkEventLoopGroup.builder().numberOfThreads(2).build())
                                       .protocol(Protocol.HTTP1_1)
                                       .buildWithDefaults(AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true).build());
    }


    @After
    public void teardown() throws InterruptedException {
        super.teardown();

        if (netty != null) {
            netty.close();
        }
        netty = null;
    }

    @Test
    public void connectionReceive500_shouldNotReuseConnection() {
        assertThat(netty).isNotNull();
        super.connectionReceive500_shouldNotReuseConnection();
    }

    @Test
    public void connectionReceive200_shouldReuseConnection() {
        assertThat(netty).isNotNull();
        super.connectionReceive200_shouldReuseConnection();
    }
}
