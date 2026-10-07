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
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
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
 * WireMock-based test that validates file content integrity after resuming a ranged download.
 * Verifies that the correct bytes end up on disk after the full resume flow.
 */
@WireMockTest
class ResumeRangedDownloadWireMockTest {

    private static final String BUCKET = "test-bucket";
    private static final String KEY = "test-key";
    private static final String URL_PATH = "/" + BUCKET + "/" + KEY;
    private static final String E_TAG = "\"abcdef1234567890\"";
    private static final Instant LAST_MODIFIED = Instant.parse("2026-09-28T12:00:00Z");
    private static final String LAST_MODIFIED_HTTP = "Mon, 28 Sep 2026 12:00:00 GMT";

    private static final int OBJECT_SIZE = 8 * 1024;
    private static final byte[] FULL_OBJECT_CONTENT = buildDeterministicContent(OBJECT_SIZE);
    private static final int ORIGINAL_RANGE_START = 2048;
    private static final int ORIGINAL_RANGE_END = 6143;
    private static final String ORIGINAL_RANGE = "bytes=" + ORIGINAL_RANGE_START + "-" + ORIGINAL_RANGE_END;
    private static final int RANGE_LENGTH = ORIGINAL_RANGE_END - ORIGINAL_RANGE_START + 1;
    private static final int BYTES_TRANSFERRED = 1024;
    private static final int EXPECTED_RESUME_START = ORIGINAL_RANGE_START + BYTES_TRANSFERRED;
    private static final String EXPECTED_RESUME_RANGE = "bytes=" + EXPECTED_RESUME_START + "-" + ORIGINAL_RANGE_END;

    private S3AsyncClient s3Client;
    private S3TransferManager tm;
    private Path destination;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wm) throws IOException {
        destination = RandomTempFile.randomUncreatedFile().toPath();

        byte[] alreadyDownloaded = Arrays.copyOfRange(FULL_OBJECT_CONTENT, ORIGINAL_RANGE_START,
                                                      ORIGINAL_RANGE_START + BYTES_TRANSFERRED);
        Files.write(destination, alreadyDownloaded);

        s3Client = S3AsyncClient.builder()
                                .region(Region.US_EAST_1)
                                .endpointOverride(URI.create("http://localhost:" + wm.getHttpPort()))
                                .forcePathStyle(true)
                                .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create("key", "secret")))
                                .build();

        tm = S3TransferManager.builder()
                              .s3Client(s3Client)
                              .build();
    }

    @AfterEach
    void tearDown() {
        tm.close();
        s3Client.close();
        try {
            Files.deleteIfExists(destination);
        } catch (IOException ignored) {
        }
    }

    @Test
    void resumeRangedDownload_fileContentMatchesExpectedRange() {
        byte[] remainingContent = Arrays.copyOfRange(FULL_OBJECT_CONTENT, EXPECTED_RESUME_START,
                                                     ORIGINAL_RANGE_END + 1);

        stubHeadObject();
        stubGetObjectForRange(EXPECTED_RESUME_RANGE, remainingContent);

        ResumableFileDownload resumableFileDownload = buildResumableToken();

        CompletedFileDownload completed = tm.resumeDownloadFile(resumableFileDownload)
                                            .completionFuture()
                                            .join();

        assertThat(completed).isNotNull();

        byte[] expectedFileContent = Arrays.copyOfRange(FULL_OBJECT_CONTENT, ORIGINAL_RANGE_START,
                                                        ORIGINAL_RANGE_END + 1);

        assertThat(destination.toFile().length()).isEqualTo(RANGE_LENGTH);

        assertThat(readFileBytes()).isEqualTo(expectedFileContent);
    }

    @Test
    void resumeNonRangedDownload_fileContentMatchesExpectedBytes() throws IOException {
        int transferred = 2048;
        byte[] alreadyDownloaded = Arrays.copyOfRange(FULL_OBJECT_CONTENT, 0, transferred);
        Files.write(destination, alreadyDownloaded);

        byte[] remainingContent = Arrays.copyOfRange(FULL_OBJECT_CONTENT, transferred, OBJECT_SIZE);
        String expectedRange = "bytes=" + transferred + "-" + OBJECT_SIZE;

        stubHeadObject();
        stubGetObjectForRange(expectedRange, remainingContent);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .bucket(BUCKET)
                                                            .key(KEY)
                                                            .build();

        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                     .getObjectRequest(getObjectRequest)
                                                                     .destination(destination)
                                                                     .build();

        ResumableFileDownload resumable = ResumableFileDownload.builder()
                                                               .downloadFileRequest(downloadFileRequest)
                                                               .bytesTransferred((long) transferred)
                                                               .s3ObjectLastModified(LAST_MODIFIED)
                                                               .fileLastModified(Instant.ofEpochMilli(
                                                                   destination.toFile().lastModified()))
                                                               .totalSizeInBytes((long) OBJECT_SIZE)
                                                               .build();

        CompletedFileDownload completed = tm.resumeDownloadFile(resumable)
                                            .completionFuture()
                                            .join();

        assertThat(completed).isNotNull();
        assertThat(destination.toFile().length()).isEqualTo(OBJECT_SIZE);
        assertThat(readFileBytes()).isEqualTo(FULL_OBJECT_CONTENT);
    }

    private ResumableFileDownload buildResumableToken() {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .bucket(BUCKET)
                                                            .key(KEY)
                                                            .range(ORIGINAL_RANGE)
                                                            .build();

        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                     .getObjectRequest(getObjectRequest)
                                                                     .destination(destination)
                                                                     .build();

        return ResumableFileDownload.builder()
                                    .downloadFileRequest(downloadFileRequest)
                                    .bytesTransferred((long) BYTES_TRANSFERRED)
                                    .s3ObjectLastModified(LAST_MODIFIED)
                                    .fileLastModified(Instant.ofEpochMilli(destination.toFile().lastModified()))
                                    .totalSizeInBytes((long) OBJECT_SIZE)
                                    .build();
    }

    private void stubHeadObject() {
        stubFor(head(urlPathEqualTo(URL_PATH))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Length", String.valueOf(OBJECT_SIZE))
                                    .withHeader("Last-Modified", LAST_MODIFIED_HTTP)
                                    .withHeader("ETag", E_TAG)));
    }

    private void stubGetObjectForRange(String expectedRange, byte[] responseBody) {
        stubFor(get(urlPathEqualTo(URL_PATH))
                    .withHeader("Range", WireMock.containing(expectedRange.replace("bytes=", "")))
                    .willReturn(aResponse()
                                    .withStatus(206)
                                    .withHeader("Content-Length", String.valueOf(responseBody.length))
                                    .withHeader("Content-Range", "bytes " + expectedRange.replace("bytes=", "")
                                                                 + "/" + OBJECT_SIZE)
                                    .withHeader("Last-Modified", LAST_MODIFIED_HTTP)
                                    .withHeader("ETag", E_TAG)
                                    .withBody(responseBody)));
    }

    private byte[] readFileBytes() {
        try {
            return Files.readAllBytes(destination);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read destination file", e);
        }
    }

    private static byte[] buildDeterministicContent(int size) {
        byte[] content = new byte[size];
        for (int i = 0; i < size; i++) {
            content[i] = (byte) (i % 251);
        }
        return content;
    }
}
