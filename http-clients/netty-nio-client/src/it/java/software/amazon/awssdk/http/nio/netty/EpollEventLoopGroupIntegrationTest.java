/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.netty;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import javax.xml.ws.handler.Handler;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;

@RunWith(MockitoJUnitRunner.class)
public final class EpollEventLoopGroupIntegrationTest extends NettyIntegrationTestBase {

    @Rule
    public final WireMockRule mockServer = new WireMockRule(0);

    @Mock
    private SdkRequestContext requestContext;

    @Test
    public void canUseEpollWithoutSpuriousChannelInactive() {
//        Assume.assumeTrue(Epoll.isAvailable());
        SdkAsyncHttpClient client = NettySdkHttpClientFactory.builder()
                                                             .eventLoopGroupConfiguration(b -> b.eventLoopGroup(new NioEventLoopGroup()))
                                                             .build().createHttpClient();

        SdkHttpRequest request = createRequest(URI.create("http://localhost:" + mockServer.port()));

        mockServer.stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withBody("Retry").withHeader("Connection", "Close")));

        RecordingResponseHandler responseHandler = new RecordingResponseHandler();

        AbortableRunnable runnable = client.prepareRequest(request, requestContext, createProvider(""), responseHandler);

        runnable.run();

        responseHandler.completeFuture.join();
    }
}
