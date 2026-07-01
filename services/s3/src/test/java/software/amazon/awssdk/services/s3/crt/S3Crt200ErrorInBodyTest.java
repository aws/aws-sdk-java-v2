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

package software.amazon.awssdk.services.s3.crt;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.crt.Log;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Tests that the CRT S3 client correctly detects and reports errors when S3 returns HTTP 200 with an error
 * in the response body. This is a documented S3 API behavior for operations like CompleteMultipartUpload
 * and CopyObject.
 *
 * @see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CompleteMultipartUpload.html">
 *     CompleteMultipartUpload — note on 200 responses with error body</a>
 */
@WireMockTest
@Timeout(30)
public class S3Crt200ErrorInBodyTest {

    private static final String BUCKET = "test-bucket";
    private static final String KEY = "test-key";
    private static final String UPLOAD_ID = "test-upload-id";
    private static final String ETAG = "\"d8e8fca2dc0f896fd7cb4cb0031ba249\"";

    private static final String ERROR_CODE = "ServiceUnavailable";
    private static final String ERROR_MESSAGE = "Service is temporarily unavailable. Please retry the request.";
    private static final String REQUEST_ID = "test-request-id-001";

    private static final String ERROR_BODY =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<Error>"
        + "<Code>" + ERROR_CODE + "</Code>"
        + "<Message>" + ERROR_MESSAGE + "</Message>"
        + "<RequestId>" + REQUEST_ID + "</RequestId>"
        + "</Error>";

    // 5 MB minimum part size for CRT multipart
    private static final long PART_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int FILE_SIZE_BYTES = 11 * 1024 * 1024;

    private S3AsyncClient crtClient;

    @TempDir
    Path tempDir;

    @BeforeAll
    public static void setUpBeforeAll() {
        System.setProperty("aws.crt.debugnative", "true");
        Log.initLoggingToStdout(Log.LogLevel.Warn);
    }

    @BeforeEach
    public void setup(WireMockRuntimeInfo wiremock) {
        crtClient = S3AsyncClient.crtBuilder()
                                 .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()))
                                 .forcePathStyle(true)
                                 .region(Region.US_EAST_1)
                                 .credentialsProvider(StaticCredentialsProvider.create(
                                     AwsBasicCredentials.create("test-key", "test-secret")))
                                 .minimumPartSizeInBytes(PART_SIZE_BYTES)
                                 .thresholdInBytes(PART_SIZE_BYTES)
                                 .retryConfiguration(S3CrtRetryConfiguration.builder().numRetries(0).build())
                                 .build();
    }

    @AfterEach
    public void tearDown() {
        if (crtClient != null) {
            crtClient.close();
        }
    }

    @Test
    @DisplayName("CRT putObject (multipart) should fail with S3Exception when CompleteMultipartUpload returns 200+error")
    void putObject_completeMultipartReturns200WithError_shouldFailWithS3Exception() throws IOException {
        stubCreateMultipartUpload();
        stubUploadParts();
        stubCompleteMultipartUploadWith200PlusErrorBody();
        stubAbortMultipartUpload();

        Path testFile = createTestFile();

        assertThatThrownBy(() ->
            crtClient.putObject(
                PutObjectRequest.builder().bucket(BUCKET).key(KEY).build(),
                testFile
            ).join()
        ).isInstanceOf(CompletionException.class)
         .hasCauseInstanceOf(S3Exception.class)
         .satisfies(thrown -> {
             S3Exception s3e = (S3Exception) thrown.getCause();
             assertThat(s3e.statusCode()).isEqualTo(200);
             assertThat(s3e.awsErrorDetails().errorCode()).isEqualTo(ERROR_CODE);
             assertThat(s3e.awsErrorDetails().errorMessage()).isEqualTo(ERROR_MESSAGE);
             assertThat(s3e.requestId()).isEqualTo(REQUEST_ID);
         });
    }

    @Test
    @DisplayName("CRT copyObject should fail with S3Exception when CopyObject returns 200+error")
    void copyObject_returns200WithError_shouldFailWithS3Exception() {
        // For CRT copy, stub HEAD (source object check) to succeed, then the actual copy to return 200+error.
        // CRT may issue a HEAD on the source object to determine size for single-part vs multi-part copy.
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.head(
            urlPathEqualTo("/" + BUCKET + "/source-key"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Length", "1024")
                .withHeader("ETag", ETAG)));

        // The actual copy request (PUT with x-amz-copy-source)
        stubFor(put(urlPathEqualTo("/" + BUCKET + "/" + KEY))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/xml")
                .withBody(ERROR_BODY)));

        assertThatThrownBy(() ->
            crtClient.copyObject(r -> r
                .sourceBucket(BUCKET)
                .sourceKey("source-key")
                .destinationBucket(BUCKET)
                .destinationKey(KEY)
            ).join()
        ).isInstanceOf(CompletionException.class)
         .hasCauseInstanceOf(S3Exception.class)
         .satisfies(thrown -> {
             S3Exception s3e = (S3Exception) thrown.getCause();
             assertThat(s3e.statusCode()).isEqualTo(200);
             assertThat(s3e.awsErrorDetails().errorCode()).isEqualTo(ERROR_CODE);
             assertThat(s3e.awsErrorDetails().errorMessage()).isEqualTo(ERROR_MESSAGE);
         });
    }

    private void stubCreateMultipartUpload() {
        stubFor(post(urlPathEqualTo("/" + BUCKET + "/" + KEY))
            .withQueryParam("uploads", equalTo(""))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/xml")
                .withBody(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<InitiateMultipartUploadResult>"
                    + "<Bucket>" + BUCKET + "</Bucket>"
                    + "<Key>" + KEY + "</Key>"
                    + "<UploadId>" + UPLOAD_ID + "</UploadId>"
                    + "</InitiateMultipartUploadResult>"
                )));
    }

    private void stubUploadParts() {
        stubFor(put(urlPathEqualTo("/" + BUCKET + "/" + KEY))
            .withQueryParam("partNumber", matching("[0-9]+"))
            .withQueryParam("uploadId", equalTo(UPLOAD_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("ETag", ETAG)));
    }

    private void stubCompleteMultipartUploadWith200PlusErrorBody() {
        stubFor(post(urlPathEqualTo("/" + BUCKET + "/" + KEY))
            .withQueryParam("uploadId", equalTo(UPLOAD_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/xml")
                .withBody(ERROR_BODY)));
    }

    private void stubAbortMultipartUpload() {
        stubFor(delete(urlPathEqualTo("/" + BUCKET + "/" + KEY))
            .withQueryParam("uploadId", equalTo(UPLOAD_ID))
            .willReturn(aResponse().withStatus(204)));
    }

    private Path createTestFile() throws IOException {
        Path file = tempDir.resolve("upload-test.dat");
        byte[] data = new byte[FILE_SIZE_BYTES];
        new Random(42).nextBytes(data);
        Files.write(file, data);
        return file;
    }
}
