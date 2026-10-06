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
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;

/**
 * Verifies that the Java-based transfer manager correctly replaces the destination file when
 * resuming a part-scoped download.
 */
@WireMockTest
class GenericS3TransferManagerDownloadResumePartNumberWireMockTest {
    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final int PART_NUMBER = 3;
    private static final String E_TAG = "\"etag123\"";
    private static final Instant LAST_MODIFIED = Instant.parse("2026-09-30T12:00:00Z");

    private byte[] fullPartContent;
    private Path destination;
    private S3AsyncClient s3AsyncClient;
    private S3TransferManager tm;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wm) throws IOException {
        fullPartContent = RandomStringUtils.randomAlphanumeric(8000).getBytes(StandardCharsets.UTF_8);
        destination = RandomTempFile.randomUncreatedFile().toPath();

        s3AsyncClient = S3AsyncClient.builder()
                                     .region(Region.US_EAST_1)
                                     .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                     .forcePathStyle(true)
                                     .credentialsProvider(StaticCredentialsProvider.create(
                                         AwsBasicCredentials.create("key", "secret")))
                                     .build();
        tm = new GenericS3TransferManager(s3AsyncClient,
                                          mock(UploadDirectoryHelper.class),
                                          mock(TransferManagerConfiguration.class),
                                          mock(DownloadDirectoryHelper.class));
    }

    @AfterEach
    void tearDown() {
        tm.close();
        s3AsyncClient.close();
        destination.toFile().delete();
    }

    /**
     * Simulates a part-scoped download that was paused after 3000 bytes, then resumed.
     * The resumed download should replace the file with the full 8000-byte part content.
     */
    @Test
    void resumeDownloadFile_partNumberSet_shouldReplaceFileNotAppend() throws Exception {
        // Simulate a partial download, 3000 bytes already on disk
        byte[] partialContent = new byte[3000];
        System.arraycopy(fullPartContent, 0, partialContent, 0, 3000);
        Files.write(destination, partialContent);

        Instant fileLastModified = Instant.ofEpochMilli(destination.toFile().lastModified());

        stubHeadObject();
        stubGetObjectWithFullPart();

        // Build a ResumableFileDownload representing the paused state
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .bucket(BUCKET)
                                                            .key(KEY)
                                                            .partNumber(PART_NUMBER)
                                                            .build();
        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                     .getObjectRequest(getObjectRequest)
                                                                     .destination(destination)
                                                                     .build();
        ResumableFileDownload resumableFileDownload = ResumableFileDownload.builder()
                                                                           .bytesTransferred(3000L)
                                                                           .s3ObjectLastModified(LAST_MODIFIED)
                                                                           .fileLastModified(fileLastModified)
                                                                           .downloadFileRequest(downloadFileRequest)
                                                                           .s3ObjectEtag(E_TAG)
                                                                           .totalSizeInBytes((long) fullPartContent.length)
                                                                           .build();

        // Resume the download
        CompletedFileDownload completed = tm.resumeDownloadFile(resumableFileDownload)
                                            .completionFuture()
                                            .join();

        assertThat(destination.toFile().length())
            .as("File should contain exactly the full part (%d bytes), not partial + full (%d bytes)",
                fullPartContent.length, 3000 + fullPartContent.length)
            .isEqualTo(fullPartContent.length);
        assertThat(destination).hasBinaryContent(fullPartContent);

        verify(getRequestedFor(urlPathEqualTo("/" + BUCKET + "/" + KEY))
                   .withQueryParam("partNumber", equalTo(String.valueOf(PART_NUMBER)))
                   .withoutHeader("Range"));
    }

    /**
     * When partNumber is NOT set, a normal resume should still append (range-based resume).
     * This ensures we didn't break the standard resume path.
     */
    @Test
    void resumeDownloadFile_noPartNumber_shouldAppendRemainingBytes() throws Exception {
        byte[] partialContent = new byte[3000];
        System.arraycopy(fullPartContent, 0, partialContent, 0, 3000);
        Files.write(destination, partialContent);

        Instant fileLastModified = Instant.ofEpochMilli(destination.toFile().lastModified());

        byte[] remainingContent = new byte[fullPartContent.length - 3000];
        System.arraycopy(fullPartContent, 3000, remainingContent, 0, remainingContent.length);

        stubHeadObject();
        stubGetObjectWithRangedResponse(remainingContent);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .bucket(BUCKET)
                                                            .key(KEY)
                                                            .build();
        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                     .getObjectRequest(getObjectRequest)
                                                                     .destination(destination)
                                                                     .build();
        ResumableFileDownload resumableFileDownload = ResumableFileDownload.builder()
                                                                           .bytesTransferred(3000L)
                                                                           .s3ObjectLastModified(LAST_MODIFIED)
                                                                           .fileLastModified(fileLastModified)
                                                                           .downloadFileRequest(downloadFileRequest)
                                                                           .s3ObjectEtag(E_TAG)
                                                                           .totalSizeInBytes((long) fullPartContent.length)
                                                                           .build();

        tm.resumeDownloadFile(resumableFileDownload)
          .completionFuture()
          .join();

        assertThat(destination).hasBinaryContent(fullPartContent);

        verify(getRequestedFor(urlPathEqualTo("/" + BUCKET + "/" + KEY))
                   .withHeader("Range", matching("bytes=.*")));
    }

    private void stubHeadObject() {
        stubFor(head(urlPathEqualTo("/" + BUCKET + "/" + KEY))
                    .willReturn(aResponse().withStatus(200)
                                           .withHeader("Content-Length", String.valueOf(fullPartContent.length))
                                           .withHeader("ETag", E_TAG)
                                           .withHeader("Last-Modified", "Wed, 30 Sep 2026 12:00:00 GMT")));
    }

    private void stubGetObjectWithFullPart() {
        stubFor(get(urlPathEqualTo("/" + BUCKET + "/" + KEY))
                    .withQueryParam("partNumber", equalTo(String.valueOf(PART_NUMBER)))
                    .willReturn(aResponse().withStatus(200)
                                           .withHeader("ETag", E_TAG)
                                           .withHeader("Content-Length", String.valueOf(fullPartContent.length))
                                           .withBody(fullPartContent)));
    }

    private void stubGetObjectWithRangedResponse(byte[] content) {
        stubFor(get(urlPathEqualTo("/" + BUCKET + "/" + KEY))
                    .willReturn(aResponse().withStatus(206)
                                           .withHeader("ETag", E_TAG)
                                           .withHeader("Content-Length", String.valueOf(content.length))
                                           .withBody(content)));
    }
}
