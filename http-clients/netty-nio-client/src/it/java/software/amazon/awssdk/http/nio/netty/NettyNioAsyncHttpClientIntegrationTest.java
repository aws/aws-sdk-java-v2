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
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.StringUtils.reverse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.URI;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Condition;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

@RunWith(MockitoJUnitRunner.class)
public class NettyNioAsyncHttpClientIntegrationTest extends NettyIntegrationTestBase {

    @Rule
    public WireMockRule mockServer = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

    @Mock
    private SdkRequestContext requestContext;

    private static SdkAsyncHttpClient client = NettySdkHttpClientFactory.builder()
                                                                        .trustAllCertificates(true)
                                                                        .build()
                                                                        .createHttpClient();

    @AfterClass
    public static void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void customFactoryIsUsed() throws Exception {
        ThreadFactory threadFactory = spy(new CustomThreadFactory());
        EventLoopGroupConfiguration eventLoopGroupConfiguration =
                EventLoopGroupConfiguration.builder()
                                           .eventLoopGroupFactory(DefaultEventLoopGroupFactory.builder()
                                                                                              .threadFactory(threadFactory)
                                                                                              .build())
                                           .build();
        SdkAsyncHttpClient customClient =
                NettySdkHttpClientFactory.builder()
                                         .trustAllCertificates(true)
                                         .eventLoopGroupConfiguration(eventLoopGroupConfiguration)
                                         .build()
                                         .createHttpClient();

        makeSimpleRequest(customClient);
        customClient.close();

        Mockito.verify(threadFactory, atLeastOnce()).newThread(Mockito.any());
    }

    @Test
    public void defaultThreadFactoryUsesHelpfulName() throws Exception {
        // Make a request to ensure a thread is primed
        makeSimpleRequest(client);

        String expectedPattern = "aws-java-sdk-NettyEventLoop-\\d+-\\d+";
        assertThat(Thread.getAllStackTraces().keySet())
                .areAtLeast(1, new Condition<>(t -> t.getName().matches(expectedPattern),
                                               "Matches default thread pattern: `%s`", expectedPattern));
    }

    @Test
    public void customThreadCountIsRespected() throws Exception {
        final int threadCount = 10;
        ThreadFactory threadFactory = spy(new CustomThreadFactory());
        EventLoopGroupConfiguration eventLoopGroupConfiguration =
                EventLoopGroupConfiguration.builder()
                                           .eventLoopGroupFactory(DefaultEventLoopGroupFactory.builder()
                                                                                              .threadFactory(threadFactory)
                                                                                              .numberOfThreads(threadCount)
                                                                                              .build())
                                           .build();
        SdkAsyncHttpClient customClient =
                NettySdkHttpClientFactory.builder()
                                         .trustAllCertificates(true)
                                         .eventLoopGroupConfiguration(eventLoopGroupConfiguration)
                                         .build()
                                         .createHttpClient();

        // Have to make enough requests to prime the threads
        for (int i = 0; i < threadCount + 1; i++) {
            makeSimpleRequest(customClient);
        }
        customClient.close();

        Mockito.verify(threadFactory, times(threadCount)).newThread(Mockito.any());
    }

    @Test
    public void customEventLoopGroup_NotClosedWhenClientIsClosed() throws Exception {

        ThreadFactory threadFactory = spy(new CustomThreadFactory());
        // Cannot use DefaultEventLoopGroupFactory because the concrete
        // implementation it creates is platform-dependent and could be a final
        // (i.e. non-spyable) class.
        EventLoopGroup eventLoopGroup = spy(new NioEventLoopGroup(0, threadFactory));
        EventLoopGroupConfiguration eventLoopGroupConfiguration =
                EventLoopGroupConfiguration.builder()
                                           .eventLoopGroup(eventLoopGroup)
                                           .build();
        SdkAsyncHttpClient customClient =
                NettySdkHttpClientFactory.builder()
                                         .trustAllCertificates(true)
                                         .eventLoopGroupConfiguration(eventLoopGroupConfiguration)
                                         .build()
                                         .createHttpClient();

        makeSimpleRequest(customClient);
        customClient.close();

        Mockito.verify(threadFactory, atLeastOnce()).newThread(Mockito.any());
        Mockito.verify(eventLoopGroup, never()).shutdownGracefully();
    }

