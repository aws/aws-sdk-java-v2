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

package software.amazon.awssdk.services;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationResponse;

@Timeout(5)
public class BlockingAsyncRequestResponseBodyTest {
    private final WireMockServer wireMock = new WireMockServer(0);
    private ProtocolRestJsonAsyncClient client;

    @BeforeEach
    public void setup() {
        wireMock.start();
        client = ProtocolRestJsonAsyncClient.builder()
                                            .region(Region.US_WEST_2)
                                            .credentialsProvider(AnonymousCredentialsProvider.create())
                                            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                            .build();
    }

    @Test
    public void blockingResponseTransformer_readsRightValue() {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("hello")));

        CompletableFuture<ResponseInputStream<StreamingOutputOperationResponse>> responseFuture =
            client.streamingOutputOperation(StreamingOutputOperationRequest.builder().build(),
                                            AsyncResponseTransformer.toBlockingInputStream());
        ResponseInputStream<StreamingOutputOperationResponse> responseStream = responseFuture.join();

        assertThat(responseStream).asString(StandardCharsets.UTF_8).isEqualTo("hello");
        assertThat(responseStream.response().sdkHttpResponse().statusCode()).isEqualTo(200);
    }

    @Test
    public void blockingResponseTransformer_abortCloseDoesNotThrow() throws IOException {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("hello")));

        CompletableFuture<ResponseInputStream<StreamingOutputOperationResponse>> responseFuture =
            client.streamingOutputOperation(StreamingOutputOperationRequest.builder().build(),
                                            AsyncResponseTransformer.toBlockingInputStream());
        ResponseInputStream<StreamingOutputOperationResponse> responseStream = responseFuture.join();
        responseStream.abort();
        responseStream.close();
    }

    @Test
    public void blockingResponseTransformer_closeDoesNotThrow() throws IOException {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("hello")));

        CompletableFuture<ResponseInputStream<StreamingOutputOperationResponse>> responseFuture =
            client.streamingOutputOperation(StreamingOutputOperationRequest.builder().build(),
                                            AsyncResponseTransformer.toBlockingInputStream());
        ResponseInputStream<StreamingOutputOperationResponse> responseStream = responseFuture.join();
        responseStream.close();
    }
}
