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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

import com.google.common.jimfs.Jimfs;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.internal.multipart.KnownContentLengthAsyncRequestBodySubscriber;
import software.amazon.awssdk.services.s3.internal.multipart.PausableUpload;
import software.amazon.awssdk.services.s3.multipart.PauseObservable;
import software.amazon.awssdk.services.s3.multipart.S3ResumeToken;
import software.amazon.awssdk.services.s3.internal.multipart.UploadWithKnownContentLengthHelper;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultFileUpload;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;

class DefaultFileUploadTest {
    private static final int TOTAL_PARTS = 10;
    private static final int NUM_OF_PARTS_COMPLETED = 5;
    private static final long PART_SIZE_IN_BYTES = 8 * MB;
    private static final String MULTIPART_UPLOAD_ID = "someId";
    private KnownContentLengthAsyncRequestBodySubscriber uploadPartSubscriber;
    private static FileSystem fileSystem;
    private static File file;
    private static S3ResumeToken token;

    @BeforeAll
    public static void setUp() throws IOException {
        fileSystem = Jimfs.newFileSystem();
        file = File.createTempFile("test", UUID.randomUUID().toString());
        Files.write(file.toPath(), RandomStringUtils.random(2000).getBytes(StandardCharsets.UTF_8));
        token = S3ResumeToken.builder()
                             .uploadId(MULTIPART_UPLOAD_ID)
                             .totalNumParts(TOTAL_PARTS)
                             .numPartsCompleted(NUM_OF_PARTS_COMPLETED)
                             .partSize(PART_SIZE_IN_BYTES)
                             .build();
    }

    @AfterAll
    public static void tearDown() throws IOException {
        file.delete();
    }

    @BeforeEach
    void setUpBeforeEachTest() {
        uploadPartSubscriber =
            Mockito.mock(KnownContentLengthAsyncRequestBodySubscriber.class);
    }

    @Test
    void equals_hashcode() {
        EqualsVerifier.forClass(DefaultFileUpload.class)
                      .withNonnullFields("completionFuture", "progress", "request", "resumableFileUpload", "pauseObservable")
                      .withPrefabValues(PauseObservable.class, new PauseObservable(), new PauseObservable())
                      .verify();
    }

@Test
    void pause_futureCompleted_shouldReturnNormally() {
        CompletableFuture<CompletedFileUpload> future =
            CompletableFuture.completedFuture(CompletedFileUpload.builder()
                                                                 .response(PutObjectResponse.builder().build())
                                                                 .build());
        TransferProgress transferProgress = Mockito.mock(TransferProgress.class);
        PauseObservable observable = new PauseObservable();
        PausableUpload pausableUpload = UploadWithKnownContentLengthHelper.defaultPausableUpload(uploadPartSubscriber);
        observable.setPausableUpload(pausableUpload);
        UploadFileRequest request = uploadFileRequest();
        DefaultFileUpload fileUpload = new DefaultFileUpload(future, transferProgress, observable, request);

        ResumableFileUpload resumableFileUpload = fileUpload.pause();

        Mockito.verify(uploadPartSubscriber, Mockito.never()).pause();
        assertThat(resumableFileUpload.totalParts()).isEmpty();
        assertThat(resumableFileUpload.partSizeInBytes()).isEmpty();
        assertThat(resumableFileUpload.multipartUploadId()).isEmpty();
        assertThat(resumableFileUpload.fileLength()).isEqualTo(file.length());
        assertThat(resumableFileUpload.uploadFileRequest()).isEqualTo(request);
        assertThat(resumableFileUpload.fileLastModified()).isEqualTo(Instant.ofEpochMilli(file.lastModified()));
    }


