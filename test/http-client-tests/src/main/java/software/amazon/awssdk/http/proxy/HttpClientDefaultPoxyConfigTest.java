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

package software.amazon.awssdk.http.proxy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.EmptyPublisher;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.Pair;

public abstract class HttpClientDefaultPoxyConfigTest {

    static Random random = new Random();

    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    protected WireMockServer mockProxy = new WireMockServer(new WireMockConfiguration()
                                                                .dynamicPort()
                                                                .dynamicHttpsPort()
                                                                .enableBrowserProxying(true));

    protected WireMockServer mockServer = new WireMockServer(new WireMockConfiguration()
                                                                 .dynamicPort()
                                                                 .dynamicHttpsPort());

    protected abstract boolean isSyncClient();

    protected abstract SdkAsyncHttpClient createHttpClientWithDefaultProxy();

    protected abstract SdkHttpClient createSyncHttpClientWithDefaultProxy();

    protected abstract Class<? extends Exception> getProxyFailedExceptionType();

    protected abstract Class<? extends Exception> getProxyFailedCauseExceptionType();


    @BeforeEach
    public void setup() {
        mockProxy.start();
        mockServer.start();
        mockServer.stubFor(get(WireMock.urlMatching(".*"))
                               .willReturn(aResponse().withStatus(200).withBody("hello")));
    }

    @AfterEach
    public void teardown() {
        mockServer.stop();
        mockProxy.stop();
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
    }

    public static Stream<Arguments> proxyConfigurationSettingWithConnectionFails() {
        return Stream.of(
            Arguments.of(
                Arrays.asList(
                    Pair.of("http.proxyHost", "localhost"),
                    Pair.of("http.proxyPort", "%s")),
                Arrays.asList(
                    Pair.of("http_proxy",
                            "http://" + "localhost" + ":" + "%s" + "/")),
                "Provided system and environment variable when configured uses proxy config"),

            Arguments.of(Collections.singletonList(
                             Pair.of("http.none", "localhost")),
                         Arrays.asList(
                             Pair.of("http_proxy",
                                     "http://" + "localhost" + ":" + "%s" + "/")),
                         "Provided environment  and No system variables uses proxy config"),
            Arguments.of(
                Arrays.asList(
                    Pair.of("http.proxyHost", "localhost"),
                    Pair.of("http.proxyPort", "%s")),
                Arrays.asList(Pair.of("none", "none")),
                "Provided system  and No environment variables uses proxy config")
        );
    }

    @ParameterizedTest(name = "{index} - {2}.")
    @MethodSource("proxyConfigurationSettingWithConnectionFails")
    public void ensureProxyErrorsWhenIncorrectPortUsed(List<Pair<String, String>> systemSettingsPair,
                                                       List<Pair<String, String>> envSystemSetting,
                                                       String testCaseName) throws Throwable {
        systemSettingsPair.forEach(settingsPair -> System.setProperty(settingsPair.left(),

                                                                      String.format(settingsPair.right(),
                                                                                    getRandomPort(mockProxy.port()))));

        envSystemSetting.forEach(settingsPair -> ENVIRONMENT_VARIABLE_HELPER.set(
            settingsPair.left(),
            String.format(settingsPair.right(), getRandomPort(mockProxy.port()))));
        if (isSyncClient()) {
            defaultProxyConfigurationSyncHttp(createSyncHttpClientWithDefaultProxy(),
                                              getProxyFailedExceptionType(),
                                              getProxyFailedCauseExceptionType());
        } else {
            defaultProxyConfigurationForAsyncHttp(createHttpClientWithDefaultProxy(),
                                                  getProxyFailedExceptionType(),
                                                  getProxyFailedCauseExceptionType());
        }
    }

    @Test
    public void ensureProxySucceedsWhenIncorrectPortUsed() throws Throwable {
        if (isSyncClient()) {
            defaultProxyConfigurationSyncHttp(createSyncHttpClientWithDefaultProxy(),
                                              null,
                                              null);
        } else {
            defaultProxyConfigurationForAsyncHttp(createHttpClientWithDefaultProxy(),
                                                  null,
                                                  null);
        }
    }

