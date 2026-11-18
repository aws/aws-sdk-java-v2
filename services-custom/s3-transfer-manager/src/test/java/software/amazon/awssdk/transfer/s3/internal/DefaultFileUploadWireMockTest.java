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
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

/**
 * WireMock test for verifying the DefaultFileUpload codepath.
 */
public class DefaultFileUploadWireMockTest {
    private static final WireMockServer wireMock = new WireMockServer(wireMockConfig().dynamicPort());
    private static Path testFile;
    private static S3AsyncClient s3;

    @BeforeAll
    public static void setup() throws IOException {
        testFile = Files.createTempFile("32mib", ".dat");
        writeTestFile(testFile, 32 * 1024 * 1024);
        wireMock.start();
    }

    @AfterAll
    public static void teardown() throws IOException {
        Files.deleteIfExists(testFile);
        wireMock.stop();
    }

    @BeforeEach
    public void methodSetup() {
        s3 = S3AsyncClient.builder()
                          .credentialsProvider(StaticCredentialsProvider.create(
                              AwsBasicCredentials.create("akid", "skid")))
                          .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                          .region(Region.US_EAST_1)
                          .forcePathStyle(true)
                          .multipartEnabled(true)
                          .multipartConfiguration(c -> c.thresholdInBytes(16 * 1024 * 1024L))
                          .overrideConfiguration(o -> o.retryPolicy(RetryPolicy.defaultRetryPolicy().toBuilder()
                                                                               .numRetries(3)
                                                                               .build()))
                          .build();
    }

    @AfterEach
    public void methodTeardown() {
        s3.close();
    }

    @Test
    void retryableErrorDuringUpload_shouldSupportRetries() {
        S3TransferManager tm = S3TransferManager.builder().s3Client(s3).build();
        stubCreateMpuSuccessfulResponse();

        wireMock.stubFor(put(anyUrl())
                             .willReturn(aResponse()
                                             .withStatus(500)
                                             .withBody("Internal Error")));

        UploadFileRequest request = createUploadFileRequest();

        assertThatThrownBy(() -> tm.uploadFile(request).completionFuture().join())
            .hasCauseInstanceOf(S3Exception.class);

        wireMock.verify(moreThanOrExactly(3),
                        putRequestedFor(urlPathMatching("/bucket/key"))
                            .withQueryParam("uploadId", matching("uploadId"))
                            .withQueryParam("partNumber", matching("1")));
    }

