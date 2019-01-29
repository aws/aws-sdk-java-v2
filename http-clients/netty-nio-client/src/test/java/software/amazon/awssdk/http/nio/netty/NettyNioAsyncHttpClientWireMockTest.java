/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.reverse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.http.trafficlistener.WiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.assertj.core.api.Condition;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration;
import software.amazon.awssdk.http.nio.netty.internal.SdkChannelOptions;
import software.amazon.awssdk.http.nio.netty.internal.SdkChannelPoolMap;
import software.amazon.awssdk.utils.AttributeMap;

@RunWith(MockitoJUnitRunner.class)
public class NettyNioAsyncHttpClientWireMockTest {

    private final RecordingNetworkTrafficListener wiremockTrafficListener = new RecordingNetworkTrafficListener();

    @Rule
    public WireMockRule mockServer = new WireMockRule(wireMockConfig()
            .dynamicPort()
            .dynamicHttpsPort()
            .networkTrafficListener(wiremockTrafficListener));

    private static SdkAsyncHttpClient client = NettyNioAsyncHttpClient.builder().buildWithDefaults(mapWithTrustAllCerts());

    @Before
    public void methodSetup() {
        wiremockTrafficListener.reset();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void customFactoryIsUsed() throws Exception {
        ThreadFactory threadFactory = spy(new CustomThreadFactory());
        SdkAsyncHttpClient customClient =
            NettyNioAsyncHttpClient.builder()
                                   .eventLoopGroupBuilder(SdkEventLoopGroup.builder()
                                                                           .threadFactory(threadFactory))
                                   .build();

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
        SdkAsyncHttpClient customClient =
                NettyNioAsyncHttpClient.builder()
                                       .eventLoopGroupBuilder(SdkEventLoopGroup.builder()
                                                                               .threadFactory(threadFactory)
                                                                               .numberOfThreads(threadCount))
                                       .build();

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
        SdkAsyncHttpClient customClient =
                NettyNioAsyncHttpClient.builder()
                                       .eventLoopGroup(SdkEventLoopGroup.create(eventLoopGroup, NioSocketChannel::new))
                                       .build();

        makeSimpleRequest(customClient);
        customClient.close();

        Mockito.verify(threadFactory, atLeastOnce()).newThread(Mockito.any());
        Mockito.verify(eventLoopGroup, never()).shutdownGracefully();
    }

    @Test
    public void customChannelFactoryIsUsed() throws Exception {

        ChannelFactory channelFactory = mock(ChannelFactory.class);

        when(channelFactory.newChannel()).thenAnswer((Answer<NioSocketChannel>) invocationOnMock -> new NioSocketChannel());

        SdkAsyncHttpClient customClient =
            NettyNioAsyncHttpClient.builder()
                                   .eventLoopGroup(SdkEventLoopGroup.create(new NioEventLoopGroup(), channelFactory))
                                   .build();

        makeSimpleRequest(customClient);
        customClient.close();

        Mockito.verify(channelFactory, atLeastOnce()).newChannel();
    }

    @Test
    public void closeClient_shouldCloseUnderlyingResources() {
        SdkEventLoopGroup eventLoopGroup = SdkEventLoopGroup.builder().build();
        ChannelPool channelPool = mock(ChannelPool.class);
        SdkChannelPoolMap<URI, ChannelPool> sdkChannelPoolMap = new SdkChannelPoolMap<URI, ChannelPool>() {
            @Override
            protected ChannelPool newPool(URI key) {
                return channelPool;
            }
        };

        sdkChannelPoolMap.get(URI.create("http://blah"));
        SdkChannelOptions channelOptions = new SdkChannelOptions();
        NettyConfiguration nettyConfiguration = new NettyConfiguration(AttributeMap.empty());

        SdkAsyncHttpClient client = new NettyNioAsyncHttpClient(eventLoopGroup, sdkChannelPoolMap, channelOptions, nettyConfiguration, 1);

        client.close();
        assertThat(eventLoopGroup.eventLoopGroup().isShuttingDown()).isTrue();
        assertThat(sdkChannelPoolMap).isEmpty();
        Mockito.verify(channelPool).close();
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
        client.execute(AsyncExecuteRequest.builder().request(request).requestContentPublisher(createProvider("")).responseHandler(recorder).build());
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
        client.execute(AsyncExecuteRequest.builder().request(request).requestContentPublisher(createProvider(body)).responseHandler(recorder).build());

        recorder.completeFuture.get(5, TimeUnit.SECONDS);

        verify(1, postRequestedFor(urlEqualTo("/echo?reversed=true")));

        assertThat(recorder.fullResponseAsString()).isEqualTo(reverse(body));
    }

    @Test
    public void requestContentOnlyEqualToContentLengthHeaderFromProvider() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        final String content = randomAlphabetic(32);
        final String streamContent = content + reverse(content);
        stubFor(any(urlEqualTo("/echo?reversed=true"))
                .withRequestBody(equalTo(content))
                .willReturn(aResponse().withBody(reverse(content))));
        URI uri = URI.create("http://localhost:" + mockServer.port());

        SdkHttpFullRequest request = createRequest(uri, "/echo", streamContent, SdkHttpMethod.POST, singletonMap("reversed", "true"));
        request = request.toBuilder().putHeader("Content-Length", Integer.toString(content.length())).build();
        RecordingResponseHandler recorder = new RecordingResponseHandler();

        client.execute(AsyncExecuteRequest.builder().request(request).requestContentPublisher(createProvider(streamContent)).responseHandler(recorder).build());

        recorder.completeFuture.get(5, TimeUnit.SECONDS);

        // HTTP servers will stop processing the request as soon as it reads
        // bytes equal to 'Content-Length' so we need to inspect the raw
        // traffic to ensure that there wasn't anything after that.
        assertThat(wiremockTrafficListener.requests.toString()).endsWith(content);
    }

