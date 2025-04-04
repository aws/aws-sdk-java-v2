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

package software.amazon.awssdk.transfer.s3;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;
import software.amazon.awssdk.utils.Logger;

public class S3TransferManagerDownloadPauseResumeIntegrationTest extends S3IntegrationTestBase {
    private static final Logger log = Logger.loggerFor(S3TransferManagerDownloadPauseResumeIntegrationTest.class);
    private static final String BUCKET = temporaryBucketName(S3TransferManagerDownloadPauseResumeIntegrationTest.class);
    private static final String KEY = "key";
    // 24 * MB is chosen to make sure we have data written in the file already upon pausing.
    private static final long OBJ_SIZE = 24 * MB;
    private static File sourceFile;

    @BeforeAll
    public static void setup() throws Exception {
        createBucket(BUCKET);
        sourceFile = new RandomTempFile(OBJ_SIZE);
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .build(), sourceFile.toPath());
    }

    @AfterAll
    public static void cleanup() {
        deleteBucketAndAllContents(BUCKET);
        sourceFile.delete();
    }

    @ParameterizedTest
    @MethodSource("transferManagers")
    void pauseAndResume_ObjectNotChanged_shouldResumeDownload(S3TransferManager tm) {
        Path path = RandomTempFile.randomUncreatedFile().toPath();
        TestDownloadListener testDownloadListener = new TestDownloadListener();
        DownloadFileRequest request = DownloadFileRequest.builder()
                                                         .getObjectRequest(b -> b.bucket(BUCKET).key(KEY))
                                                         .destination(path)
                                                         .addTransferListener(testDownloadListener)
                                                         .build();
        FileDownload download = tm.downloadFile(request);
        waitUntilFirstByteBufferDelivered(download);

        ResumableFileDownload resumableFileDownload = download.pause();
        long bytesTransferred = resumableFileDownload.bytesTransferred();
        log.debug(() -> "Paused: " + resumableFileDownload);
        assertEqualsBySdkFields(resumableFileDownload.downloadFileRequest(), request);
        assertThat(testDownloadListener.getObjectResponse).isNotNull();
        assertThat(resumableFileDownload.s3ObjectLastModified()).hasValue(testDownloadListener.getObjectResponse.lastModified());
        assertThat(bytesTransferred).isEqualTo(path.toFile().length());
        assertThat(resumableFileDownload.totalSizeInBytes()).hasValue(sourceFile.length());

        assertThat(bytesTransferred).isLessThan(sourceFile.length());
        assertThat(download.completionFuture()).isDone();

        log.debug(() -> "Resuming download ");
        verifyFileDownload(path, resumableFileDownload, OBJ_SIZE - bytesTransferred, tm);
    }

    private void assertEqualsBySdkFields(DownloadFileRequest actual, DownloadFileRequest expected) {
        // Transfer manager adds an execution attribute to the GetObjectRequest, so both objects are different.
        // Need to assert equality by sdk fields, which does not check execution attributes.
        assertThat(actual.destination())
            .withFailMessage("ResumableFileDownload destination not equal to the original DownloadFileRequest")
            .isEqualTo(expected.destination());
        assertThat(actual.transferListeners())
            .withFailMessage("ResumableFileDownload transferListeners not equal to the original DownloadFileRequest")
            .isEqualTo(expected.transferListeners());
        assertTrue(actual.getObjectRequest().equalsBySdkFields(expected.getObjectRequest()),
                   () -> String.format("ResumableFileDownload GetObjectRequest not equal to the original DownloadFileRequest. "
                                       + "expected: %s. Actual:"
                                       + " %s", actual.getObjectRequest(), expected.getObjectRequest()));
    }

    @ParameterizedTest
    @MethodSource("transferManagers")
    void pauseAndResume_objectChanged_shouldStartFromBeginning(S3TransferManager tm) {
        try {
            Path path = RandomTempFile.randomUncreatedFile().toPath();
            DownloadFileRequest request = DownloadFileRequest.builder()
                                                             .getObjectRequest(b -> b.bucket(BUCKET).key(KEY))
                                                             .destination(path)
                                                             .build();
            FileDownload download = tm.downloadFile(request);
            waitUntilFirstByteBufferDelivered(download);

            ResumableFileDownload resumableFileDownload = download.pause();
            log.debug(() -> "Paused: " + resumableFileDownload);
            String newObject = RandomStringUtils.randomAlphanumeric(1000);

            // Re-upload the S3 object
            s3.putObject(PutObjectRequest.builder()
                                         .bucket(BUCKET)
                                         .key(KEY)
                                         .build(), RequestBody.fromString(newObject));

            log.debug(() -> "Resuming download ");
            FileDownload resumedFileDownload = tm.resumeDownloadFile(resumableFileDownload);
            resumedFileDownload.progress().snapshot();
            resumedFileDownload.completionFuture().join();
            assertThat(path.toFile()).hasContent(newObject);
            assertThat(resumedFileDownload.progress().snapshot().totalBytes()).hasValue((long) newObject.getBytes(StandardCharsets.UTF_8).length);
        } finally {
            s3.putObject(PutObjectRequest.builder()
                                         .bucket(BUCKET)
                                         .key(KEY)
                                         .build(), sourceFile.toPath());
        }
    }

    @ParameterizedTest
    @MethodSource("transferManagers")
    void pauseAndResume_fileChanged_shouldStartFromBeginning(S3TransferManager tm) throws Exception {
        Path path = RandomTempFile.randomUncreatedFile().toPath();
        DownloadFileRequest request = DownloadFileRequest.builder()
                                                         .getObjectRequest(b -> b.bucket(BUCKET).key(KEY))
                                                         .destination(path)
                                                         .build();
        FileDownload download = tm.downloadFile(request);
        waitUntilFirstByteBufferDelivered(download);

        ResumableFileDownload resumableFileDownload = download.pause();
        Files.write(path, "helloworld".getBytes(StandardCharsets.UTF_8));

        verifyFileDownload(path, resumableFileDownload, OBJ_SIZE, tm);
    }

    private static void verifyFileDownload(Path path, ResumableFileDownload resumableFileDownload,
                                           long expectedBytesTransferred, S3TransferManager tm) {
        FileDownload resumedFileDownload = tm.resumeDownloadFile(resumableFileDownload);
        resumedFileDownload.completionFuture().join();
        assertThat(resumedFileDownload.progress().snapshot().totalBytes()).hasValue(expectedBytesTransferred);
        assertThat(path.toFile()).hasSameBinaryContentAs(sourceFile);
    }

    private static void waitUntilFirstByteBufferDelivered(FileDownload download) {
        Waiter<TransferProgressSnapshot> waiter = Waiter.builder(TransferProgressSnapshot.class)
                                                        .addAcceptor(WaiterAcceptor.successOnResponseAcceptor(r -> r.transferredBytes() > 0))
                                                        .addAcceptor(WaiterAcceptor.retryOnResponseAcceptor(r -> true))
                                                        .overrideConfiguration(o -> o.waitTimeout(Duration.ofMinutes(1))
                                                                                     .maxAttempts(Integer.MAX_VALUE)
                                                                                     .backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofMillis(100))))
                                                        .build();
        waiter.run(() -> download.progress().snapshot());
    }

    private static final class TestDownloadListener implements TransferListener {
        private GetObjectResponse getObjectResponse;

        @Override
        public void bytesTransferred(Context.BytesTransferred context) {
            Optional<SdkResponse> sdkResponse = context.progressSnapshot().sdkResponse();
            if (sdkResponse.isPresent() && sdkResponse.get() instanceof GetObjectResponse) {
                getObjectResponse = (GetObjectResponse) sdkResponse.get();
            }
        }

        @Override
        public void transferFailed(Context.TransferFailed context) {
            Optional<SdkResponse> sdkResponse = context.progressSnapshot().sdkResponse();
            if (sdkResponse.isPresent() && sdkResponse.get() instanceof GetObjectResponse) {
                getObjectResponse = (GetObjectResponse) sdkResponse.get();
            }
        }
    }

}
