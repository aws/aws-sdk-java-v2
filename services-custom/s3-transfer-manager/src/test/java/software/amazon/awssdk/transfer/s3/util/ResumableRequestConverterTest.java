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
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.testutils.LogCaptor;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.utils.Pair;

class ResumableRequestConverterTest {
    private static final String ETAG_FOO = "acbd18db4cc2f85cedef654fccc4a4d8";
    private static final String ETAG_BAR = "37b51d194a7513e45b56f6524f2d51f2";
    private static final String ETAG_BAR_UPPERCASE = "ddc35f88fa71b6ef142ae61f35364653";

    private File file;

    @BeforeEach
    public void methodSetup() throws IOException {
        file = RandomTempFile.createTempFile("test", UUID.randomUUID().toString());
        Files.write(file.toPath(), RandomStringUtils.randomAlphanumeric(1000).getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    public void methodTeardown() {
        file.delete();
    }

    @Test
    void toDownloadFileAndTransformer_notModified_shouldSetRangeAccordingly() {
        Instant s3ObjectLastModified = Instant.now();
        GetObjectRequest getObjectRequest = getObjectRequest();
        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                     .getObjectRequest(getObjectRequest)
                                                                     .destination(file)
                                                                     .build();
        Instant fileLastModified = Instant.ofEpochMilli(file.lastModified());
        ResumableFileDownload resumableFileDownload = ResumableFileDownload.builder()
                                                                           .bytesTransferred(file.length())
                                                                           .s3ObjectLastModified(s3ObjectLastModified)
                                                                           .fileLastModified(fileLastModified)
                                                                           .downloadFileRequest(downloadFileRequest)
                                                                           .build();
        Pair<DownloadFileRequest, AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> actual =
            toDownloadFileRequestAndTransformer(resumableFileDownload, headObjectResponse(s3ObjectLastModified),
                                                downloadFileRequest);
        verifyActualGetObjectRequest(getObjectRequest, actual.left().getObjectRequest(), "bytes=1000-2000");
    }

    @Test
    void toDownloadFileAndTransformer_s3ObjectModified_shouldStartFromBeginning() {
        Instant s3ObjectLastModified = Instant.now().minusSeconds(5);
        GetObjectRequest getObjectRequest = getObjectRequest();
        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                     .getObjectRequest(getObjectRequest)
                                                                     .destination(file)
                                                                     .build();
        Instant fileLastModified = Instant.ofEpochMilli(file.lastModified());
        ResumableFileDownload resumableFileDownload = ResumableFileDownload.builder()
                                                                           .bytesTransferred(1000L)
                                                                           .s3ObjectLastModified(s3ObjectLastModified)
                                                                           .fileLastModified(fileLastModified)
                                                                           .downloadFileRequest(downloadFileRequest)
                                                                           .build();
        Pair<DownloadFileRequest, AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> actual =
            toDownloadFileRequestAndTransformer(resumableFileDownload, headObjectResponse(Instant.now()),
                                                downloadFileRequest);
        verifyActualGetObjectRequest(getObjectRequest, actual.left().getObjectRequest(), null);
    }

    @Test
    void toDownloadFileAndTransformer_fileLastModifiedTimeChanged_shouldStartFromBeginning() throws IOException {
        Instant s3ObjectLastModified = Instant.now();
        GetObjectRequest getObjectRequest = getObjectRequest();
        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                     .getObjectRequest(getObjectRequest)
                                                                     .destination(file)
                                                                     .build();
        Instant fileLastModified = Instant.now().minusSeconds(10);
        ResumableFileDownload resumableFileDownload = ResumableFileDownload.builder()
                                                                           .bytesTransferred(1000L)
                                                                           .s3ObjectLastModified(s3ObjectLastModified)
                                                                           .fileLastModified(fileLastModified)
                                                                           .downloadFileRequest(downloadFileRequest)
                                                                           .build();
        Pair<DownloadFileRequest, AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> actual =
            toDownloadFileRequestAndTransformer(resumableFileDownload, headObjectResponse(s3ObjectLastModified),
                                                downloadFileRequest);
        verifyActualGetObjectRequest(getObjectRequest, actual.left().getObjectRequest(), null);
    }

    static Stream<Arguments> EtagTestCases() {
        return Stream.of(
            Arguments.of(ETAG_FOO, null),
            Arguments.of(ETAG_BAR, "bytes=1000-2000"),
            Arguments.of(null, "bytes=1000-2000"),
            Arguments.of(ETAG_BAR_UPPERCASE, null)
        );
    }

    @ParameterizedTest
    @MethodSource("EtagTestCases")
    void toDownloadFileAndTransformer_fileEtagChanged_shouldRestartOrResume(String etag, String range) {
        Instant s3ObjectLastModified = Instant.now();
        GetObjectRequest getObjectRequest = getObjectRequest();
        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                     .getObjectRequest(getObjectRequest)
                                                                     .destination(file)
                                                                     .build();

        ResumableFileDownload resumableFileDownload = ResumableFileDownload.builder()
                                                                           .bytesTransferred(1000L)
                                                                           .s3ObjectLastModified(s3ObjectLastModified)
                                                                           .fileLastModified(Instant.ofEpochMilli(file.lastModified()))
                                                                           .s3ObjectEtag(etag)
                                                                           .downloadFileRequest(downloadFileRequest)
                                                                           .build();
        Pair<DownloadFileRequest, AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> actual =
            toDownloadFileRequestAndTransformer(resumableFileDownload, headObjectResponse(s3ObjectLastModified),
                                                downloadFileRequest);
        verifyActualGetObjectRequest(getObjectRequest, actual.left().getObjectRequest(), range);
    }

    @Test
    void toDownloadFileAndTransformer_fileEtagChanged_shouldLogHint() {
        Instant s3ObjectLastModified = Instant.now();
        GetObjectRequest getObjectRequest = getObjectRequest();
        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                     .getObjectRequest(getObjectRequest)
                                                                     .destination(file)
                                                                     .build();

        ResumableFileDownload resumableFileDownload = ResumableFileDownload.builder()
                                                                           .bytesTransferred(1000L)
                                                                           .s3ObjectLastModified(s3ObjectLastModified)
                                                                           .fileLastModified(Instant.ofEpochMilli(file.lastModified()))
                                                                           .s3ObjectEtag("acbd18db4cc2f85cedef654fccc4a4d8")
                                                                           .downloadFileRequest(downloadFileRequest)
                                                                           .build();
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
                toDownloadFileRequestAndTransformer(resumableFileDownload, headObjectResponse(s3ObjectLastModified),
                                                    downloadFileRequest);
            List<LogEvent> logEvents = logCaptor.loggedEvents();

            assertThat(logEvents.get(0).getMessage().getFormattedMessage())
                .contains("The ETag of the requested object in bucket (bucket) with key (key)"
                          + " has changed since the last pause. The SDK will download the S3 object from the beginning");
        }
    }

    @Test
    void toDownloadFileAndTransformer_fileLengthChanged_shouldStartFromBeginning() {
        Instant s3ObjectLastModified = Instant.now();
        GetObjectRequest getObjectRequest = getObjectRequest();
        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                     .getObjectRequest(getObjectRequest)
                                                                     .destination(file)
                                                                     .build();
        Instant fileLastModified = Instant.now().minusSeconds(10);
        ResumableFileDownload resumableFileDownload = ResumableFileDownload.builder()
                                                                           .bytesTransferred(1100L)
                                                                           .s3ObjectLastModified(s3ObjectLastModified)
                                                                           .fileLastModified(fileLastModified)
                                                                           .downloadFileRequest(downloadFileRequest)
                                                                           .build();
        Pair<DownloadFileRequest, AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> actual =
            toDownloadFileRequestAndTransformer(resumableFileDownload, headObjectResponse(s3ObjectLastModified),
                                                downloadFileRequest);
        verifyActualGetObjectRequest(getObjectRequest, actual.left().getObjectRequest(), null);
    }

    private static void verifyActualGetObjectRequest(GetObjectRequest originalRequest, GetObjectRequest actualRequest,
                                                     String range) {
        assertThat(actualRequest.bucket()).isEqualTo(originalRequest.bucket());
        assertThat(actualRequest.key()).isEqualTo(originalRequest.key());
        assertThat(actualRequest.range()).isEqualTo(range);
    }

    private static HeadObjectResponse headObjectResponse(Instant s3ObjectLastModified) {
        return HeadObjectResponse
            .builder()
            .contentLength(2000L)
            .lastModified(s3ObjectLastModified)
            .eTag(ETAG_BAR)
            .build();
    }

    private static GetObjectRequest getObjectRequest() {
        return GetObjectRequest.builder()
                               .key("key")
                               .bucket("bucket")
                               .build();
    }
}
