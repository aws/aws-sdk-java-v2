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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsResponse;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.awssdk.utils.Logger;

public class S3TransferManagerUploadPauseResumeIntegrationTest extends S3IntegrationTestBase {
    private static final Logger log = Logger.loggerFor(S3TransferManagerUploadPauseResumeIntegrationTest.class);
    private static final String BUCKET = temporaryBucketName(S3TransferManagerUploadPauseResumeIntegrationTest.class);
    private static final String KEY = "key";
    // 24 * MB is chosen to make sure we have data written in the file already upon pausing.
    private static final long OBJ_SIZE = 24 * MB;
    private static File largeFile;
    private static File smallFile;

    @BeforeAll
    public static void setup() throws Exception {
        createBucket(BUCKET);
        largeFile = new RandomTempFile(OBJ_SIZE);
        smallFile = new RandomTempFile(2 * MB);
    }

    @AfterAll
    public static void cleanup() {
        deleteBucketAndAllContents(BUCKET);
        largeFile.delete();
        smallFile.delete();
    }

    @Test
    void pause_singlePart_shouldResume() {
        UploadFileRequest request = UploadFileRequest.builder()
                                                     .putObjectRequest(b -> b.bucket(BUCKET).key(KEY))
                                                     .source(smallFile)
                                                     .build();
        FileUpload fileUpload = tm.uploadFile(request);
        ResumableFileUpload resumableFileUpload = fileUpload.pause();
        log.debug(() -> "Paused: " + resumableFileUpload);

        validateEmptyResumeToken(resumableFileUpload);

        FileUpload resumedUpload = tm.resumeUploadFile(resumableFileUpload);
        resumedUpload.completionFuture().join();
    }

    @Test
    void pause_fileNotChanged_shouldResume() {
        UploadFileRequest request = UploadFileRequest.builder()
                                                     .putObjectRequest(b -> b.bucket(BUCKET).key(KEY))
                                                     .addTransferListener(LoggingTransferListener.create())
                                                     .source(largeFile)
                                                     .build();
        FileUpload fileUpload = tm.uploadFile(request);
        waitUntilMultipartUploadExists();
        ResumableFileUpload resumableFileUpload = fileUpload.pause();
        log.debug(() -> "Paused: " + resumableFileUpload);

        assertThat(resumableFileUpload.multipartUploadId()).isNotEmpty();
        assertThat(resumableFileUpload.partSizeInBytes()).isNotEmpty();
        assertThat(resumableFileUpload.totalParts()).isNotEmpty();

        verifyMultipartUploadIdExists(resumableFileUpload);

        FileUpload resumedUpload = tm.resumeUploadFile(resumableFileUpload);
        resumedUpload.completionFuture().join();
    }

    @Test
    void pauseImmediately_resume_shouldStartFromBeginning() {
        UploadFileRequest request = UploadFileRequest.builder()
                                                   .putObjectRequest(b -> b.bucket(BUCKET).key(KEY))
                                                   .source(largeFile)
                                                   .build();
        FileUpload fileUpload = tm.uploadFile(request);
        ResumableFileUpload resumableFileUpload = fileUpload.pause();
        log.debug(() -> "Paused: " + resumableFileUpload);

        validateEmptyResumeToken(resumableFileUpload);

        FileUpload resumedUpload = tm.resumeUploadFile(resumableFileUpload);
        resumedUpload.completionFuture().join();
    }

    @Test
    void pause_fileChanged_resumeShouldStartFromBeginning() throws Exception {
        UploadFileRequest request = UploadFileRequest.builder()
                                                     .putObjectRequest(b -> b.bucket(BUCKET).key(KEY))
                                                     .source(largeFile)
                                                     .build();
        FileUpload fileUpload = tm.uploadFile(request);
        waitUntilMultipartUploadExists();
        ResumableFileUpload resumableFileUpload = fileUpload.pause();
        log.debug(() -> "Paused: " + resumableFileUpload);

        assertThat(resumableFileUpload.multipartUploadId()).isNotEmpty();
        assertThat(resumableFileUpload.partSizeInBytes()).isNotEmpty();
        assertThat(resumableFileUpload.totalParts()).isNotEmpty();
        verifyMultipartUploadIdExists(resumableFileUpload);

        byte[] bytes = "helloworld".getBytes(StandardCharsets.UTF_8);
        Files.write(largeFile.toPath(), bytes);

        FileUpload resumedUpload = tm.resumeUploadFile(resumableFileUpload);
        resumedUpload.completionFuture().join();
        verifyMultipartUploadIdNotExist(resumableFileUpload);
        assertThat(resumedUpload.progress().snapshot().totalBytes()).hasValue(bytes.length);
    }

    private void verifyMultipartUploadIdExists(ResumableFileUpload resumableFileUpload) {
        String multipartUploadId = resumableFileUpload.multipartUploadId().get();
        ListPartsResponse listMultipartUploadsResponse =
            s3Async.listParts(r -> r.uploadId(multipartUploadId).bucket(BUCKET).key(KEY)).join();
        assertThat(listMultipartUploadsResponse).isNotNull();
    }

    private void verifyMultipartUploadIdNotExist(ResumableFileUpload resumableFileUpload) {
        String multipartUploadId = resumableFileUpload.multipartUploadId().get();
        assertThatThrownBy(() -> s3Async.listParts(r -> r.uploadId(multipartUploadId).bucket(BUCKET).key(KEY)).join())
            .hasCauseInstanceOf(NoSuchUploadException.class);
    }

    private static void waitUntilMultipartUploadExists() {
        Waiter<ListMultipartUploadsResponse> waiter = Waiter.builder(ListMultipartUploadsResponse.class)
                                                        .addAcceptor(WaiterAcceptor.successOnResponseAcceptor(ListMultipartUploadsResponse::hasUploads))
                                                        .addAcceptor(WaiterAcceptor.retryOnResponseAcceptor(r -> true))
                                                        .overrideConfiguration(o -> o.waitTimeout(Duration.ofMinutes(1))
                                                                                     .maxAttempts(10)
                                                                                     .backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofMillis(100))))
                                                        .build();
        waiter.run(() -> s3.listMultipartUploads(l -> l.bucket(BUCKET)));
    }

    private static void validateEmptyResumeToken(ResumableFileUpload resumableFileUpload) {
        assertThat(resumableFileUpload.multipartUploadId()).isEmpty();
        assertThat(resumableFileUpload.partSizeInBytes()).isEmpty();
        assertThat(resumableFileUpload.totalParts()).isEmpty();
        assertThat(resumableFileUpload.transferredParts()).isEmpty();
    }
}