    /**
     * Make a simple async request and wait for it to fiish.
     *
     * @param client Client to make request with.
     */
    private void makeSimpleRequest(SdkAsyncHttpClient client) throws Exception {
        String body = randomAlphabetic(10);
        URI uri = URI.create("http://localhost:" + mockServer.port());
        stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withBody(body)));
        SdkHttpRequest request = createRequest(uri);
        RecordingResponseHandler recorder = new RecordingResponseHandler();
        client.prepareRequest(request, requestContext, createProvider(""), recorder).run();
        recorder.completeFuture.get(5, TimeUnit.SECONDS);
    }

    @Test
    public void canMakeBasicRequestOverHttp() throws Exception {
        String smallBody = randomAlphabetic(10);
        URI uri = URI.create("http://localhost:" + mockServer.port());

        assertCanReceiveBasicRequest(uri, smallBody);
    }

    @Test
    public void canMakeBasicRequestOverHttps() throws Exception {
        String smallBody = randomAlphabetic(10);
        URI uri = URI.create("https://localhost:" + mockServer.httpsPort());

        assertCanReceiveBasicRequest(uri, smallBody);
    }

    @Test
    public void canHandleLargerPayloadsOverHttp() throws Exception {
        String largishBody = randomAlphabetic(25000);

        URI uri = URI.create("http://localhost:" + mockServer.port());

        assertCanReceiveBasicRequest(uri, largishBody);
    }

    @Test
    public void canHandleLargerPayloadsOverHttps() throws Exception {
        String largishBody = randomAlphabetic(25000);

        URI uri = URI.create("https://localhost:" + mockServer.httpsPort());

        assertCanReceiveBasicRequest(uri, largishBody);
    }

    @Test
    public void canSendContentAndGetThatContentBack() throws Exception {
        String body = randomAlphabetic(50);
        stubFor(any(urlEqualTo("/echo?reversed=true"))
                        .withRequestBody(equalTo(body))
                        .willReturn(aResponse().withBody(reverse(body))));
        URI uri = URI.create("http://localhost:" + mockServer.port());

        SdkHttpRequest request = createRequest(uri, "/echo", body, SdkHttpMethod.POST, singletonMap("reversed", "true"));

        RecordingResponseHandler recorder = new RecordingResponseHandler();
        client.prepareRequest(request, requestContext, createProvider(body), recorder).run();

        recorder.completeFuture.get(5, TimeUnit.SECONDS);

        verify(1, postRequestedFor(urlEqualTo("/echo?reversed=true")));

        assertThat(recorder.fullResponseAsString()).isEqualTo(reverse(body));
    }

    private void assertCanReceiveBasicRequest(URI uri, String body) throws Exception {
        stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withHeader("Some-Header", "With Value").withBody(body)));

        SdkHttpRequest request = createRequest(uri);

        RecordingResponseHandler recorder = new RecordingResponseHandler();
        client.prepareRequest(request, requestContext, createProvider(""), recorder).run();

        recorder.completeFuture.get(5, TimeUnit.SECONDS);

        assertThat(recorder.responses).hasOnlyOneElementSatisfying(
                headerResponse -> {
                    assertThat(headerResponse.headers()).containsKey("Some-Header");
                    assertThat(headerResponse.statusCode()).isEqualTo(200);
                });

        assertThat(recorder.fullResponseAsString()).isEqualTo(body);
        verify(1, getRequestedFor(urlMatching("/")));
    }

    // Needs to be a non-anon class in order to spy
    public static class CustomThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r);
        }
    }
}