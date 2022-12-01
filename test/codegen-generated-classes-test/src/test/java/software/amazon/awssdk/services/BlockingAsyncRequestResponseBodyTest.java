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
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.allRequests;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.BlockingOutputStreamAsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingInputOperationResponse;
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

    @Test
    public void blockingOutputStreamWithoutExecutor_sendsRightValues() throws IOException {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}")));

        BlockingOutputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingOutputStream(5L);
        CompletableFuture<?> responseFuture =
            client.streamingInputOperation(StreamingInputOperationRequest.builder().build(), body);

        try (OutputStream stream = body.outputStream()) {
            stream.write("Hello".getBytes(StandardCharsets.UTF_8));
        }
        responseFuture.join();

        List<LoggedRequest> requests = wireMock.findAll(allRequests());
        assertThat(requests).singleElement()
                            .extracting(LoggedRequest::getBody)
                            .extracting(String::new)
                            .isEqualTo("Hello");
    }

    @Test
    public void blockingOutputStreamWithoutExecutor_canUnderUpload() throws IOException {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}")));

        BlockingOutputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingOutputStream(4L);
        CompletableFuture<?> responseFuture =
            client.streamingInputOperation(StreamingInputOperationRequest.builder().build(), body);

        try (OutputStream stream = body.outputStream()) {
            stream.write("Hello".getBytes(StandardCharsets.UTF_8));
        }
        responseFuture.join();

        List<LoggedRequest> requests = wireMock.findAll(allRequests());
        assertThat(requests).singleElement()
                            .extracting(LoggedRequest::getBody)
                            .extracting(String::new)
                            .isEqualTo("Hell");
    }

    @Test
    public void blockingOutputStreamWithoutExecutor_canUnderUploadOneByteAtATime() throws IOException {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}")));

        BlockingOutputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingOutputStream(4L);
        CompletableFuture<?> responseFuture =
            client.streamingInputOperation(StreamingInputOperationRequest.builder().build(), body);

        try (OutputStream stream = body.outputStream()) {
            stream.write('H');
            stream.write('e');
            stream.write('l');
            stream.write('l');
            stream.write('o');
        }
        responseFuture.join();

        List<LoggedRequest> requests = wireMock.findAll(allRequests());
        assertThat(requests).singleElement()
                            .extracting(LoggedRequest::getBody)
                            .extracting(String::new)
                            .isEqualTo("Hell");
    }

    @Test
    public void blockingOutputStreamWithoutExecutor_propagatesCancellations() {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}")));

        BlockingOutputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingOutputStream(5L);
        CompletableFuture<StreamingInputOperationResponse> responseFuture =
            client.streamingInputOperation(StreamingInputOperationRequest.builder().build(), body);
        body.outputStream().cancel();
        assertThatThrownBy(responseFuture::get).hasRootCauseInstanceOf(CancellationException.class);
    }

    @Test
    public void blockingOutputStreamWithoutExecutor_propagates400Failures() throws IOException {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(404).withBody("{}")));
        BlockingOutputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingOutputStream(5L);
        CompletableFuture<?> responseFuture =
            client.streamingInputOperation(StreamingInputOperationRequest.builder().build(), body);

        try (OutputStream stream = body.outputStream()) {
            stream.write("Hello".getBytes(StandardCharsets.UTF_8));
        }
        assertThatThrownBy(responseFuture::join).hasCauseInstanceOf(SdkServiceException.class);
    }

    @Test
    public void blockingOutputStreamWithoutExecutor_propagates500Failures() throws IOException {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(500).withBody("{}")));

        BlockingOutputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingOutputStream(5L);
        CompletableFuture<StreamingInputOperationResponse> responseFuture =
            client.streamingInputOperation(StreamingInputOperationRequest.builder().build(), body);

        try (OutputStream stream = body.outputStream()) {
            stream.write("Hello".getBytes(StandardCharsets.UTF_8));
        }
        assertThatThrownBy(responseFuture::get)
            .hasCauseInstanceOf(SdkClientException.class)
            .hasMessageContaining("AsyncRequestBody.forBlockingOutputStream does not support retries");
    }
}
