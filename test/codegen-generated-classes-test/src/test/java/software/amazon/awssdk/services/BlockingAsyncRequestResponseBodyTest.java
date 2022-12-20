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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.async.BlockingOutputStreamAsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingInputOperationResponse;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

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
    public void blockingWithExecutor_sendsRightValues() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}")));
            client.streamingInputOperation(StreamingInputOperationRequest.builder().build(),
                                           AsyncRequestBody.fromInputStream(new StringInputStream("Hello"),
                                                                            5L,
                                                                            executorService))
                  .join();
            List<LoggedRequest> requests = wireMock.findAll(allRequests());
            assertThat(requests).singleElement()
                                .extracting(LoggedRequest::getBody)
                                .extracting(String::new)
                                .isEqualTo("Hello");
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    public void blockingWithExecutor_canUnderUpload() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}")));
            client.streamingInputOperation(StreamingInputOperationRequest.builder().build(),
                                           AsyncRequestBody.fromInputStream(new StringInputStream("Hello"),
                                                                            4L,
                                                                            executorService))
                  .join();
            List<LoggedRequest> requests = wireMock.findAll(allRequests());
            assertThat(requests).singleElement()
                                .extracting(LoggedRequest::getBody)
                                .extracting(String::new)
                                .isEqualTo("Hell");
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    public void blockingWithExecutor_canUnderUploadOneByteAtATime() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}")));
            client.streamingInputOperation(StreamingInputOperationRequest.builder().build(),
                                           AsyncRequestBody.fromInputStream(new TrickleInputStream(new StringInputStream("Hello")),
                                                                            4L,
                                                                            executorService))
                  .join();
            List<LoggedRequest> requests = wireMock.findAll(allRequests());
            assertThat(requests).singleElement()
                                .extracting(LoggedRequest::getBody)
                                .extracting(String::new)
                                .isEqualTo("Hell");
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    public void blockingWithExecutor_propagatesReadFailures() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}")));

            CompletableFuture<StreamingInputOperationResponse> responseFuture =
                client.streamingInputOperation(StreamingInputOperationRequest.builder().build(),
                                               AsyncRequestBody.fromInputStream(new FailOnReadInputStream(),
                                                                                5L,
                                                                                executorService));
            assertThatThrownBy(responseFuture::join).hasRootCauseInstanceOf(IOException.class);
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    public void blockingWithExecutor_propagates400Failures() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(404).withBody("{}")));
            CompletableFuture<?> responseFuture =
                client.streamingInputOperation(StreamingInputOperationRequest.builder().build(),
                                               AsyncRequestBody.fromInputStream(new StringInputStream("Hello"),
                                                                                5L,
                                                                                executorService));
            assertThatThrownBy(responseFuture::join).hasCauseInstanceOf(SdkServiceException.class);
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    public void blockingWithExecutor_propagates500Failures() {
        ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().daemonThreads(true).build());
        try {
            wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(500).withBody("{}")));
            CompletableFuture<?> responseFuture =
                client.streamingInputOperation(StreamingInputOperationRequest.builder().build(),
                                               AsyncRequestBody.fromInputStream(new StringInputStream("Hello"),
                                                                                5L,
                                                                                executorService));
            assertThatThrownBy(responseFuture::join).hasCauseInstanceOf(SdkServiceException.class);
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    public void blockingInputStreamWithoutExecutor_sendsRightValues() {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}")));

        BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream(5L);
        CompletableFuture<?> responseFuture =
            client.streamingInputOperation(StreamingInputOperationRequest.builder().build(), body);
        body.writeInputStream(new StringInputStream("Hello"));
        responseFuture.join();

        List<LoggedRequest> requests = wireMock.findAll(allRequests());
        assertThat(requests).singleElement()
                            .extracting(LoggedRequest::getBody)
                            .extracting(String::new)
                            .isEqualTo("Hello");
    }

    @Test
    public void blockingInputStreamWithoutExecutor_canUnderUpload() {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}")));

        BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream(4L);
        CompletableFuture<?> responseFuture =
            client.streamingInputOperation(StreamingInputOperationRequest.builder().build(), body);
        body.writeInputStream(new StringInputStream("Hello"));
        responseFuture.join();

        List<LoggedRequest> requests = wireMock.findAll(allRequests());
        assertThat(requests).singleElement()
                            .extracting(LoggedRequest::getBody)
                            .extracting(String::new)
                            .isEqualTo("Hell");
    }

    @Test
    public void blockingInputStreamWithoutExecutor_canUnderUploadOneByteAtATime() {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}")));

        BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream(4L);
        CompletableFuture<?> responseFuture =
            client.streamingInputOperation(StreamingInputOperationRequest.builder().build(), body);
        body.writeInputStream(new TrickleInputStream(new StringInputStream("Hello")));
        responseFuture.join();

        List<LoggedRequest> requests = wireMock.findAll(allRequests());
        assertThat(requests).singleElement()
                            .extracting(LoggedRequest::getBody)
                            .extracting(String::new)
                            .isEqualTo("Hell");
    }

    @Test
    public void blockingInputStreamWithoutExecutor_propagatesReadFailures() {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}")));

        BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream(5L);
        CompletableFuture<StreamingInputOperationResponse> responseFuture =
            client.streamingInputOperation(StreamingInputOperationRequest.builder().build(), body);
        assertThatThrownBy(() -> body.writeInputStream(new FailOnReadInputStream())).hasRootCauseInstanceOf(IOException.class);
        assertThatThrownBy(responseFuture::get)
            .hasCauseInstanceOf(SdkClientException.class)
            .hasMessageContaining("AsyncRequestBody.forBlockingInputStream does not support retries");
    }

    @Test
    public void blockingInputStreamWithoutExecutor_propagates400Failures() {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(404).withBody("{}")));
        BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream(5L);
        CompletableFuture<?> responseFuture =
            client.streamingInputOperation(StreamingInputOperationRequest.builder().build(), body);
        body.writeInputStream(new StringInputStream("Hello"));
        assertThatThrownBy(responseFuture::join).hasCauseInstanceOf(SdkServiceException.class);
    }

    @Test
    public void blockingInputStreamWithoutExecutor_propagates500Failures() {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(500).withBody("{}")));

        BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream(5L);
        CompletableFuture<StreamingInputOperationResponse> responseFuture =
            client.streamingInputOperation(StreamingInputOperationRequest.builder().build(), body);
        body.writeInputStream(new StringInputStream("Hello"));
        assertThatThrownBy(responseFuture::get)
            .hasCauseInstanceOf(SdkClientException.class)
            .hasMessageContaining("AsyncRequestBody.forBlockingInputStream does not support retries");
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

    private static class FailOnReadInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            throw new IOException("Intentionally failed to read.");
        }
    }

    private static class TrickleInputStream extends InputStream {
        private final InputStream delegate;

        private TrickleInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            if (b.length == 0) {
                return 0;
            }
            return delegate.read(b, 0, 1);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            return delegate.read(b, off, 1);
        }
    }
}
