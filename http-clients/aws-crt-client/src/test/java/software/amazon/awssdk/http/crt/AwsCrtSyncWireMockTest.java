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


package software.amazon.awssdk.http.crt;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.io.EventLoopGroup;
import software.amazon.awssdk.crt.io.HostResolver;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;

public class AwsCrtSyncWireMockTest {
    private final WireMockServer mockServer = new WireMockServer(new WireMockConfiguration()
                                                                     .dynamicPort()
                                                                     .dynamicHttpsPort());
    private SdkHttpClient client;

    @BeforeEach
    public void setup() {
        mockServer.start();
        mockServer.stubFor(put(urlMatching(".*")).willReturn(aResponse().withStatus(200).withBody("hello")));
        client = AwsCrtHttpClient.builder().build();
    }

    @AfterEach
    public void teardown() {
        mockServer.stop();
        client.close();
        EventLoopGroup.closeStaticDefault();
        HostResolver.closeStaticDefault();
        CrtResource.waitForNoResources();
    }

    @Test
    public void sdkAddsContentLength_For_Requests_with_RequestBody() throws Throwable {
        URI uri = URI.create("http://localhost:" + mockServer.port());
        String stringRequestBody = "Body";
        byte[] reqBody = stringRequestBody.getBytes(StandardCharsets.UTF_8);
        String resourcePath = "/server/test";

        Map<String, String> params = emptyMap();
        SdkHttpRequest request = SdkHttpFullRequest.builder()
                                                   .uri(uri)
                                                   .method(SdkHttpMethod.PUT)
                                                   .contentStreamProvider(() -> new ByteArrayInputStream(reqBody))
                                                   .encodedPath(resourcePath)
                                                   .applyMutation(b -> params.forEach(b::putRawQueryParameter))
                                                   .applyMutation(b ->
                                                                      b.putHeader("Host", uri.getHost())).build();
        HttpExecuteRequest.Builder executeRequestBuilder = HttpExecuteRequest.builder();
        executeRequestBuilder.request(request);
        executeRequestBuilder.contentStreamProvider(() -> new ByteArrayInputStream(reqBody));
        ExecutableHttpRequest executableRequest = client.prepareRequest(executeRequestBuilder.build());
        HttpExecuteResponse response = executableRequest.call();
        assertThat(response.httpResponse().statusCode()).isEqualTo(200);
        mockServer.verify(putRequestedFor(urlPathEqualTo(resourcePath))
                              .withRequestBody(equalTo(stringRequestBody))
                              .withHeader("content-length",
                                          equalTo(String.valueOf(stringRequestBody.length()))));
    }

    @Test
    public void sdkDoesNotAddsContentLength_For_Requests_with_NoRequestBody() throws Throwable {
        URI uri = URI.create("http://localhost:" + mockServer.port());
        String stringRequestBody = "Body";
        String resourcePath = "/server/test";

        Map<String, String> params = emptyMap();
        SdkHttpRequest request = SdkHttpFullRequest.builder()
                                                   .uri(uri)
                                                   .method(SdkHttpMethod.PUT)
                                                   .encodedPath(resourcePath)
                                                   .applyMutation(b -> params.forEach(b::putRawQueryParameter))
                                                   .applyMutation(b ->
                                                                      b.putHeader("Host", uri.getHost())).build();
        HttpExecuteRequest.Builder executeRequestBuilder = HttpExecuteRequest.builder();
        executeRequestBuilder.request(request);
        ExecutableHttpRequest executableRequest = client.prepareRequest(executeRequestBuilder.build());
        HttpExecuteResponse response = executableRequest.call();
        assertThat(response.httpResponse().statusCode()).isEqualTo(200);
        mockServer.verify(putRequestedFor(urlPathEqualTo(resourcePath))
                              .withoutHeader("content-length"));
    }
}
