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
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.CaptureTransferListener;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

@WireMockTest
public class S3JavaMultipartTransferProgressListenerTest {

    public static final String ERROR_CODE = "NoSuchBucket";
    public static final String ERROR_MESSAGE = "We encountered an internal error. Please try again.";
    public static final String ERROR_BODY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                            + "<Error>\n"
                                            + "  <Code>" + ERROR_CODE + "</Code>\n"
                                            + "  <Message>" + ERROR_MESSAGE + "</Message>\n"
                                            + "</Error>";
    private static final String EXAMPLE_BUCKET = "Example-Bucket";
    private static final String TEST_KEY = "16mib_file.dat";
    private static final int OBJ_SIZE = 16 * 1024 * 1024;
    private static RandomTempFile testFile;
    private static URI testEndpoint;

    @BeforeAll
    public static void init(WireMockRuntimeInfo wm) throws IOException {
        testEndpoint = URI.create(wm.getHttpBaseUrl());
        testFile = new RandomTempFile(TEST_KEY, OBJ_SIZE);
    }

    @BeforeEach
    void resetWireMock() {
        WireMock.reset();
    }

    private static S3AsyncClient s3AsyncClient(boolean multipartEnabled) {
        return S3AsyncClient.builder()
                            .multipartEnabled(multipartEnabled)
                            .region(Region.US_EAST_1)
                            .endpointOverride(testEndpoint)
                            .credentialsProvider(
                                StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
                            .build();
    }

    private static void assertMockOnFailure(TransferListener transferListenerMock) {
        Mockito.verify(transferListenerMock, timeout(1000).times(1)).transferFailed(ArgumentMatchers.any());
        Mockito.verify(transferListenerMock, times(1)).transferInitiated(ArgumentMatchers.any());
        Mockito.verify(transferListenerMock, times(0)).transferComplete(ArgumentMatchers.any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void listeners_reports_ErrorsWithValidPayload(boolean multipartEnabled) {
        S3AsyncClient s3Async = s3AsyncClient(multipartEnabled);

        TransferListener transferListenerMock = mock(TransferListener.class);
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(404).withBody(ERROR_BODY)));
        S3TransferManager tm = new GenericS3TransferManager(s3Async, mock(UploadDirectoryHelper.class),
                                                            mock(TransferManagerConfiguration.class),
                                                            mock(DownloadDirectoryHelper.class));
        CaptureTransferListener transferListener = new CaptureTransferListener();

        FileUpload fileUpload =
            tm.uploadFile(u -> u.putObjectRequest(p -> p.bucket(EXAMPLE_BUCKET).key(TEST_KEY))
                                .source(testFile)
                                .addTransferListener(LoggingTransferListener.create())
                                .addTransferListener(transferListener)
                                .addTransferListener(transferListenerMock)
                                .build());

        assertTransferListenerCompletion(transferListener);
        assertThatExceptionOfType(CompletionException.class).isThrownBy(() -> fileUpload.completionFuture().join());
        assertThat(transferListener.getExceptionCaught()).isInstanceOf(NoSuchBucketException.class);
        assertThat(transferListener.isTransferComplete()).isFalse();
        assertThat(transferListener.isTransferInitiated()).isTrue();

        assertMockOnFailure(transferListenerMock);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void listeners_reports_ErrorsWithValidInValidPayload(boolean multipartEnabled) {
        S3AsyncClient s3Async = s3AsyncClient(multipartEnabled);

        TransferListener transferListenerMock = mock(TransferListener.class);
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(404).withBody("?")));
        S3TransferManager tm = new GenericS3TransferManager(s3Async, mock(UploadDirectoryHelper.class),
                                                            mock(TransferManagerConfiguration.class),
                                                            mock(DownloadDirectoryHelper.class));
        CaptureTransferListener transferListener = new CaptureTransferListener();

        FileUpload fileUpload =
            tm.uploadFile(u -> u.putObjectRequest(p -> p.bucket(EXAMPLE_BUCKET).key(TEST_KEY))
                                .source(testFile)
                                .addTransferListener(LoggingTransferListener.create())
                                .addTransferListener(transferListener)
                                .addTransferListener(transferListenerMock)
                                .build());

        assertTransferListenerCompletion(transferListener);
        assertThatExceptionOfType(CompletionException.class).isThrownBy(() -> fileUpload.completionFuture().join());
        assertThat(transferListener.getExceptionCaught()).isInstanceOf(S3Exception.class);
        assertThat(transferListener.isTransferComplete()).isFalse();
        assertThat(transferListener.isTransferInitiated()).isTrue();
        assertMockOnFailure(transferListenerMock);

    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void listeners_reports_ErrorsWhenCancelled(boolean multipartEnabled) {
        S3AsyncClient s3Async = s3AsyncClient(multipartEnabled);

        TransferListener transferListenerMock = mock(TransferListener.class);
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}")));
        S3TransferManager tm = new GenericS3TransferManager(s3Async, mock(UploadDirectoryHelper.class),
                                                            mock(TransferManagerConfiguration.class),
                                                            mock(DownloadDirectoryHelper.class));
        CaptureTransferListener transferListener = new CaptureTransferListener();

        tm.uploadFile(u -> u.putObjectRequest(p -> p.bucket(EXAMPLE_BUCKET).key(TEST_KEY))
                            .source(testFile)
                            .addTransferListener(LoggingTransferListener.create())
                            .addTransferListener(transferListener)
                            .addTransferListener(transferListenerMock)
                            .build()).completionFuture().cancel(true);
        assertTransferListenerCompletion(transferListener);
        assertThat(transferListener.getExceptionCaught()).isInstanceOf(CancellationException.class);
        assertThat(transferListener.isTransferComplete()).isFalse();
        assertThat(transferListener.isTransferInitiated()).isTrue();
        assertMockOnFailure(transferListenerMock);
    }

