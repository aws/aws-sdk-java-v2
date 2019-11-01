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


package software.amazon.awssdk.http.nio.netty;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

/**
 * Tests for HTTP proxy functionality in the Netty client.
 */
public class ProxyWireMockTest {
    private static SdkAsyncHttpClient client;

    private static ProxyConfiguration proxyCfg;

    private static WireMockServer mockServer = new WireMockServer(new WireMockConfiguration()
            .dynamicPort()
            .dynamicHttpsPort());

    private static WireMockServer mockProxy = new WireMockServer(new WireMockConfiguration()
            .dynamicPort()
            .dynamicHttpsPort());

    @BeforeClass
    public static void setup() {
        mockProxy.start();
        mockServer.start();

        mockServer.stubFor(get(urlPathEqualTo("/")).willReturn(aResponse().withStatus(200).withBody("hello")));

        proxyCfg = ProxyConfiguration.builder()
                .host("localhost")
                .port(mockProxy.port())
                .build();
    }

    @AfterClass
    public static void teardown() {
        mockServer.stop();
        mockProxy.stop();
    }

    @After
    public void methodTeardown() {
        if (client != null) {
            client.close();
        }
        client = null;
    }

    @Test(expected = IOException.class)
    public void proxyConfigured_attemptsToConnect() throws Throwable {
        AsyncExecuteRequest req = AsyncExecuteRequest.builder()
                .request(testSdkRequest())
                .responseHandler(mock(SdkAsyncHttpResponseHandler.class))
                .build();

        client = NettyNioAsyncHttpClient.builder()
                .proxyConfiguration(proxyCfg)
                .build();

        try {
            client.execute(req).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            // WireMock doesn't allow for mocking the CONNECT method so it will just return a 404, causing the client
            // to throw an exception.
            assertThat(e.getCause().getMessage()).isEqualTo("Could not connect to proxy");
            throw cause;
        }
    }

    @Test
    public void proxyConfigured_hostInNonProxySet_doesNotConnect() {
        RecordingResponseHandler responseHandler = new RecordingResponseHandler();
        AsyncExecuteRequest req = AsyncExecuteRequest.builder()
                .request(testSdkRequest())
                .responseHandler(responseHandler)
                .requestContentPublisher(new EmptyPublisher())
                .build();

        ProxyConfiguration cfg = proxyCfg.toBuilder()
                .nonProxyHosts(Stream.of("localhost").collect(Collectors.toSet()))
                .build();

        client = NettyNioAsyncHttpClient.builder()
                .proxyConfiguration(cfg)
                .build();

        client.execute(req).join();

        responseHandler.completeFuture.join();
        assertThat(responseHandler.fullResponseAsString()).isEqualTo("hello");
    }

    private SdkHttpFullRequest testSdkRequest() {
        return SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.GET)
                .protocol("http")
                .host("localhost")
                .port(mockServer.port())
                .putHeader("host", "localhost")
                .build();
    }
}
