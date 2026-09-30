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

package software.amazon.awssdk.transfer.s3.util;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.transfer.s3.internal.utils.ResumableRequestConverter.toDownloadFileRequestAndTransformer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.utils.Pair;

/**
 * Tests that resuming a part-scoped download restarts from the beginning instead of adding a Range header.
 */
class ResumableRequestConverterPartNumberResumeTest {

    private static final long WHOLE_OBJECT_SIZE = 8 * 1024 * 1024;
    private static final long BYTES_TRANSFERRED = 1024 * 1024;

    private File file;
    private Instant s3ObjectLastModified;

    @BeforeEach
    void setUp() throws IOException {
        file = RandomTempFile.createTempFile("test", UUID.randomUUID().toString());
        Files.write(file.toPath(), RandomStringUtils.randomAlphanumeric((int) BYTES_TRANSFERRED)
                                                    .getBytes(StandardCharsets.UTF_8));
        s3ObjectLastModified = Instant.now();
    }

    @AfterEach
    void tearDown() {
        file.delete();
    }

    @Test
    void resumeWithPartNumber_shouldRestartFromBeginning() {
        GetObjectRequest originalGetRequest = GetObjectRequest.builder()
                                                              .bucket("test-bucket")
                                                              .key("test-key")
                                                              .partNumber(3)
                                                              .build();

        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                     .getObjectRequest(originalGetRequest)
                                                                     .destination(file)
                                                                     .build();

        Instant fileLastModified = Instant.ofEpochMilli(file.lastModified());
        ResumableFileDownload resumableFileDownload = ResumableFileDownload.builder()
                                                                           .bytesTransferred(BYTES_TRANSFERRED)
                                                                           .s3ObjectLastModified(s3ObjectLastModified)
                                                                           .fileLastModified(fileLastModified)
                                                                           .downloadFileRequest(downloadFileRequest)
                                                                           .totalSizeInBytes(WHOLE_OBJECT_SIZE)
                                                                           .build();

        HeadObjectResponse headObjectResponse = HeadObjectResponse.builder()
                                                                   .contentLength(WHOLE_OBJECT_SIZE)
                                                                   .lastModified(s3ObjectLastModified)
                                                                   .build();

        Pair<DownloadFileRequest, AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> result =
            toDownloadFileRequestAndTransformer(resumableFileDownload, headObjectResponse, downloadFileRequest);

        GetObjectRequest resumedRequest = result.left().getObjectRequest();

        // partNumber and Range cannot coexist, S3 rejects with 400 InvalidRequest
        assertThat(resumedRequest.partNumber()).isEqualTo(3);
        assertThat(resumedRequest.range()).isNull();
    }

    @Test
    void resumeWithoutPartNumber_shouldStillAddRangeHeader() {
        GetObjectRequest originalGetRequest = GetObjectRequest.builder()
                                                              .bucket("test-bucket")
                                                              .key("test-key")
                                                              .build();

        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                     .getObjectRequest(originalGetRequest)
                                                                     .destination(file)
                                                                     .build();

        Instant fileLastModified = Instant.ofEpochMilli(file.lastModified());
        ResumableFileDownload resumableFileDownload = ResumableFileDownload.builder()
                                                                           .bytesTransferred(BYTES_TRANSFERRED)
                                                                           .s3ObjectLastModified(s3ObjectLastModified)
                                                                           .fileLastModified(fileLastModified)
                                                                           .downloadFileRequest(downloadFileRequest)
                                                                           .build();

        HeadObjectResponse headObjectResponse = HeadObjectResponse.builder()
                                                                   .contentLength(WHOLE_OBJECT_SIZE)
                                                                   .lastModified(s3ObjectLastModified)
                                                                   .build();

        Pair<DownloadFileRequest, AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> result =
            toDownloadFileRequestAndTransformer(resumableFileDownload, headObjectResponse, downloadFileRequest);

        GetObjectRequest resumedRequest = result.left().getObjectRequest();

        assertThat(resumedRequest.partNumber()).isNull();
        assertThat(resumedRequest.range()).isNotNull();
    }
}
