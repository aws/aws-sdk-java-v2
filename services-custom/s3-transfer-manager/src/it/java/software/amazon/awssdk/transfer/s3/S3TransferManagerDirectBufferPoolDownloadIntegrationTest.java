/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.transfer.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.crt.S3CrtDirectBufferPoolConfiguration;
import software.amazon.awssdk.services.s3.crt.S3CrtResponseTransformerFactory;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.model.DirectBufferPoolDownloadRequest;
import software.amazon.awssdk.transfer.s3.model.Download;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.utils.Md5Utils;

public class S3TransferManagerDirectBufferPoolDownloadIntegrationTest extends S3IntegrationTestBase {
    private static final int MIB = 1024 * 1024;
    private static final int PART_SIZE = 8 * MIB;
    private static final int OBJECT_SIZE = 18 * MIB;
    private static final String BUCKET = temporaryBucketName(S3TransferManagerDirectBufferPoolDownloadIntegrationTest.class);
    private static final String KEY = "object";
    private static File object;

    @BeforeAll
    public static void setup() throws IOException {
        createBucket(BUCKET);
        object = new RandomTempFile(OBJECT_SIZE);
        s3.putObject(PutObjectRequest.builder().bucket(BUCKET).key(KEY).build(), object.toPath());
    }

    @AfterAll
    public static void cleanup() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    void downloadWithDirectBufferPool_shouldReportProgressAndComplete() throws Exception {
        try (S3AsyncClient client = pooledClient();
             S3TransferManager transferManager = S3TransferManager.builder().s3Client(client).build()) {
            CaptureTransferListener listener = new CaptureTransferListener();
            Download<ResponseInputStream<GetObjectResponse>> download =
                transferManager.downloadWithDirectBufferPool(pooledRequest(listener));

            try (ResponseInputStream<GetObjectResponse> stream = download.completionFuture().get(1, TimeUnit.MINUTES).result()) {
                assertThat(listener.isTransferComplete()).isFalse();
                assertStreamMatchesObject(stream);
            }

            listener.getCompletionFuture().get(1, TimeUnit.MINUTES);
            assertThat(listener.isTransferInitiated()).isTrue();
            assertThat(listener.isTransferComplete()).isTrue();
            assertThat(listener.getExceptionCaught()).isNull();
            assertThat(listener.getRatioTransferredList()).contains(1.0);
            assertThat(download.progress().snapshot().totalBytes()).hasValue((long) OBJECT_SIZE);
            assertThat(download.progress().snapshot().transferredBytes()).isEqualTo(OBJECT_SIZE);
            assertThat(download.progress().snapshot().sdkResponse()).isPresent();
            assertThat(((GetObjectResponse) download.progress().snapshot().sdkResponse().get())
                           .responseMetadata()
                           .requestId()).isNotNull();
        }
    }

    @Test
    void downloadWithDirectBufferPool_closeEarly_shouldReportCancellationAndAllowNextDownload() throws Exception {
        try (S3AsyncClient client = pooledClient();
             S3TransferManager transferManager = S3TransferManager.builder().s3Client(client).build()) {
            CountingTransferListener listener = new CountingTransferListener();
            Download<ResponseInputStream<GetObjectResponse>> download =
                transferManager.downloadWithDirectBufferPool(pooledRequest(listener));

            try (ResponseInputStream<GetObjectResponse> stream = download.completionFuture().get(1, TimeUnit.MINUTES).result()) {
                assertThat(stream.read()).isGreaterThanOrEqualTo(0);
            }

            assertThatThrownBy(() -> listener.terminalFuture().get(1, TimeUnit.MINUTES))
                .isInstanceOf(CancellationException.class);

            try (ResponseInputStream<GetObjectResponse> stream = transferManager.downloadWithDirectBufferPool(pooledRequest())
                                                                                .completionFuture()
                                                                                .get(1, TimeUnit.MINUTES)
                                                                                .result()) {
                assertStreamMatchesObject(stream);
            }

            assertThat(listener.initiated()).hasValue(1);
            assertThat(listener.completed()).hasValue(0);
            assertThat(listener.failed()).hasValue(1);
            assertThat(listener.failure()).isInstanceOf(CancellationException.class);
        }
    }

    private static S3AsyncClient pooledClient() {
        return S3AsyncClient.crtBuilder()
                            .region(DEFAULT_REGION)
                            .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                            .minimumPartSizeInBytes((long) PART_SIZE)
                            .initialReadBufferSizeInBytes((long) PART_SIZE)
                            .directBufferPoolConfiguration(S3CrtDirectBufferPoolConfiguration.fixed(2L * PART_SIZE))
                            .build();
    }

    private static DirectBufferPoolDownloadRequest<ResponseInputStream<GetObjectResponse>> pooledRequest() {
        return DirectBufferPoolDownloadRequest.builder()
                                              .getObjectRequest(b -> b.bucket(BUCKET).key(KEY))
                                              .responseTransformer(
                                                  S3CrtResponseTransformerFactory.toBlockingInputStreamFromDirectBufferPool())
                                              .build();
    }

    private static DirectBufferPoolDownloadRequest<ResponseInputStream<GetObjectResponse>> pooledRequest(
        TransferListener listener) {
        return DirectBufferPoolDownloadRequest.builder()
                                              .getObjectRequest(b -> b.bucket(BUCKET).key(KEY))
                                              .addTransferListener(listener)
                                              .responseTransformer(
                                                  S3CrtResponseTransformerFactory.toBlockingInputStreamFromDirectBufferPool())
                                              .build();
    }

    private static void assertStreamMatchesObject(ResponseInputStream<GetObjectResponse> stream) throws IOException {
        Path downloadedObject = RandomTempFile.randomUncreatedFile().toPath();
        try {
            Files.copy(stream, downloadedObject);
            assertThat(Md5Utils.md5AsBase64(downloadedObject.toFile())).isEqualTo(Md5Utils.md5AsBase64(object));
        } finally {
            Files.deleteIfExists(downloadedObject);
        }
    }

    private static final class CountingTransferListener implements TransferListener {
        private final AtomicInteger initiated = new AtomicInteger();
        private final AtomicInteger completed = new AtomicInteger();
        private final AtomicInteger failed = new AtomicInteger();
        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private final CompletableFuture<Void> terminalFuture = new CompletableFuture<>();

        @Override
        public void transferInitiated(Context.TransferInitiated context) {
            initiated.incrementAndGet();
        }

        @Override
        public void transferComplete(Context.TransferComplete context) {
            completed.incrementAndGet();
            terminalFuture.complete(null);
        }

        @Override
        public void transferFailed(Context.TransferFailed context) {
            failed.incrementAndGet();
            failure.set(context.exception());
            terminalFuture.completeExceptionally(context.exception());
        }

        private AtomicInteger initiated() {
            return initiated;
        }

        private AtomicInteger completed() {
            return completed;
        }

        private AtomicInteger failed() {
            return failed;
        }

        private Throwable failure() {
            return failure.get();
        }

        private CompletableFuture<Void> terminalFuture() {
            return terminalFuture;
        }
    }
}
