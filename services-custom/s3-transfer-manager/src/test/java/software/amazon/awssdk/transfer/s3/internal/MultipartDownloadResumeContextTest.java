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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.MULTIPART_DOWNLOAD_RESUME_CONTEXT;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;

public class MultipartDownloadResumeContextTest {

    S3AsyncClient s3;
    S3TransferManager tm;

    @BeforeEach
    void init() {
        this.s3 = mock(S3AsyncClient.class);
        this.tm = new GenericS3TransferManager(s3,
                                               mock(UploadDirectoryHelper.class),
                                               mock(TransferManagerConfiguration.class),
                                               mock(DownloadDirectoryHelper.class));
    }

    @Test
    void pauseAndResume_shouldKeepMultipartContext() {
        CompletableFuture<GetObjectResponse> future = new CompletableFuture<>();
        when(s3.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(future);
        when(s3.headObject(any(Consumer.class)))
            .thenReturn(new CompletableFuture<>());

        GetObjectRequest req = GetObjectRequest.builder().key("key").bucket("bucket").build();

        FileDownload dl = tm.downloadFile(
            DownloadFileRequest.builder()
                               .destination(Paths.get("some", "path"))
                               .getObjectRequest(req)
                               .build());
        ResumableFileDownload resume = dl.pause();

        assertThat(resume.downloadFileRequest().getObjectRequest())
            .matches(hasMultipartContextAttribute(), "[1] hasMultipartContextAttribute");

        FileDownload dl2 = tm.resumeDownloadFile(resume);
        ResumableFileDownload resume2 = dl2.pause();

        assertThat(resume2.downloadFileRequest().getObjectRequest())
            .matches(hasMultipartContextAttribute(), "[2] hasMultipartContextAttribute");
    }

    private Predicate<GetObjectRequest> hasMultipartContextAttribute() {
        return getObjectRequest -> {
            if (!getObjectRequest.overrideConfiguration().isPresent()) {
                return false;
            }

            return getObjectRequest.overrideConfiguration()
                                   .get()
                                   .executionAttributes()
                                   .getAttribute(MULTIPART_DOWNLOAD_RESUME_CONTEXT)
                   != null;
        };
    }

}