    @Test
    void pauseTwice_shouldReturnTheSame() {
        CompletableFuture<CompletedFileUpload> future = new CompletableFuture<>();
        TransferProgress transferProgress = Mockito.mock(TransferProgress.class);
        PauseObservable observable = new PauseObservable();
        PausableUpload pausableUpload = UploadWithKnownContentLengthHelper.defaultPausableUpload(uploadPartSubscriber);
        observable.setPausableUpload(pausableUpload);
        UploadFileRequest request = uploadFileRequest();
        DefaultFileUpload fileUpload = new DefaultFileUpload(future, transferProgress, observable, request);
        when(uploadPartSubscriber.pause()).thenReturn(token);

        ResumableFileUpload resumableFileUpload = fileUpload.pause();
        ResumableFileUpload resumableFileUpload2 = fileUpload.pause();

        assertThat(resumableFileUpload).isEqualTo(resumableFileUpload2);
    }

    @Test
    void pause_futureNotComplete_shouldPause() {
        CompletableFuture<CompletedFileUpload> future =
            new CompletableFuture<>();
        TransferProgress transferProgress = Mockito.mock(TransferProgress.class);
        when(transferProgress.snapshot()).thenReturn(DefaultTransferProgressSnapshot.builder()
                                                                                    .transferredBytes(0L)
                                                                                    .build());


        PauseObservable observable = new PauseObservable();
        PausableUpload pausableUpload = UploadWithKnownContentLengthHelper.defaultPausableUpload(uploadPartSubscriber);
        observable.setPausableUpload(pausableUpload);

        UploadFileRequest request = uploadFileRequest();
        DefaultFileUpload fileUpload = new DefaultFileUpload(future, transferProgress, observable, request);

        when(uploadPartSubscriber.pause()).thenReturn(token);
        ResumableFileUpload resumableFileUpload = fileUpload.pause();

        Mockito.verify(uploadPartSubscriber).pause();
        assertThat(resumableFileUpload.totalParts()).hasValue(TOTAL_PARTS);
        assertThat(resumableFileUpload.partSizeInBytes()).hasValue(PART_SIZE_IN_BYTES);
        assertThat(resumableFileUpload.multipartUploadId()).hasValue(MULTIPART_UPLOAD_ID);
        assertThat(resumableFileUpload.transferredParts()).hasValue(NUM_OF_PARTS_COMPLETED);
        assertThat(resumableFileUpload.fileLength()).isEqualTo(file.length());
        assertThat(resumableFileUpload.uploadFileRequest()).isEqualTo(request);
        assertThat(resumableFileUpload.fileLastModified()).isEqualTo(Instant.ofEpochMilli(file.lastModified()));
    }

    @Test
    void pause_singlePart_shouldPause() {
        CompletableFuture<CompletedFileUpload> future =
            new CompletableFuture<>();
        TransferProgress transferProgress = Mockito.mock(TransferProgress.class);

        PauseObservable observable = new PauseObservable();
        PausableUpload pausableUpload = UploadWithKnownContentLengthHelper.defaultPausableUpload(uploadPartSubscriber);
        observable.setPausableUpload(pausableUpload);

        UploadFileRequest request = uploadFileRequest();
        DefaultFileUpload fileUpload = new DefaultFileUpload(future, transferProgress, observable, request);

        when(uploadPartSubscriber.pause()).thenReturn(null);

        ResumableFileUpload resumableFileUpload = fileUpload.pause();
        Mockito.verify(uploadPartSubscriber).pause();
        assertThat(resumableFileUpload.totalParts()).isEmpty();
        assertThat(resumableFileUpload.partSizeInBytes()).isEmpty();
        assertThat(resumableFileUpload.multipartUploadId()).isEmpty();
        assertThat(resumableFileUpload.fileLength()).isEqualTo(file.length());
        assertThat(resumableFileUpload.uploadFileRequest()).isEqualTo(request);
        assertThat(resumableFileUpload.fileLastModified()).isEqualTo(Instant.ofEpochMilli(file.lastModified()));
    }

    private UploadFileRequest uploadFileRequest() {
        return UploadFileRequest.builder()
                                .source(file)
                                .putObjectRequest(p -> p.key("test").bucket("bucket"))
                                .build();
    }
}