    @ParameterizedTest(name = "multipartEnabled = {0}")
    @ValueSource(booleans = {true, false})
    void listeners_reports_ProgressWhenSuccess(boolean multipartEnabled) {
        S3AsyncClient s3Async = s3AsyncClient(multipartEnabled);

        TransferListener transferListenerMock = mock(TransferListener.class);
        String createMpuUrl = "/" + EXAMPLE_BUCKET + "/" + TEST_KEY + "?uploads";
        String createMpuResponse = "<CreateMultipartUploadResult><UploadId>1234</UploadId></CreateMultipartUploadResult>";
        stubFor(post(urlEqualTo(createMpuUrl)).willReturn(aResponse().withStatus(200).withBody(createMpuResponse)));
        stubFor(any(anyUrl()).atPriority(6).willReturn(aResponse().withStatus(200).withBody("<body/>")));
        S3TransferManager tm = new GenericS3TransferManager(s3Async, mock(UploadDirectoryHelper.class),
                                                            mock(TransferManagerConfiguration.class),
                                                            mock(DownloadDirectoryHelper.class));
        CaptureTransferListener transferListener = new CaptureTransferListener();

        tm.uploadFile(u -> u.putObjectRequest(p -> p.bucket(EXAMPLE_BUCKET).key(TEST_KEY))
                            .source(testFile)
                            .addTransferListener(LoggingTransferListener.create())
                            .addTransferListener(transferListener)
                            .addTransferListener(transferListenerMock)
                            .build()).completionFuture().join();

        assertTransferListenerCompletion(transferListener);
        assertThat(transferListener.getExceptionCaught()).isNull();
        assertThat(transferListener.isTransferComplete()).isTrue();
        assertThat(transferListener.isTransferInitiated()).isTrue();
        Mockito.verify(transferListenerMock, times(0)).transferFailed(ArgumentMatchers.any());
        Mockito.verify(transferListenerMock, times(1)).transferInitiated(ArgumentMatchers.any());
        Mockito.verify(transferListenerMock, timeout(1000).times(1)).transferComplete(ArgumentMatchers.any());

        // when false, the generic S3 TM will read 16KiB chunks, so OBJ_SIZE / 16KiB = 16MiB / 16KiB = 1024
        int numTimesBytesTransferred = multipartEnabled ? 2 : 1024;
        Mockito.verify(transferListenerMock, times(numTimesBytesTransferred)).bytesTransferred(ArgumentMatchers.any());
    }

