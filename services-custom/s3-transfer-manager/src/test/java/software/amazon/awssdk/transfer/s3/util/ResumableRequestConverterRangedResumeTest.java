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
 * Tests for resuming ranged downloads.
 * Verifies that the resumed Range header correctly accounts for the original range offset.
 */
class ResumableRequestConverterRangedResumeTest {

    private static final long WHOLE_OBJECT_SIZE = 8 * 1024 * 1024;
    private static final long ORIGINAL_RANGE_START = 2 * 1024 * 1024;
    private static final long ORIGINAL_RANGE_END = 6 * 1024 * 1024 - 1;
    private static final String ORIGINAL_RANGE = "bytes=" + ORIGINAL_RANGE_START + "-" + ORIGINAL_RANGE_END;
    private static final long BYTES_TRANSFERRED = 1024 * 1024;
    private static final long EXPECTED_RESUME_START = ORIGINAL_RANGE_START + BYTES_TRANSFERRED;
    private static final String EXPECTED_RESUMED_RANGE = "bytes=" + EXPECTED_RESUME_START + "-" + ORIGINAL_RANGE_END;

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
    void resumeRangedDownload_shouldComputeCorrectResumedRange() {
        GetObjectRequest originalGetRequest = GetObjectRequest.builder()
                                                              .bucket("test-bucket")
                                                              .key("test-key")
                                                              .range(ORIGINAL_RANGE)
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

        assertThat(resumedRequest.range())
            .isEqualTo(EXPECTED_RESUMED_RANGE);
    }

    @Test
    void resumeNonRangedDownload_shouldComputeRangeFromBytesTransferred() {
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

        String expectedRange = "bytes=" + BYTES_TRANSFERRED + "-" + WHOLE_OBJECT_SIZE;
        assertThat(resumedRequest.range()).isEqualTo(expectedRange);
    }

    @Test
    void resumeRangedDownload_resumedRangeEndShouldMatchOriginalRangeEnd() {
        GetObjectRequest originalGetRequest = GetObjectRequest.builder()
                                                              .bucket("test-bucket")
                                                              .key("test-key")
                                                              .range(ORIGINAL_RANGE)
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
        String range = resumedRequest.range();
        String rangeValue = range.replace("bytes=", "");
        long rangeEnd = Long.parseLong(rangeValue.split("-")[1]);

        assertThat(rangeEnd).isEqualTo(ORIGINAL_RANGE_END);
    }

    @Test
    void resumeRangedDownload_rangeStartingAtZero_shouldComputeCorrectRange() {
        long rangeEnd = 4 * 1024 * 1024 - 1;
        String originalRange = "bytes=0-" + rangeEnd;

        GetObjectRequest originalGetRequest = GetObjectRequest.builder()
                                                              .bucket("test-bucket")
                                                              .key("test-key")
                                                              .range(originalRange)
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

        String expectedRange = "bytes=" + BYTES_TRANSFERRED + "-" + rangeEnd;
        assertThat(resumedRequest.range()).isEqualTo(expectedRange);
    }

    @Test
    void resumeRangedDownload_rangeNearEndOfObject_shouldComputeCorrectRange() throws IOException {
        long rangeStart = 7 * 1024 * 1024;
        long rangeEnd = WHOLE_OBJECT_SIZE - 1;
        long transferred = 512 * 1024;
        String originalRange = "bytes=" + rangeStart + "-" + rangeEnd;

        File smallFile = RandomTempFile.createTempFile("test-small", UUID.randomUUID().toString());
        try {
            Files.write(smallFile.toPath(), RandomStringUtils.randomAlphanumeric((int) transferred)
                                                             .getBytes(StandardCharsets.UTF_8));

            GetObjectRequest originalGetRequest = GetObjectRequest.builder()
                                                                  .bucket("test-bucket")
                                                                  .key("test-key")
                                                                  .range(originalRange)
                                                                  .build();

            DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                         .getObjectRequest(originalGetRequest)
                                                                         .destination(smallFile)
                                                                         .build();

            Instant fileLastModified = Instant.ofEpochMilli(smallFile.lastModified());
            ResumableFileDownload resumableFileDownload = ResumableFileDownload.builder()
                                                                               .bytesTransferred(transferred)
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

            String expectedRange = "bytes=" + (rangeStart + transferred) + "-" + rangeEnd;
            assertThat(resumedRequest.range()).isEqualTo(expectedRange);
        } finally {
            smallFile.delete();
        }
    }

