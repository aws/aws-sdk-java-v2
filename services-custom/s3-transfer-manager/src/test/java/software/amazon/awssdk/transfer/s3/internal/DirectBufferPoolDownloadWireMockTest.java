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

package software.amazon.awssdk.transfer.s3.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.headRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.crt.S3CrtResponseTransformerFactory;
import software.amazon.awssdk.services.s3.crt.S3CrtDirectBufferPoolConfiguration;
import software.amazon.awssdk.services.s3.crt.S3CrtRetryConfiguration;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.transfer.s3.model.DirectBufferPoolDownloadRequest;
import software.amazon.awssdk.transfer.s3.model.Download;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

@WireMockTest
@Timeout(20)
class DirectBufferPoolDownloadWireMockTest {
    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final int PART_SIZE = 256 * 1024;
    private static final int PART_COUNT = 3;
    private static final byte[] CONTENT = payload(PART_SIZE * PART_COUNT);
    private static final String E_TAG = "\"etag\"";

    @Test
    void download_shouldStreamAndReportProgress_thenCompleteAfterCrtFinishes(WireMockRuntimeInfo wireMock) throws Exception {
        stubObject(CONTENT);
        CountingTransferListener listener = new CountingTransferListener();
        Download<ResponseInputStream<GetObjectResponse>> download;

        try (S3AsyncClient client = crtClient(wireMock);
             CrtS3TransferManager tm = crtTransferManager(client)) {
            download = tm.downloadWithDirectBufferPool(poolRequest(listener));
            try (ResponseInputStream<GetObjectResponse> stream = download.completionFuture().get(10, TimeUnit.SECONDS)
                                                                         .result()) {
                // The one-part pool and read window keep CRT from finishing until the reader consumes the earlier parts.
                assertThat(listener.completed).hasValue(0);
                assertThat(readAll(stream)).containsExactly(CONTENT);
            }
            listener.awaitTerminal();
        }

        assertThat(listener.initiated).hasValue(1);
        assertThat(listener.completed).hasValue(1);
        assertThat(listener.failed).hasValue(0);
        assertThat(listener.transferredBytes).hasValue(CONTENT.length);
        assertThat(download.progress().snapshot().transferredBytes()).isEqualTo(CONTENT.length);
        assertThat(download.progress().snapshot().totalBytes()).hasValue(CONTENT.length);
        assertThat(listener.response.get().contentLength()).isEqualTo(CONTENT.length);
    }