    @Test
    void connectionFaultDuringUpload_shouldSaveStateOfUpload() {
        S3TransferManager tm = S3TransferManager.builder().s3Client(s3).build();

        stubCreateMpuSuccessfulResponse();

        wireMock.stubFor(put(urlEqualTo("/bucket/key?partNumber=1&uploadId=uploadId"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody("<Part><PartNumber>1</PartNumber><ETag>\"etag1\"</ETag></Part>")));

        wireMock.stubFor(put(urlEqualTo("/bucket/key?partNumber=2&uploadId=uploadId"))
                             .willReturn(aResponse()
                                             .withFault(Fault.CONNECTION_RESET_BY_PEER)));

        FileUpload fileUpload = tm.uploadFile(createUploadFileRequest());

        assertThatThrownBy(() -> fileUpload.completionFuture().join()).isInstanceOf(CompletionException.class);
        ResumableFileUpload resumableFileUpload = fileUpload.pause();
        assertThat(resumableFileUpload.multipartUploadId()).hasValue("uploadId");
    }


    @Test
    void pause_whilePartsInFlight_shouldReturnTokenAndCancelCompletionFuture() throws Exception {
        wireMock.resetAll();
        S3TransferManager tm = S3TransferManager.builder().s3Client(s3).build();
        stubCreateMpuSuccessfulResponse();
        stubUploadPartSuccessfulResponse(10_000);
        AtomicReference<Throwable> reportedFailure = new AtomicReference<>();
        TransferListener listener = new TransferListener() {
            @Override
            public void transferFailed(Context.TransferFailed context) {
                reportedFailure.set(context.exception());
            }
        };
        UploadFileRequest request = createUploadFileRequest().toBuilder().addTransferListener(listener).build();

        FileUpload fileUpload = tm.uploadFile(request);
        awaitRequest(putRequestedFor(urlPathEqualTo("/bucket/key")).withQueryParam("uploadId", matching("uploadId")));

        ResumableFileUpload resumableFileUpload = fileUpload.pause();

        assertThat(resumableFileUpload.multipartUploadId()).hasValue("uploadId");
        assertThatThrownBy(() -> fileUpload.completionFuture().get(5, TimeUnit.SECONDS))
            .isInstanceOf(CancellationException.class);
        assertThat(reportedFailure.get()).isInstanceOf(CancellationException.class);
    }

    @Test
    void pause_whileCompleteMultipartUploadInFlight_shouldLetUploadFinish() throws Exception {
        wireMock.resetAll();
        S3TransferManager tm = S3TransferManager.builder().s3Client(s3).build();
        stubCreateMpuSuccessfulResponse();
        stubUploadPartSuccessfulResponse(0);
        stubCompleteMpuSuccessfulResponse(5_000);
        stubAbortMpuSuccessfulResponse();

        FileUpload fileUpload = tm.uploadFile(createUploadFileRequest());
        awaitRequest(postRequestedFor(urlPathEqualTo("/bucket/key")).withQueryParam("uploadId", matching("uploadId")));
        assertThat(fileUpload.completionFuture()).isNotDone();

        ResumableFileUpload resumableFileUpload = fileUpload.pause();

        assertThat(resumableFileUpload.multipartUploadId()).isEmpty();
        CompletedFileUpload completedFileUpload = fileUpload.completionFuture().get(10, TimeUnit.SECONDS);
        assertThat(completedFileUpload.response().eTag()).isEqualTo("\"etag\"");
        wireMock.verify(0, deleteRequestedFor(anyUrl()));
    }

    @Test
    void cancel_whileCompleteMultipartUploadInFlight_shouldAbortUpload() throws Exception {
        wireMock.resetAll();
        S3TransferManager tm = S3TransferManager.builder().s3Client(s3).build();
        stubCreateMpuSuccessfulResponse();
        stubUploadPartSuccessfulResponse(0);
        stubCompleteMpuSuccessfulResponse(5_000);
        stubAbortMpuSuccessfulResponse();

        FileUpload fileUpload = tm.uploadFile(createUploadFileRequest());
        awaitRequest(postRequestedFor(urlPathEqualTo("/bucket/key")).withQueryParam("uploadId", matching("uploadId")));

        fileUpload.completionFuture().cancel(true);

        awaitRequest(deleteRequestedFor(urlPathEqualTo("/bucket/key")).withQueryParam("uploadId", matching("uploadId")));
    }

    private static void awaitRequest(RequestPatternBuilder pattern) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (wireMock.findAll(pattern).isEmpty()) {
            assertThat(System.nanoTime()).withFailMessage("Timed out waiting for request %s", pattern.build())
                                         .isLessThan(deadline);
            Thread.sleep(10);
        }
    }

    private void stubUploadPartSuccessfulResponse(int delayMillis) {
        wireMock.stubFor(put(urlPathEqualTo("/bucket/key"))
                             .withQueryParam("uploadId", matching("uploadId"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withHeader("ETag", "\"part-etag\"")
                                             .withFixedDelay(delayMillis)));
    }

    private void stubCompleteMpuSuccessfulResponse(int delayMillis) {
        String completeMpuBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<CompleteMultipartUploadResult>\n"
                                 + "   <Bucket>bucket</Bucket>\n"
                                 + "   <Key>key</Key>\n"
                                 + "   <ETag>\"etag\"</ETag>\n"
                                 + "</CompleteMultipartUploadResult>";

        wireMock.stubFor(post(urlPathEqualTo("/bucket/key"))
                             .withQueryParam("uploadId", matching("uploadId"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody(completeMpuBody)
                                             .withFixedDelay(delayMillis)));
    }

    private void stubAbortMpuSuccessfulResponse() {
        wireMock.stubFor(delete(urlPathEqualTo("/bucket/key"))
                             .withQueryParam("uploadId", matching("uploadId"))
                             .willReturn(aResponse().withStatus(204)));
    }

    private void stubCreateMpuSuccessfulResponse() {
        String mpuInitBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                             + "<InitiateMultipartUploadResult>\n"
                             + "   <Bucket>bucket</Bucket>\n"
                             + "   <Key>key</Key>\n"
                             + "   <UploadId>uploadId</UploadId>\n"
                             + "</InitiateMultipartUploadResult>";

        wireMock.stubFor(post(urlEqualTo("/bucket/key?uploads"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody(mpuInitBody)));
    }

    private UploadFileRequest createUploadFileRequest() {
        return UploadFileRequest.builder()
                                .source(testFile)
                                .putObjectRequest(put -> put.bucket("bucket").key("key"))
                                .build();
    }

    private static void writeTestFile(Path file, long size) {
        try (OutputStream os = Files.newOutputStream(file, StandardOpenOption.CREATE)) {
            byte[] buff = new byte[4096];
            long remaining = size;
            while (remaining != 0) {
                int writeLen = (int) Math.min(remaining, buff.length);
                os.write(buff, 0, writeLen);
                remaining -= writeLen;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