    @Test
    void resumeRangedDownload_openEndedRange_shouldUseContentLengthAsEnd() {
        long rangeStart = 2 * 1024 * 1024;
        String originalRange = "bytes=" + rangeStart + "-";

        GetObjectRequest originalGetRequest = GetObjectRequest.builder()
                                                              .bucket("test-bucket")
                                                              .key("test-key")
                                                              .range(originalRange)
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

        String expectedRange = "bytes=" + (rangeStart + BYTES_TRANSFERRED) + "-" + (WHOLE_OBJECT_SIZE - 1);
        assertThat(resumedRequest.range()).isEqualTo(expectedRange);
    }

    @Test
    void resumeNonRangedDownload_doubleResume_shouldNotDoubleCountOffset() throws IOException {
        long firstTransferred = 1000;
        long secondTransferred = 3000;

        File doubleResumeFile = RandomTempFile.createTempFile("test-double", UUID.randomUUID().toString());
        try {
            Files.write(doubleResumeFile.toPath(), RandomStringUtils.randomAlphanumeric((int) secondTransferred)
                                                                    .getBytes(StandardCharsets.UTF_8));

            GetObjectRequest originalGetRequest = GetObjectRequest.builder()
                                                                  .bucket("test-bucket")
                                                                  .key("test-key")
                                                                  .build();

            DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                         .getObjectRequest(originalGetRequest)
                                                                         .destination(doubleResumeFile)
                                                                         .build();

            HeadObjectResponse headObjectResponse = HeadObjectResponse.builder()
                                                                       .contentLength(WHOLE_OBJECT_SIZE)
                                                                       .lastModified(s3ObjectLastModified)
                                                                       .build();

            // Simulate second resume: the token still has the original request (no range),
            // but bytesTransferred is the total file size after two partial downloads.
            Instant fileLastModified = Instant.ofEpochMilli(doubleResumeFile.lastModified());
            ResumableFileDownload secondToken = ResumableFileDownload.builder()
                                                                     .bytesTransferred(secondTransferred)
                                                                     .s3ObjectLastModified(s3ObjectLastModified)
                                                                     .fileLastModified(fileLastModified)
                                                                     .downloadFileRequest(downloadFileRequest)
                                                                     .build();

            Pair<DownloadFileRequest, AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> result =
                toDownloadFileRequestAndTransformer(secondToken, headObjectResponse, downloadFileRequest);

            GetObjectRequest resumedRequest = result.left().getObjectRequest();

            String expectedRange = "bytes=" + secondTransferred + "-" + WHOLE_OBJECT_SIZE;
            assertThat(resumedRequest.range()).isEqualTo(expectedRange);
        } finally {
            doubleResumeFile.delete();
        }
    }

    @Test
    void resumeRangedDownload_doubleResume_shouldNotDoubleCountOffset() throws IOException {
        long firstTransferred = BYTES_TRANSFERRED;
        long secondTransferred = 2 * BYTES_TRANSFERRED;

        File doubleResumeFile = RandomTempFile.createTempFile("test-double", UUID.randomUUID().toString());
        try {
            Files.write(doubleResumeFile.toPath(), RandomStringUtils.randomAlphanumeric((int) secondTransferred)
                                                                    .getBytes(StandardCharsets.UTF_8));

            GetObjectRequest originalGetRequest = GetObjectRequest.builder()
                                                                  .bucket("test-bucket")
                                                                  .key("test-key")
                                                                  .range(ORIGINAL_RANGE)
                                                                  .build();

            // The original request is preserved in the token (not the resumed one)
            DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                         .getObjectRequest(originalGetRequest)
                                                                         .destination(doubleResumeFile)
                                                                         .build();

            HeadObjectResponse headObjectResponse = HeadObjectResponse.builder()
                                                                       .contentLength(WHOLE_OBJECT_SIZE)
                                                                       .lastModified(s3ObjectLastModified)
                                                                       .build();

            Instant fileLastModified = Instant.ofEpochMilli(doubleResumeFile.lastModified());
            ResumableFileDownload secondToken = ResumableFileDownload.builder()
                                                                     .bytesTransferred(secondTransferred)
                                                                     .s3ObjectLastModified(s3ObjectLastModified)
                                                                     .fileLastModified(fileLastModified)
                                                                     .downloadFileRequest(downloadFileRequest)
                                                                     .totalSizeInBytes(WHOLE_OBJECT_SIZE)
                                                                     .build();

            Pair<DownloadFileRequest, AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> result =
                toDownloadFileRequestAndTransformer(secondToken, headObjectResponse, downloadFileRequest);

            GetObjectRequest resumedRequest = result.left().getObjectRequest();

            String expectedRange = "bytes=" + (ORIGINAL_RANGE_START + secondTransferred) + "-" + ORIGINAL_RANGE_END;
            assertThat(resumedRequest.range()).isEqualTo(expectedRange);
        } finally {
            doubleResumeFile.delete();
        }
    }
}