    @Test
    void copyWithJavaBasedClient_listeners_reports_ErrorsWithValidPayload() {
        S3AsyncClient s3Async = s3AsyncClient(true);

        TransferListener transferListenerMock = mock(TransferListener.class);
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(404).withBody(ERROR_BODY)));
        S3TransferManager tm = new GenericS3TransferManager(s3Async, mock(UploadDirectoryHelper.class),
                                                            mock(TransferManagerConfiguration.class),
                                                            mock(DownloadDirectoryHelper.class));
        CaptureTransferListener transferListener = new CaptureTransferListener();

        Copy copy =
            tm.copy(u -> u.copyObjectRequest(p -> p
                              .sourceBucket(EXAMPLE_BUCKET)
                              .sourceKey(TEST_KEY)
                              .destinationBucket(EXAMPLE_BUCKET)
                              .destinationKey("copiedObj"))
                          .addTransferListener(LoggingTransferListener.create())
                          .addTransferListener(transferListener)
                          .addTransferListener(transferListenerMock)
                          .build());
        assertTransferListenerCompletion(transferListener);
        assertThatExceptionOfType(CompletionException.class).isThrownBy(() -> copy.completionFuture().join());
        assertThat(transferListener.getExceptionCaught()).isInstanceOf(NoSuchKeyException.class);
        assertThat(transferListener.isTransferComplete()).isFalse();
        assertThat(transferListener.isTransferInitiated()).isTrue();
        assertMockOnFailure(transferListenerMock);
    }

    @Test
    void copyWithJavaBasedClient_listeners_reports_ErrorsWithValidInValidPayload() {
        S3AsyncClient s3Async = s3AsyncClient(true);

        TransferListener transferListenerMock = mock(TransferListener.class);
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(404).withBody("?")));
        S3TransferManager tm = new GenericS3TransferManager(s3Async, mock(UploadDirectoryHelper.class),
                                                            mock(TransferManagerConfiguration.class),
                                                            mock(DownloadDirectoryHelper.class));
        CaptureTransferListener transferListener = new CaptureTransferListener();

        Copy copy =
            tm.copy(u -> u.copyObjectRequest(p -> p
                              .sourceBucket(EXAMPLE_BUCKET)
                              .sourceKey(TEST_KEY)
                              .destinationBucket(EXAMPLE_BUCKET)
                              .destinationKey("copiedObj"))
                          .addTransferListener(LoggingTransferListener.create())
                          .addTransferListener(transferListener)
                          .addTransferListener(transferListenerMock)
                          .build());

        assertTransferListenerCompletion(transferListener);
        assertThatExceptionOfType(CompletionException.class).isThrownBy(() -> copy.completionFuture().join());
        assertThat(transferListener.getExceptionCaught()).isInstanceOf(S3Exception.class);
        assertThat(transferListener.isTransferComplete()).isFalse();
        assertThat(transferListener.isTransferInitiated()).isTrue();
        assertMockOnFailure(transferListenerMock);
    }

    @Test
    void copyWithJavaBasedClient_listeners_reports_ErrorsWhenCancelled() throws InterruptedException {
        S3AsyncClient s3Async = s3AsyncClient(true);

        TransferListener transferListenerMock = mock(TransferListener.class);
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}")));
        S3TransferManager tm = new GenericS3TransferManager(s3Async, mock(UploadDirectoryHelper.class),
                                                            mock(TransferManagerConfiguration.class),
                                                            mock(DownloadDirectoryHelper.class));
        CaptureTransferListener transferListener = new CaptureTransferListener();

        tm.copy(u -> u.copyObjectRequest(p -> p
                          .sourceBucket(EXAMPLE_BUCKET)
                          .sourceKey(TEST_KEY)
                          .destinationBucket(EXAMPLE_BUCKET)
                          .destinationKey("copiedObj"))
                      .addTransferListener(LoggingTransferListener.create())
                      .addTransferListener(transferListener)
                      .addTransferListener(transferListenerMock)
                      .build()).completionFuture().cancel(true);

        assertTransferListenerCompletion(transferListener);
        assertThat(transferListener.getExceptionCaught()).isInstanceOf(CancellationException.class);
        assertThat(transferListener.isTransferComplete()).isFalse();
        assertThat(transferListener.isTransferInitiated()).isTrue();
        assertMockOnFailure(transferListenerMock);
    }

    @Test
    void copyWithJavaBasedClient_listeners_reports_ProgressWhenSuccess_copy() {
        String destinationKey = "copiedObj";
        S3AsyncClient s3Async = s3AsyncClient(true);

        TransferListener transferListenerMock = mock(TransferListener.class);

        stubFor(head(anyUrl()).willReturn(aResponse().withStatus(200).withHeader("Content-Length", "16777216")));

        String createMpuUrl = "/" + EXAMPLE_BUCKET + "/" + destinationKey + "?uploads";
        String createMpuResponse = "<CreateMultipartUploadResult><UploadId>1234</UploadId></CreateMultipartUploadResult>";
        stubFor(post(urlEqualTo(createMpuUrl)).willReturn(aResponse().withStatus(200).withBody(createMpuResponse)));

        String copyObjectUrl = "/" + EXAMPLE_BUCKET + "/" + destinationKey + "?uploadId=1234";
        String copyObjectUrl1 = "/" + EXAMPLE_BUCKET + "/" + destinationKey + "?partNumber=1&uploadId=1234";
        String copyObjectUrl2 = "/" + EXAMPLE_BUCKET + "/" + destinationKey + "?partNumber=2&uploadId=1234";

        String copyObjectResponse = "<CopyPartResult><ETag>test-etag</ETag></CopyPartResult>";
        stubFor(post(copyObjectUrl).willReturn(aResponse().withStatus(200).withBody(copyObjectResponse)));
        stubFor(put(copyObjectUrl1).willReturn(aResponse().withStatus(200).withBody(copyObjectResponse)));
        stubFor(put(copyObjectUrl2).willReturn(aResponse().withStatus(200).withBody(copyObjectResponse)));

        S3TransferManager tm = new GenericS3TransferManager(s3Async, mock(UploadDirectoryHelper.class),
                                                            mock(TransferManagerConfiguration.class),
                                                            mock(DownloadDirectoryHelper.class));
        CaptureTransferListener transferListener = new CaptureTransferListener();

        tm.copy(u -> u.copyObjectRequest(p -> p
                          .sourceBucket(EXAMPLE_BUCKET)
                          .sourceKey(TEST_KEY)
                          .destinationBucket(EXAMPLE_BUCKET)
                          .destinationKey(destinationKey))
                      .addTransferListener(LoggingTransferListener.create())
                      .addTransferListener(transferListener)
                      .addTransferListener(transferListenerMock)
                      .build());

        assertTransferListenerCompletion(transferListener);
        assertThat(transferListener.getExceptionCaught()).isNull();
        assertThat(transferListener.isTransferComplete()).isTrue();
        assertThat(transferListener.isTransferInitiated()).isTrue();
        Mockito.verify(transferListenerMock, times(0)).transferFailed(ArgumentMatchers.any());
        Mockito.verify(transferListenerMock, times(1)).transferInitiated(ArgumentMatchers.any());
        Mockito.verify(transferListenerMock, times(1)).transferComplete(ArgumentMatchers.any());

        int numTimesBytesTransferred = 2;
        Mockito.verify(transferListenerMock, times(numTimesBytesTransferred)).bytesTransferred(ArgumentMatchers.any());
    }

    private static void assertTransferListenerCompletion(CaptureTransferListener transferListener) {
        Duration waitDuration = Duration.ofSeconds(5);
        assertTimeoutPreemptively(
            waitDuration, () -> {
                while (!transferListener.getCompletionFuture().isDone()) {
                    Thread.sleep(50);
                }
            }, "TransferListener future not completed even after waiting for " + waitDuration);
    }
}