    @Test
    void serviceErrorBeforeStream_shouldReportOneFailure(WireMockRuntimeInfo wireMock) throws Exception {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(404)
                                                    .withBody("<Error><Code>NoSuchKey</Code><Message>missing</Message>"
                                                              + "</Error>")));
        CountingTransferListener listener = new CountingTransferListener();

        try (S3AsyncClient client = crtClient(wireMock);
             CrtS3TransferManager tm = crtTransferManager(client)) {
            Download<ResponseInputStream<GetObjectResponse>> download =
                tm.downloadWithDirectBufferPool(poolRequest(listener));

            assertThatThrownBy(() -> download.completionFuture().get(10, TimeUnit.SECONDS))
                .hasCauseInstanceOf(S3Exception.class);
            listener.awaitTerminal();
        }

        assertThat(listener.failed).hasValue(1);
        assertThat(listener.completed).hasValue(0);
    }

    @Test
    void cancelBeforeStream_shouldReportOneFailure(WireMockRuntimeInfo wireMock) throws Exception {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withFixedDelay(5_000)));
        CountingTransferListener listener = new CountingTransferListener();

        try (S3AsyncClient client = crtClient(wireMock);
             CrtS3TransferManager tm = crtTransferManager(client)) {
            Download<ResponseInputStream<GetObjectResponse>> download =
                tm.downloadWithDirectBufferPool(poolRequest(listener));

            download.completionFuture().cancel(true);
            listener.awaitTerminal();
        }

        assertThat(listener.failed).hasValue(1);
        assertThat(listener.completed).hasValue(0);
    }

    @Test
    void failureAfterStream_shouldDrainQueuedBytesThenReportOneFailure(WireMockRuntimeInfo wireMock) throws Exception {
        stubHeadObject(CONTENT.length);
        stubSuccessfulRange(0, CONTENT);
        stubFor(get(anyUrl()).atPriority(10)
                             .willReturn(aResponse().withStatus(404)
                                                        .withHeader("x-amz-request-id", "request-id")
                                                        .withHeader("Content-Length", "0")));
        CountingTransferListener listener = new CountingTransferListener();

        try (S3AsyncClient client = crtClient(wireMock);
             CrtS3TransferManager tm = crtTransferManager(client)) {
            Download<ResponseInputStream<GetObjectResponse>> download =
                tm.downloadWithDirectBufferPool(poolRequest(listener));
            try (ResponseInputStream<GetObjectResponse> stream = download.completionFuture().get(10, TimeUnit.SECONDS)
                                                                         .result()) {
                byte[] firstPart = new byte[PART_SIZE];
                readExactly(stream, firstPart);
                assertThat(firstPart).containsExactly(Arrays.copyOfRange(CONTENT, 0, PART_SIZE));
                assertThatThrownBy(stream::read).isInstanceOf(IOException.class);
            }
            listener.awaitTerminal();
        }

        assertThat(listener.failed).hasValue(1);
        assertThat(listener.completed).hasValue(0);
    }

    @Test
    void closeWhileRequestRunning_shouldReportOneCancellationFailure_andReleasePool(WireMockRuntimeInfo wireMock)
        throws Exception {
        stubObject(CONTENT);
        CountingTransferListener listener = new CountingTransferListener();
        CountingTransferListener nextListener = new CountingTransferListener();

        try (S3AsyncClient client = crtClient(wireMock);
             CrtS3TransferManager tm = crtTransferManager(client)) {
            ResponseInputStream<GetObjectResponse> stream =
                tm.downloadWithDirectBufferPool(poolRequest(listener)).completionFuture().get(10, TimeUnit.SECONDS)
                  .result();
            assertThat(stream.read()).isEqualTo(CONTENT[0] & 0xff);
            stream.close();
            stream.close();
            listener.awaitTerminal();

            try (ResponseInputStream<GetObjectResponse> next =
                     tm.downloadWithDirectBufferPool(poolRequest(nextListener)).completionFuture()
                       .get(10, TimeUnit.SECONDS).result()) {
                assertThat(readAll(next)).containsExactly(CONTENT);
            }
            nextListener.awaitTerminal();
        }

        assertThat(listener.failed).hasValue(1);
        assertThat(listener.completed).hasValue(0);
        assertThat(listener.failure.get()).isInstanceOf(CancellationException.class);
        assertThat(nextListener.completed).hasValue(1);
    }

    @Test
    void closeAfterCrtFinished_withUnreadData_shouldReportComplete(WireMockRuntimeInfo wireMock) throws Exception {
        byte[] content = payload(PART_SIZE);
        stubObject(content);
        CountingTransferListener listener = new CountingTransferListener();

        try (S3AsyncClient client = crtClient(wireMock);
             CrtS3TransferManager tm = crtTransferManager(client)) {
            ResponseInputStream<GetObjectResponse> stream =
                tm.downloadWithDirectBufferPool(poolRequest(listener)).completionFuture().get(10, TimeUnit.SECONDS)
                  .result();
            listener.awaitTerminal();
            stream.close();
        }

        assertThat(listener.completed).hasValue(1);
        assertThat(listener.failed).hasValue(0);
    }

    @Test
    void crossRegionRedirect_shouldReportOneCompletionWithFinalResponse(WireMockRuntimeInfo wireMock) throws Exception {
        byte[] content = payload(PART_SIZE);
        int port = wireMock.getHttpPort();
        byte[] errorBody = ("<Error><Code>PermanentRedirect</Code><Message>redirect</Message>"
                            + "<RequestId>request-id</RequestId></Error>").getBytes(StandardCharsets.UTF_8);
        stubFor(get(anyUrl()).withHeader("Host", equalTo("localhost:" + port))
                             .willReturn(aResponse().withStatus(301)
                                                        .withHeader("x-amz-bucket-region", Region.EU_WEST_1.id())
                                                        .withHeader("Content-Type", "application/xml")
                                                        .withHeader("Content-Length", Integer.toString(errorBody.length))
                                                        .withBody(errorBody)));
        stubFor(get(anyUrl()).withHeader("Host", equalTo("127.0.0.1:" + port))
                             .willReturn(aResponse().withStatus(206)
                                                        .withHeader("Content-Length", Integer.toString(content.length))
                                                        .withHeader("Content-Range",
                                                                    "bytes 0-" + (content.length - 1) + "/" + content.length)
                                                        .withHeader("ETag", E_TAG)
                                                        .withBody(content)));
        CountingTransferListener listener = new CountingTransferListener();
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket(BUCKET)
                                                   .key(KEY)
                                                   .overrideConfiguration(o -> o.endpointProvider(
                                                       regionalEndpointProvider(port)))
                                                   .build();

        try (S3AsyncClient client = crtClientBuilder(wireMock)
                 .crossRegionAccessEnabled(true)
                 .forcePathStyle(true)
                 .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                 .retryConfiguration(S3CrtRetryConfiguration.builder().numRetries(0).build())
                 .directBufferPoolConfiguration(S3CrtDirectBufferPoolConfiguration.fixed(PART_SIZE))
                 .build();
             CrtS3TransferManager tm = crtTransferManager(client)) {
            try (ResponseInputStream<GetObjectResponse> stream =
                     tm.downloadWithDirectBufferPool(poolRequest(request, listener)).completionFuture()
                       .get(10, TimeUnit.SECONDS).result()) {
                assertThat(readAll(stream)).containsExactly(content);
            }
            listener.awaitTerminal();
        }

        assertThat(findAll(getRequestedFor(anyUrl()))).hasSize(2);
        assertThat(listener.completed).hasValue(1);
        assertThat(listener.failed).hasValue(0);
        assertThat(listener.response.get().eTag()).isEqualTo(E_TAG);
        assertThat(listener.response.get().contentLength()).isEqualTo(content.length);
    }

    @Test
    void crtClientWithoutPool_shouldFailBeforeTransmission(WireMockRuntimeInfo wireMock) throws Exception {
        CountingTransferListener listener = new CountingTransferListener();

        try (S3AsyncClient client = crtClientBuilder(wireMock).build();
             CrtS3TransferManager tm = crtTransferManager(client)) {
            Download<ResponseInputStream<GetObjectResponse>> download =
                tm.downloadWithDirectBufferPool(poolRequest(listener));

            assertThatThrownBy(() -> download.completionFuture().get(10, TimeUnit.SECONDS))
                .hasRootCauseInstanceOf(IllegalStateException.class);
            listener.awaitTerminal();
        }

        assertThat(listener.failed).hasValue(1);
        assertThat(listener.completed).hasValue(0);
        verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void reusedRequest_shouldStartAnotherDownload(WireMockRuntimeInfo wireMock) throws Exception {
        stubObject(CONTENT);
        CountingTransferListener listener = new CountingTransferListener(2);
        DirectBufferPoolDownloadRequest<ResponseInputStream<GetObjectResponse>> request = poolRequest(listener);

        try (S3AsyncClient client = crtClient(wireMock);
             CrtS3TransferManager tm = crtTransferManager(client)) {
            for (int i = 0; i < 2; i++) {
                try (ResponseInputStream<GetObjectResponse> stream =
                         tm.downloadWithDirectBufferPool(request).completionFuture().get(10, TimeUnit.SECONDS).result()) {
                    assertThat(readAll(stream)).containsExactly(CONTENT);
                }
            }
            listener.awaitTerminal();
        }

        assertThat(listener.initiated).hasValue(2);
        assertThat(listener.completed).hasValue(2);
        assertThat(listener.failed).hasValue(0);
    }

    @Test
    void reusedRequestWhileFirstDownloadActive_shouldReportEachDownloadSeparately(WireMockRuntimeInfo wireMock)
        throws Exception {
        stubObject(CONTENT);
        CountingTransferListener listener = new CountingTransferListener(2);
        DirectBufferPoolDownloadRequest<ResponseInputStream<GetObjectResponse>> request = poolRequest(listener);

        // Room for both objects, so the unread second download cannot hold memory the first one needs.
        long poolSize = 2L * PART_SIZE * PART_COUNT;
        try (S3AsyncClient client =
                 crtClientBuilder(wireMock).directBufferPoolConfiguration(S3CrtDirectBufferPoolConfiguration.fixed(poolSize))
                                           .build();
             CrtS3TransferManager tm = crtTransferManager(client)) {
            Download<ResponseInputStream<GetObjectResponse>> first = tm.downloadWithDirectBufferPool(request);
            Download<ResponseInputStream<GetObjectResponse>> second;
            try (ResponseInputStream<GetObjectResponse> stream = first.completionFuture().get(10, TimeUnit.SECONDS).result()) {
                second = tm.downloadWithDirectBufferPool(request);
                assertThat(readAll(stream)).containsExactly(CONTENT);
            }
            try (ResponseInputStream<GetObjectResponse> stream =
                     second.completionFuture().get(10, TimeUnit.SECONDS).result()) {
                assertThat(readAll(stream)).containsExactly(CONTENT);
            }
            listener.awaitTerminal();
            assertThat(first.progress().snapshot().sdkResponse()).isPresent();
            assertThat(second.progress().snapshot().sdkResponse()).isPresent();
        }

        assertThat(listener.completed).hasValue(2);
        assertThat(listener.failed).hasValue(0);
    }

    @Test
    void ordinaryTransformer_shouldFailBeforeTransmission(WireMockRuntimeInfo wireMock) throws Exception {
        CountingTransferListener listener = new CountingTransferListener();
        DirectBufferPoolDownloadRequest<ResponseInputStream<GetObjectResponse>> request =
            DirectBufferPoolDownloadRequest.builder()
                                           .getObjectRequest(r -> r.bucket(BUCKET).key(KEY))
                                           .addTransferListener(listener)
                                           .responseTransformer(AsyncResponseTransformer.toBlockingInputStream())
                                           .build();

        assertFailsBeforeTransmission(wireMock, request, listener, IllegalArgumentException.class);
    }

    @Test
    void wrappedPooledTransformer_shouldFailBeforeTransmission(WireMockRuntimeInfo wireMock) throws Exception {
        CountingTransferListener listener = new CountingTransferListener();
        DirectBufferPoolDownloadRequest<ResponseInputStream<GetObjectResponse>> request =
            DirectBufferPoolDownloadRequest.builder()
                                           .getObjectRequest(r -> r.bucket(BUCKET).key(KEY))
                                           .addTransferListener(listener)
                                           .responseTransformer(new ForwardingTransformer<>(
                                               S3CrtResponseTransformerFactory.toBlockingInputStreamFromDirectBufferPool()))
                                           .build();

        assertFailsBeforeTransmission(wireMock, request, listener, IllegalArgumentException.class);
    }

    @Test
    void downloadWithPooledTransformer_shouldFailBeforeTransmission(WireMockRuntimeInfo wireMock) throws Exception {
        CountingTransferListener listener = new CountingTransferListener();
        DownloadRequest<ResponseInputStream<GetObjectResponse>> request =
            DownloadRequest.builder()
                           .getObjectRequest(r -> r.bucket(BUCKET).key(KEY))
                           .addTransferListener(listener)
                           .responseTransformer(S3CrtResponseTransformerFactory.toBlockingInputStreamFromDirectBufferPool())
                           .build();

        try (S3AsyncClient client = crtClient(wireMock);
             CrtS3TransferManager tm = crtTransferManager(client)) {
            Download<ResponseInputStream<GetObjectResponse>> download = tm.download(request);

            assertThatThrownBy(() -> download.completionFuture().get(10, TimeUnit.SECONDS))
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Direct buffer pool transformers are not supported by download(). Use "
                                     + "downloadWithDirectBufferPool instead.");
            listener.awaitTerminal();
        }

        assertThat(listener.failed).hasValue(1);
        assertThat(listener.completed).hasValue(0);
        verify(0, anyRequestedFor(anyUrl()));
    }

    @ParameterizedTest(name = "multipartEnabled={0}")
    @ValueSource(booleans = {true, false})
    void javaClient_shouldFailBeforeTransmission(boolean multipartEnabled, WireMockRuntimeInfo wireMock) throws Exception {
        CountingTransferListener listener = new CountingTransferListener();

        try (S3AsyncClient javaClient = javaClient(wireMock, multipartEnabled);
             GenericS3TransferManager tm =
                 new GenericS3TransferManager(TransferManagerConfiguration.builder().build(), javaClient, false)) {
            Download<ResponseInputStream<GetObjectResponse>> download =
                tm.downloadWithDirectBufferPool(poolRequest(listener));

            assertThatThrownBy(() -> download.completionFuture().get(10, TimeUnit.SECONDS))
                .hasRootCauseInstanceOf(UnsupportedOperationException.class);
            listener.awaitTerminal();
        }

        assertThat(listener.failed).hasValue(1);
        assertThat(listener.completed).hasValue(0);
        verify(0, anyRequestedFor(anyUrl()));
    }

    private static void assertFailsBeforeTransmission(WireMockRuntimeInfo wireMock,
                                                      DirectBufferPoolDownloadRequest<?> request,
                                                      CountingTransferListener listener,
                                                      Class<? extends Throwable> rootCause) throws Exception {
        try (S3AsyncClient client = crtClient(wireMock);
             CrtS3TransferManager tm = crtTransferManager(client)) {
            Download<?> download = tm.downloadWithDirectBufferPool(request);

            assertThatThrownBy(() -> download.completionFuture().get(10, TimeUnit.SECONDS))
                .hasRootCauseInstanceOf(rootCause);
            listener.awaitTerminal();
        }

        assertThat(listener.failed).hasValue(1);
        assertThat(listener.completed).hasValue(0);
        verify(0, anyRequestedFor(anyUrl()));
    }

    private static DirectBufferPoolDownloadRequest<ResponseInputStream<GetObjectResponse>> poolRequest(
        TransferListener listener) {
        return poolRequest(GetObjectRequest.builder().bucket(BUCKET).key(KEY).build(), listener);
    }

    private static DirectBufferPoolDownloadRequest<ResponseInputStream<GetObjectResponse>> poolRequest(
        GetObjectRequest getObjectRequest, TransferListener listener) {
        return DirectBufferPoolDownloadRequest.builder()
                                              .getObjectRequest(getObjectRequest)
                                              .addTransferListener(listener)
                                              .responseTransformer(
                                                  S3CrtResponseTransformerFactory.toBlockingInputStreamFromDirectBufferPool())
                                              .build();
    }

    private static CrtS3TransferManager crtTransferManager(S3AsyncClient client) {
        return new CrtS3TransferManager(TransferManagerConfiguration.builder().build(), client, false);
    }

    private static S3AsyncClient crtClient(WireMockRuntimeInfo wireMock) {
        return crtClientBuilder(wireMock).directBufferPoolConfiguration(S3CrtDirectBufferPoolConfiguration.fixed(PART_SIZE))
                                         .build();
    }

    private static S3CrtAsyncClientBuilder crtClientBuilder(WireMockRuntimeInfo wireMock) {
        return S3AsyncClient.crtBuilder()
                            .region(Region.US_EAST_1)
                            .endpointOverride(URI.create("http://localhost:" + wireMock.getHttpPort()))
                            .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("key", "secret")))
                            .minimumPartSizeInBytes((long) PART_SIZE)
                            .initialReadBufferSizeInBytes((long) PART_SIZE);
    }

    private static S3AsyncClient javaClient(WireMockRuntimeInfo wireMock, boolean multipartEnabled) {
        return S3AsyncClient.builder()
                            .region(Region.US_EAST_1)
                            .endpointOverride(URI.create("http://localhost:" + wireMock.getHttpPort()))
                            .forcePathStyle(true)
                            .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("key", "secret")))
                            .multipartEnabled(multipartEnabled)
                            .build();
    }

    private static S3EndpointProvider regionalEndpointProvider(int port) {
        return params -> {
            String host = Region.EU_WEST_1.equals(params.region()) ? "127.0.0.1" : "localhost";
            return CompletableFuture.completedFuture(Endpoint.builder().url(URI.create("http://" + host + ":" + port)).build());
        };
    }

    private static void stubObject(byte[] content) {
        stubHeadObject(content.length);
        for (int part = 0; part * PART_SIZE < content.length; part++) {
            stubSuccessfulRange(part, content);
        }
    }

    private static void stubHeadObject(int contentLength) {
        stubFor(head(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("Content-Length", Integer.toString(contentLength))
                                                    .withHeader("ETag", E_TAG)));
    }

    private static void stubSuccessfulRange(int part, byte[] content) {
        int start = part * PART_SIZE;
        int end = Math.min(start + PART_SIZE, content.length) - 1;
        byte[] body = Arrays.copyOfRange(content, start, end + 1);
        stubFor(get(anyUrl()).atPriority(1)
                             .withHeader("Range", equalTo("bytes=" + start + "-" + end))
                             .willReturn(aResponse().withStatus(206)
                                                        .withHeader("Content-Length", Integer.toString(body.length))
                                                        .withHeader("Content-Range",
                                                                    "bytes " + start + "-" + end + "/" + content.length)
                                                        .withHeader("ETag", E_TAG)
                                                        .withBody(body)));
    }

    private static byte[] readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) >= 0) {
            result.write(buffer, 0, read);
        }
        return result.toByteArray();
    }

    private static void readExactly(InputStream stream, byte[] destination) throws IOException {
        int offset = 0;
        while (offset < destination.length) {
            int read = stream.read(destination, offset, destination.length - offset);
            if (read < 0) {
                throw new IOException("Unexpected end of stream");
            }
            offset += read;
        }
    }

    private static byte[] payload(int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (i % 251);
        }
        return result;
    }

    private static final class ForwardingTransformer<T> implements AsyncResponseTransformer<GetObjectResponse, T> {
        private final AsyncResponseTransformer<GetObjectResponse, T> delegate;

        private ForwardingTransformer(AsyncResponseTransformer<GetObjectResponse, T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public CompletableFuture<T> prepare() {
            return delegate.prepare();
        }

        @Override
        public void onResponse(GetObjectResponse response) {
            delegate.onResponse(response);
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            delegate.onStream(publisher);
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            delegate.exceptionOccurred(error);
        }
    }

    private static final class CountingTransferListener implements TransferListener {
        private final AtomicInteger initiated = new AtomicInteger();
        private final AtomicInteger completed = new AtomicInteger();
        private final AtomicInteger failed = new AtomicInteger();
        private final AtomicLong transferredBytes = new AtomicLong();
        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private final AtomicReference<GetObjectResponse> response = new AtomicReference<>();
        private final AtomicReference<Optional<SdkResponse>> failureResponse = new AtomicReference<>();
        private final CountDownLatch terminal;

        private CountingTransferListener() {
            this(1);
        }

        private CountingTransferListener(int expectedTerminalEvents) {
            terminal = new CountDownLatch(expectedTerminalEvents);
        }

        @Override
        public void transferInitiated(Context.TransferInitiated context) {
            initiated.incrementAndGet();
        }

        @Override
        public void bytesTransferred(Context.BytesTransferred context) {
            transferredBytes.set(context.progressSnapshot().transferredBytes());
        }

        @Override
        public void transferComplete(Context.TransferComplete context) {
            context.progressSnapshot().sdkResponse().ifPresent(r -> response.set((GetObjectResponse) r));
            completed.incrementAndGet();
            terminal.countDown();
        }

        @Override
        public void transferFailed(Context.TransferFailed context) {
            failure.set(context.exception());
            failureResponse.set(context.progressSnapshot().sdkResponse());
            failed.incrementAndGet();
            terminal.countDown();
        }

        private void awaitTerminal() throws InterruptedException {
            assertThat(terminal.await(10, TimeUnit.SECONDS)).isTrue();
        }
    }
}
