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
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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

    static Stream<Arguments> resumeRangeCases() {
        long objectSize = 8 * 1024 * 1024;
        long transferred = 1024 * 1024;
        long rangeStart = 2 * 1024 * 1024;
        long rangeEnd = 6 * 1024 * 1024 - 1;
        return Stream.of(
            // ranged: bytes=(start+transferred)-end
            Arguments.of("bytes=" + rangeStart + "-" + rangeEnd,
                         "bytes=" + (rangeStart + transferred) + "-" + rangeEnd),
            // zero-offset range: bytes=(0+transferred)-end
            Arguments.of("bytes=0-" + (4 * 1024 * 1024 - 1),
                         "bytes=" + transferred + "-" + (4 * 1024 * 1024 - 1)),
            // open-ended range: bytes=(start+transferred)-(contentLength-1)
            Arguments.of("bytes=" + rangeStart + "-",
                         "bytes=" + (rangeStart + transferred) + "-" + (objectSize - 1)),
            // non-ranged: bytes=transferred-contentLength
            Arguments.of(null,
                         "bytes=" + transferred + "-" + objectSize)
        );
    }

    @ParameterizedTest
    @MethodSource("resumeRangeCases")
    void resumeDownload_shouldComputeCorrectRange(String originalRange, String expectedRange) {
        GetObjectRequest.Builder requestBuilder = GetObjectRequest.builder()
                                                                  .bucket("test-bucket")
                                                                  .key("test-key");
        if (originalRange != null) {
            requestBuilder.range(originalRange);
        }

        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                     .getObjectRequest(requestBuilder.build())
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

        assertThat(result.left().getObjectRequest().range()).isEqualTo(expectedRange);
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

    @ParameterizedTest
    @ValueSource(strings = {"bytes=-500", "bytes=100-500,1000-2000", "bytes=0-0,-1"})
    void resumeDownload_unparseableRange_shouldRestartFromBeginning(String unparseableRange) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .bucket("test-bucket")
                                                            .key("test-key")
                                                            .range(unparseableRange)
                                                            .build();

        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                     .getObjectRequest(getObjectRequest)
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

        // Unparseable ranges should restart from the beginning, preserving the original range
        GetObjectRequest resumedRequest = result.left().getObjectRequest();
        assertThat(resumedRequest.range()).isEqualTo(unparseableRange);
        // ifUnmodifiedSince should be set for the restart request
        assertThat(resumedRequest.ifUnmodifiedSince()).isEqualTo(s3ObjectLastModified);
    }

    @Test
    void resumeNonRangedDownload_doubleResume_shouldNotDoubleCountOffset() throws IOException {
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
