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
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.CaptureTransferListener;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;

/**
 * Exercises the CRT-based transfer manager's download against a real CRT client, so that the response file is genuinely
 * written by CRT rather than by the SDK.
 */
@WireMockTest
public class CrtS3TransferManagerDownloadWireMockTest {
    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final int OBJ_SIZE = 16 * 1024;
    private static final String E_TAG = "\"1234\"";

    private byte[] content;
    private Path destination;
    private S3AsyncClient crtClient;
    private CrtS3TransferManager tm;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wm) throws IOException {
        content = RandomStringUtils.randomAlphanumeric(OBJ_SIZE).getBytes(StandardCharsets.UTF_8);
        destination = RandomTempFile.randomUncreatedFile().toPath();
        crtClient = S3AsyncClient.crtBuilder()
                                 .region(Region.US_EAST_1)
                                 .endpointOverride(URI.create("http://localhost:" + wm.getHttpPort()))
                                 .forcePathStyle(true)
                                 .credentialsProvider(StaticCredentialsProvider.create(
                                     AwsBasicCredentials.create("key", "secret")))
                                 .build();
        tm = new CrtS3TransferManager(TransferManagerConfiguration.builder().build(), crtClient, false);
    }

    @AfterEach
    void tearDown() {
        tm.close();
        crtClient.close();
        destination.toFile().delete();
    }

    @Test
    void downloadFile_crtWritesResponseBodyToDestinationFile() throws Exception {
        stubSuccessfulGetObject();
        CaptureTransferListener listener = new CaptureTransferListener();

        FileDownload download = tm.downloadFile(r -> r.getObjectRequest(g -> g.bucket(BUCKET).key(KEY))
                                                      .destination(destination)
                                                      .addTransferListener(listener));
        CompletedFileDownload completed = download.completionFuture().join();

        assertThat(destination).hasBinaryContent(content);
        assertThat(completed.response().eTag()).isEqualTo(E_TAG);

        listener.getCompletionFuture().get(10, TimeUnit.SECONDS);
        assertThat(listener.isTransferInitiated()).isTrue();
        assertThat(listener.isTransferComplete()).isTrue();
    }

    @Test
    void downloadFile_crtWritesResponseBodyToDestinationFile_progressIsNotResetWhenTransferCompletes() throws Exception {
        // CRT hands the (empty) body publisher to the SDK only once the transfer has finished, so a response transformer
        // wrapper that reset the byte count on subscribe would wipe out all the progress CRT reported.
        stubSuccessfulGetObject();
        CaptureTransferListener listener = new CaptureTransferListener();

        FileDownload download = tm.downloadFile(r -> r.getObjectRequest(g -> g.bucket(BUCKET).key(KEY))
                                                      .destination(destination)
                                                      .addTransferListener(listener));
        download.completionFuture().join();
        listener.getCompletionFuture().get(10, TimeUnit.SECONDS);

        TransferProgressSnapshot snapshot = download.progress().snapshot();
        assertThat(snapshot.transferredBytes()).isEqualTo(OBJ_SIZE);
        assertThat(snapshot.totalBytes()).hasValue((long) OBJ_SIZE);
        assertThat(snapshot.ratioTransferred()).hasValue(1.0);
        assertThat(listener.getRatioTransferredList()).contains(1.0);
    }

    @Test
    void downloadFile_serverError_completesExceptionallyAndReportsFailure() throws Exception {
        stubFor(head(anyUrl()).willReturn(aResponse().withStatus(404)));
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(404)
                                                   .withBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                                             + "<Error><Code>NoSuchKey</Code>"
                                                             + "<Message>does not exist</Message></Error>")));
        CaptureTransferListener listener = new CaptureTransferListener();

        FileDownload download = tm.downloadFile(r -> r.getObjectRequest(g -> g.bucket(BUCKET).key(KEY))
                                                      .destination(destination)
                                                      .addTransferListener(listener));

        assertThatExceptionOfType(CompletionException.class)
            .isThrownBy(() -> download.completionFuture().join())
            .withCauseInstanceOf(S3Exception.class);
        assertThat(listener.isTransferComplete()).isFalse();
    }

    private void stubSuccessfulGetObject() {
        stubFor(head(anyUrl()).willReturn(aResponse().withStatus(200)
                                                     .withHeader("Content-Length", String.valueOf(OBJ_SIZE))
                                                     .withHeader("ETag", E_TAG)));
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("ETag", E_TAG)
                                                    .withBody(content)));
    }
}