    public void defaultProxyConfigurationForAsyncHttp(SdkAsyncHttpClient client,
                                                      Class<? extends Exception> proxyFailedExceptionType,
                                                      Class<? extends Exception> proxyFailedCauseExceptionType) throws Throwable {
        CompletableFuture<Boolean> streamReceived = new CompletableFuture<>();
        AtomicReference<SdkHttpResponse> response = new AtomicReference<>(null);
        AtomicReference<Throwable> error = new AtomicReference<>(null);
        Subscriber<ByteBuffer> subscriber = createDummySubscriber();
        SdkAsyncHttpResponseHandler handler = createTestResponseHandler(response, streamReceived, error, subscriber);
        URI uri = URI.create("http://localhost:" + mockServer.port());
        SdkHttpRequest request = createRequest(uri, "/server/test", null, SdkHttpMethod.GET, emptyMap());
        CompletableFuture future = client.execute(AsyncExecuteRequest.builder()
                                                                     .request(request)
                                                                     .responseHandler(handler)
                                                                     .requestContentPublisher(new EmptyPublisher())
                                                                     .build());

        if (proxyFailedExceptionType != null && proxyFailedCauseExceptionType != null) {
            assertThatExceptionOfType(proxyFailedExceptionType).isThrownBy(() -> future.get(60, TimeUnit.SECONDS))
                                                               .withCauseInstanceOf(proxyFailedCauseExceptionType);
        } else {
            future.get(60, TimeUnit.SECONDS);
            assertThat(error.get()).isNull();
            assertThat(streamReceived.get(60, TimeUnit.SECONDS)).isTrue();
            assertThat(response.get().statusCode()).isEqualTo(200);
        }


    }


    public void defaultProxyConfigurationSyncHttp(SdkHttpClient client, Class<? extends Exception> exceptionType,
                                                  Class<? extends Exception> proxyFailedCauseExceptionType) throws Throwable {
        CompletableFuture<Boolean> streamReceived = new CompletableFuture<>();
        AtomicReference<SdkHttpResponse> response = new AtomicReference<>(null);
        AtomicReference<Throwable> error = new AtomicReference<>(null);
        Subscriber<ByteBuffer> subscriber = createDummySubscriber();
        SdkAsyncHttpResponseHandler handler = createTestResponseHandler(response, streamReceived, error, subscriber);
        URI uri = URI.create("http://localhost:" + mockServer.port());
        SdkHttpRequest request = createRequest(uri, "/server/test", null, SdkHttpMethod.GET, emptyMap());
        ExecutableHttpRequest executableHttpRequest = client.prepareRequest(HttpExecuteRequest.builder()
                                                                                              .request(request)
                                                                                              .build());

        if (exceptionType != null) {
            if (proxyFailedCauseExceptionType != null) {
                assertThatExceptionOfType(exceptionType).isThrownBy(() -> executableHttpRequest.call())
                                                        .withCauseInstanceOf(proxyFailedCauseExceptionType);
            } else {

                assertThatExceptionOfType(exceptionType).isThrownBy(() -> executableHttpRequest.call());
            }
        } else {
            HttpExecuteResponse executeResponse = executableHttpRequest.call();
            assertThat(error.get()).isNull();
            assertThat(executeResponse.httpResponse().statusCode()).isEqualTo(200);
        }


    }

    static Subscriber<ByteBuffer> createDummySubscriber() {
        return new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        };
    }

    static SdkAsyncHttpResponseHandler createTestResponseHandler(AtomicReference<SdkHttpResponse> response,
                                                                 CompletableFuture<Boolean> streamReceived,
                                                                 AtomicReference<Throwable> error,
                                                                 Subscriber<ByteBuffer> subscriber) {
        return new SdkAsyncHttpResponseHandler() {
            @Override
            public void onHeaders(SdkHttpResponse headers) {
                response.compareAndSet(null, headers);
            }

            @Override
            public void onStream(Publisher<ByteBuffer> stream) {
                stream.subscribe(subscriber);
                streamReceived.complete(true);
            }

            @Override
            public void onError(Throwable t) {
                error.compareAndSet(null, t);
            }
        };
    }

    static SdkHttpFullRequest createRequest(URI endpoint,
                                            String resourcePath,
                                            byte[] body,
                                            SdkHttpMethod method,
                                            Map<String, String> params) {
        String contentLength = (body == null) ? null : String.valueOf(body.length);
        return SdkHttpFullRequest.builder()
                                 .uri(endpoint)
                                 .method(method)
                                 .encodedPath(resourcePath)
                                 .applyMutation(b -> params.forEach(b::putRawQueryParameter))
                                 .applyMutation(b -> {
                                     b.putHeader("Host", endpoint.getHost());
                                     if (contentLength != null) {
                                         b.putHeader("Content-Length", contentLength);
                                     }
                                 }).build();
    }

    private int getRandomPort(int currentPort) {
        int randomPort;
        do {
            randomPort = random.nextInt(65535);
        } while (randomPort == currentPort);
        return randomPort;
    }

}