    @Test
    public void closeMethodClosesOpenedChannels() throws InterruptedException, TimeoutException, ExecutionException {
        String body = randomAlphabetic(10);
        URI uri = URI.create("https://localhost:" + mockServer.httpsPort());
        stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withHeader("Some-Header", "With Value").withBody(body)));

        SdkHttpFullRequest request = createRequest(uri, "/", body, SdkHttpMethod.POST, Collections.emptyMap());
        RecordingResponseHandler recorder = new RecordingResponseHandler();

        CompletableFuture<Boolean> channelClosedFuture = new CompletableFuture<>();
        ChannelFactory<NioSocketChannel> channelFactory = new ChannelFactory<NioSocketChannel>() {
            @Override
            public NioSocketChannel newChannel() {
                return new NioSocketChannel() {
                    @Override
                    public ChannelFuture close() {
                        ChannelFuture cf = super.close();
                        channelClosedFuture.complete(true);
                        return cf;
                    }
                };
            }
        };

        SdkAsyncHttpClient customClient = NettyNioAsyncHttpClient.builder()
                .eventLoopGroup(new SdkEventLoopGroup(new NioEventLoopGroup(1), channelFactory))
                .buildWithDefaults(mapWithTrustAllCerts());

        try {
            customClient.execute(AsyncExecuteRequest.builder()
                    .request(request)
                    .requestContentPublisher(createProvider(body))
                    .responseHandler(recorder).build())
                    .join();
        } finally {
            customClient.close();
        }

        assertThat(channelClosedFuture.get(5, TimeUnit.SECONDS)).isTrue();
    }

    private void assertCanReceiveBasicRequest(URI uri, String body) throws Exception {
        stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withHeader("Some-Header", "With Value").withBody(body)));

        SdkHttpRequest request = createRequest(uri);

        RecordingResponseHandler recorder = new RecordingResponseHandler();
        client.execute(AsyncExecuteRequest.builder().request(request).requestContentPublisher(createProvider("")).responseHandler(recorder).build());

        recorder.completeFuture.get(5, TimeUnit.SECONDS);

        assertThat(recorder.responses).hasOnlyOneElementSatisfying(
                headerResponse -> {
                    assertThat(headerResponse.headers()).containsKey("Some-Header");
                    assertThat(headerResponse.statusCode()).isEqualTo(200);
                });

        assertThat(recorder.fullResponseAsString()).isEqualTo(body);
        verify(1, getRequestedFor(urlMatching("/")));
    }

    private SdkHttpContentPublisher createProvider(String body) {
        Stream<ByteBuffer> chunks = splitStringBySize(body).stream()
                                                           .map(chunk -> ByteBuffer.wrap(chunk.getBytes(UTF_8)));
        return new SdkHttpContentPublisher() {

            @Override
            public Optional<Long> contentLength() {
                return Optional.of(Long.valueOf(body.length()));
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                s.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        chunks.forEach(s::onNext);
                        s.onComplete();
                    }

                    @Override
                    public void cancel() {

                    }
                });
            }
        };
    }

    private SdkHttpFullRequest createRequest(URI uri) {
        return createRequest(uri, "/", null, SdkHttpMethod.GET, emptyMap());
    }

    private SdkHttpFullRequest createRequest(URI uri,
                                         String resourcePath,
                                         String body,
                                         SdkHttpMethod method,
                                         Map<String, String> params) {
        String contentLength = body == null ? null : String.valueOf(body.getBytes(UTF_8).length);
        return SdkHttpFullRequest.builder()
                                 .uri(uri)
                                 .method(method)
                                 .encodedPath(resourcePath)
                                 .applyMutation(b -> params.forEach(b::putRawQueryParameter))
                                 .applyMutation(b -> {
                                     b.putHeader("Host", uri.getHost());
                                     if (contentLength != null) {
                                         b.putHeader("Content-Length", contentLength);
                                     }
                                 }).build();
    }

    private static Collection<String> splitStringBySize(String str) {
        if (isBlank(str)) {
            return Collections.emptyList();
        }
        ArrayList<String> split = new ArrayList<>();
        for (int i = 0; i <= str.length() / 1000; i++) {
            split.add(str.substring(i * 1000, Math.min((i + 1) * 1000, str.length())));
        }
        return split;
    }

    // Needs to be a non-anon class in order to spy
    public static class CustomThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r);
        }
    }

    @Test
    public void testExceptionMessageChanged_WhenPendingAcquireQueueIsFull() throws Exception {
        String expectedErrorMsg = "Maximum pending connection acquisitions exceeded.";

        SdkAsyncHttpClient customClient = NettyNioAsyncHttpClient.builder()
                                                                 .maxConcurrency(1)
                                                                 .maxPendingConnectionAcquires(1)
                                                                 .build();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(makeSimpleRequestAndReturnResponseHandler(customClient).completeFuture);
        }

        assertThatThrownBy(() -> CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join())
            .hasMessageContaining(expectedErrorMsg);

        customClient.close();
    }


    @Test
    public void testExceptionMessageChanged_WhenConnectionTimeoutErrorEncountered() throws Exception {
        String expectedErrorMsg = "Acquire operation took longer than the configured maximum time. This indicates that a request "
                                  + "cannot get a connection from the pool within the specified maximum time.";

        SdkAsyncHttpClient customClient = NettyNioAsyncHttpClient.builder()
                                                                 .maxConcurrency(1)
                                                                 .connectionTimeout(Duration.ofMillis(1))
                                                                 .connectionAcquisitionTimeout(Duration.ofMillis(1))
                                                                 .build();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(makeSimpleRequestAndReturnResponseHandler(customClient).completeFuture);
        }

        assertThatThrownBy(() -> CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join())
            .hasMessageContaining(expectedErrorMsg);

        customClient.close();
    }

    private RecordingResponseHandler makeSimpleRequestAndReturnResponseHandler(SdkAsyncHttpClient client) throws Exception {
        String body = randomAlphabetic(10);
        URI uri = URI.create("http://localhost:" + mockServer.port());
        stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withBody(body).withFixedDelay(1000)));
        SdkHttpRequest request = createRequest(uri);
        RecordingResponseHandler recorder = new RecordingResponseHandler();
        client.execute(AsyncExecuteRequest.builder()
                                          .request(request)
                                          .requestContentPublisher(createProvider(""))
                                          .responseHandler(recorder)
                                          .build());
        return recorder;
    }

    private static AttributeMap mapWithTrustAllCerts() {
        return AttributeMap.builder()
                           .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                           .build();
    }

    private static class RecordingNetworkTrafficListener implements WiremockNetworkTrafficListener {
        private final StringBuilder requests = new StringBuilder();


        @Override
        public void opened(Socket socket) {

        }

        @Override
        public void incoming(Socket socket, ByteBuffer byteBuffer) {
            requests.append(StandardCharsets.UTF_8.decode(byteBuffer));
        }

        @Override
        public void outgoing(Socket socket, ByteBuffer byteBuffer) {

        }

        @Override
        public void closed(Socket socket) {

        }

        public void reset() {
            requests.setLength(0);
        }
    }
}
