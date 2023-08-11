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

package software.amazon.awssdk.http.nio.netty;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.StringUtils.reverse;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClientTestUtils.assertCanReceiveBasicRequest;
import static software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClientTestUtils.createProvider;
import static software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClientTestUtils.createRequest;
import static software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClientTestUtils.makeSimpleRequest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.assertj.core.api.Condition;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

@RunWith(MockitoJUnitRunner.class)
public class NettyNioAsyncHttpClientNonBlockingDnsTest {

    private final RecordingNetworkTrafficListener wiremockTrafficListener = new RecordingNetworkTrafficListener();

    private static final SdkAsyncHttpClient client = NettyNioAsyncHttpClient.builder()
                                                                            .useNonBlockingDnsResolver(true)
                                                                            .buildWithDefaults(
                                                                                AttributeMap.builder()
                                                                                            .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                                                                                            .build());
    @Rule
    public WireMockRule mockServer = new WireMockRule(wireMockConfig()
                                                          .dynamicPort()
                                                          .dynamicHttpsPort()
                                                          .networkTrafficListener(wiremockTrafficListener));

    @Before
    public void methodSetup() {
        wiremockTrafficListener.reset();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void canSendContentAndGetThatContentBackNonBlockingDns() throws Exception {
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
    public void defaultThreadFactoryUsesHelpfulName() throws Exception {
        // Make a request to ensure a thread is primed
        makeSimpleRequest(client, mockServer);

        String expectedPattern = "aws-java-sdk-NettyEventLoop-\\d+-\\d+";
        assertThat(Thread.getAllStackTraces().keySet())
            .areAtLeast(1, new Condition<>(t -> t.getName().matches(expectedPattern),
                                           "Matches default thread pattern: `%s`", expectedPattern));
    }

    @Test
    public void canMakeBasicRequestOverHttp() throws Exception {
        String smallBody = randomAlphabetic(10);
        URI uri = URI.create("http://localhost:" + mockServer.port());

        assertCanReceiveBasicRequest(client, uri, smallBody);
    }

    @Test
    public void canMakeBasicRequestOverHttps() throws Exception {
        String smallBody = randomAlphabetic(10);
        URI uri = URI.create("https://localhost:" + mockServer.httpsPort());

        assertCanReceiveBasicRequest(client, uri, smallBody);
    }

    @Test
    public void canHandleLargerPayloadsOverHttp() throws Exception {
        String largishBody = randomAlphabetic(25000);

        URI uri = URI.create("http://localhost:" + mockServer.port());

        assertCanReceiveBasicRequest(client, uri, largishBody);
    }

    @Test
    public void canHandleLargerPayloadsOverHttps() throws Exception {
        String largishBody = randomAlphabetic(25000);

        URI uri = URI.create("https://localhost:" + mockServer.httpsPort());

        assertCanReceiveBasicRequest(client, uri, largishBody);
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
        assertThat(wiremockTrafficListener.requests().toString()).endsWith(content);
    }
}
