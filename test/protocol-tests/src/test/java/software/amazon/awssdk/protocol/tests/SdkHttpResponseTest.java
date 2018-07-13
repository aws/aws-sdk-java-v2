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

package software.amazon.awssdk.protocol.tests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Verify response contains correct {@link SdkHttpResponse}.
 */
public class SdkHttpResponseTest {

    private static final String JSON_BODY = "{\"StringMember\":\"foo\"}";
    private static final String STATUS_TEXT = "hello world";

    @Rule
    public final WireMockRule wireMock = new WireMockRule(0);

    private ProtocolRestJsonClient client;

    private ProtocolRestJsonAsyncClient asyncClient;

    private static final Map<String, List<String>> EXPECTED_HEADERS = new HashMap<String, List<String>>() {{
        put("x-foo", Collections.singletonList("a"));
        put("x-bar", Collections.singletonList("b"));
    }};

    @Before
    public void setupClient() {
        client = ProtocolRestJsonClient.builder()
                                       .region(Region.US_WEST_1)
                                       .endpointOverride(URI.create("http://localhost:"
                                                                    + wireMock.port()))
                                       .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                       .build();

        asyncClient = ProtocolRestJsonAsyncClient.builder()
                                                 .region(Region.US_WEST_1)
                                                 .endpointOverride(URI.create("http://localhost:"
                                                                              + wireMock.port()))
                                                 .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                                 .build();
    }

    @Test
    public void syncNonStreamingShouldContainSdkHttpDate() {
        stubWithHeaders(EXPECTED_HEADERS);
        AllTypesResponse response = client.allTypes(b -> b.simpleList("test"));
        verifySdkHttpResponse(response);
    }

    @Test
    public void syncStreamingShouldContainSdkHttpDate() {

        stubWithHeaders(EXPECTED_HEADERS);
        ResponseBytes<StreamingOutputOperationResponse> responseBytes = client
            .streamingOutputOperation(SdkBuilder::build, ResponseTransformer.toBytes());
        StreamingOutputOperationResponse response = responseBytes.response();

        verifySdkHttpResponse(response);
    }

    @Test
    public void asyncNonStreamingShouldContainsSdkHttpData() {
        stubWithHeaders(EXPECTED_HEADERS);
        AllTypesResponse response = asyncClient.allTypes(b -> b.simpleList("test")).join();
        verifySdkHttpResponse(response);
    }

    @Test
    public void asyncStreamingMethodShouldContainSdkHttpDate() {

        stubWithHeaders(EXPECTED_HEADERS);
        ResponseBytes<StreamingOutputOperationResponse> responseBytes = asyncClient
            .streamingOutputOperation(SdkBuilder::build, AsyncResponseTransformer.toBytes()).join();
        StreamingOutputOperationResponse response = responseBytes.response();

        verifySdkHttpResponse(response);
    }

    private void stubWithHeaders(Map<String, List<String>> headers) {

        HttpHeaders httpHeaders = new HttpHeaders(headers.entrySet().stream().map(entry -> new HttpHeader(entry.getKey(),
                                                                                                          entry.getValue()))
                                                         .collect(Collectors.toList()));
        stubFor(post(anyUrl()).willReturn(aResponse()
                                              .withStatus(200)
                                              .withStatusMessage(STATUS_TEXT)
                                              .withHeaders(httpHeaders)
                                              .withBody(JSON_BODY)));
    }

    private void verifySdkHttpResponse(AwsResponse response) {
        SdkHttpResponse sdkHttpResponse = response.sdkHttpResponse();
        assertThat(sdkHttpResponse.statusCode()).isEqualTo(200);
        assertThat(sdkHttpResponse.statusText().get()).isEqualTo(STATUS_TEXT);
        EXPECTED_HEADERS.entrySet().forEach(entry -> assertThat(sdkHttpResponse.headers()).contains(entry));
    }

}
