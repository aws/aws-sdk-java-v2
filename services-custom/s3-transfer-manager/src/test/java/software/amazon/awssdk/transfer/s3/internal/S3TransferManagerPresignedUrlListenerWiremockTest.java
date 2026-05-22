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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Download;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.PresignedDownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.PresignedDownloadRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

/**
 * Tests that TransferListener callbacks fire correctly for presigned URL downloads
 * with both multipart-enabled and non-multipart clients.
 */
@WireMockTest
public class S3TransferManagerPresignedUrlListenerWiremockTest {

    private static URI testEndpoint;
    private static RandomTempFile testFile;

    @BeforeAll
    public static void init(WireMockRuntimeInfo wm) throws IOException {
        testEndpoint = URI.create(wm.getHttpBaseUrl());
        testFile = new RandomTempFile("presigned-listener-test", 1024);
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

    static Stream<Arguments> presignedUrlTestCases() {
        return Stream.of(
            Arguments.of(true, "toFile", null),
            Arguments.of(true, "toFile", "bytes=0-511"),
            Arguments.of(true, "toBytes", null),
            Arguments.of(true, "toBytes", "bytes=0-511"),
            Arguments.of(false, "toFile", null),
            Arguments.of(false, "toFile", "bytes=0-511"),
            Arguments.of(false, "toBytes", null),
            Arguments.of(false, "toBytes", "bytes=0-511")
        );
    }

    @ParameterizedTest(name = "presignedUrlDownload_multipart={0}_type={1}_range={2}")
    @MethodSource("presignedUrlTestCases")
    void presignedUrlDownload_shouldInvokeListener(boolean multipartEnabled, String type, String range) throws Exception {
        S3AsyncClient s3Async = s3AsyncClient(multipartEnabled);
        S3TransferManager tm = new GenericS3TransferManager(s3Async, mock(UploadDirectoryHelper.class),
                                                            mock(TransferManagerConfiguration.class),
                                                            mock(DownloadDirectoryHelper.class));

        byte[] responseBody = new byte[512];
        stubFor(get(urlPathEqualTo("/presigned-key")).willReturn(aResponse()
                                                                    .withStatus(206)
                                                                    .withHeader("Content-Length", "512")
                                                                    .withHeader("Content-Range", "bytes 0-511/512")
                                                                    .withHeader("ETag", "\"test-etag\"")
                                                                    .withBody(responseBody)));

        TransferListener listener = mock(TransferListener.class);
        URL presignedUrl = new URL(testEndpoint + "/presigned-key?X-Amz-Algorithm=AWS4-HMAC-SHA256");

        PresignedUrlDownloadRequest.Builder requestBuilder = PresignedUrlDownloadRequest.builder()
                                                                                       .presignedUrl(presignedUrl);
        if (range != null) {
            requestBuilder.range(range);
        }

        if ("toFile".equals(type)) {
            FileDownload download = tm.downloadFileWithPresignedUrl(
                PresignedDownloadFileRequest.builder()
                    .presignedUrlDownloadRequest(requestBuilder.build())
                    .destination(testFile.toPath())
                    .addTransferListener(listener)
                    .build());
            download.completionFuture().join();
        } else {
            Download<?> download = tm.downloadWithPresignedUrl(
                PresignedDownloadRequest.builder()
                    .presignedUrlDownloadRequest(requestBuilder.build())
                    .responseTransformer(AsyncResponseTransformer.toBytes())
                    .addTransferListener(listener)
                    .build());
            download.completionFuture().join();
        }

        Mockito.verify(listener, timeout(1000).times(1)).transferInitiated(ArgumentMatchers.any());
        Mockito.verify(listener, timeout(1000).atLeastOnce()).bytesTransferred(ArgumentMatchers.any());

        tm.close();
        s3Async.close();
    }

    static Stream<Arguments> presignedUrlFailureTestCases() {
        return Stream.of(
            Arguments.of(true, "toFile"),
            Arguments.of(true, "toBytes"),
            Arguments.of(false, "toFile"),
            Arguments.of(false, "toBytes")
        );
    }

    @ParameterizedTest(name = "presignedUrlDownload_failure_multipart={0}_type={1}")
    @MethodSource("presignedUrlFailureTestCases")
    void presignedUrlDownload_failure_shouldInvokeListener(boolean multipartEnabled, String type) throws Exception {
        S3AsyncClient s3Async = s3AsyncClient(multipartEnabled);
        S3TransferManager tm = new GenericS3TransferManager(s3Async, mock(UploadDirectoryHelper.class),
                                                            mock(TransferManagerConfiguration.class),
                                                            mock(DownloadDirectoryHelper.class));

        stubFor(get(urlPathEqualTo("/presigned-key"))
                    .willReturn(aResponse().withStatus(404)
                                           .withBody("<Error><Code>TestError</Code><Message>Test failure</Message></Error>")));

        TransferListener listener = mock(TransferListener.class);
        URL presignedUrl = new URL(testEndpoint + "/presigned-key?X-Amz-Algorithm=AWS4-HMAC-SHA256");

        if ("toFile".equals(type)) {
            FileDownload download = tm.downloadFileWithPresignedUrl(
                PresignedDownloadFileRequest.builder()
                    .presignedUrlDownloadRequest(PresignedUrlDownloadRequest.builder()
                                                                           .presignedUrl(presignedUrl)
                                                                           .build())
                    .destination(testFile.toPath())
                    .addTransferListener(listener)
                    .build());
            assertThatExceptionOfType(CompletionException.class).isThrownBy(() -> download.completionFuture().join());
        } else {
            Download<?> download = tm.downloadWithPresignedUrl(
                PresignedDownloadRequest.builder()
                    .presignedUrlDownloadRequest(PresignedUrlDownloadRequest.builder()
                                                                           .presignedUrl(presignedUrl)
                                                                           .build())
                    .responseTransformer(AsyncResponseTransformer.toBytes())
                    .addTransferListener(listener)
                    .build());
            assertThatExceptionOfType(CompletionException.class).isThrownBy(() -> download.completionFuture().join());
        }

        Mockito.verify(listener, timeout(1000).times(1)).transferInitiated(ArgumentMatchers.any());
        Mockito.verify(listener, timeout(1000).times(1)).transferFailed(ArgumentMatchers.any());
        Mockito.verify(listener, times(0)).transferComplete(ArgumentMatchers.any());

        tm.close();
        s3Async.close();
    }

    @ParameterizedTest(name = "presignedUrlDownload_cancelled_multipart={0}_type={1}")
    @MethodSource("presignedUrlFailureTestCases")
    void presignedUrlDownload_cancelled_shouldInvokeTransferFailed(boolean multipartEnabled, String type) throws Exception {
        S3AsyncClient s3Async = s3AsyncClient(multipartEnabled);
        S3TransferManager tm = new GenericS3TransferManager(s3Async, mock(UploadDirectoryHelper.class),
                                                            mock(TransferManagerConfiguration.class),
                                                            mock(DownloadDirectoryHelper.class));

        // Slow response to keep request in-flight during cancellation
        stubFor(get(urlPathEqualTo("/presigned-key")).willReturn(aResponse()
                                                                     .withStatus(206)
                                                                     .withHeader("Content-Length", "512")
                                                                     .withHeader("Content-Range", "bytes 0-511/512")
                                                                     .withHeader("ETag", "\"test-etag\"")
                                                                     .withBody(new byte[512])
                                                                     .withFixedDelay(5000)));

        TransferListener listener = mock(TransferListener.class);
        URL presignedUrl = new URL(testEndpoint + "/presigned-key?X-Amz-Algorithm=AWS4-HMAC-SHA256");

        if ("toFile".equals(type)) {
            FileDownload download = tm.downloadFileWithPresignedUrl(
                PresignedDownloadFileRequest.builder()
                                            .presignedUrlDownloadRequest(PresignedUrlDownloadRequest.builder()
                                                                                                    .presignedUrl(presignedUrl)
                                                                                                    .build())
                                            .destination(testFile.toPath())
                                            .addTransferListener(listener)
                                            .build());
            download.completionFuture().cancel(true);
        } else {
            Download<?> download = tm.downloadWithPresignedUrl(
                PresignedDownloadRequest.builder()
                                        .presignedUrlDownloadRequest(PresignedUrlDownloadRequest.builder()
                                                                                                .presignedUrl(presignedUrl)
                                                                                                .build())
                                        .responseTransformer(AsyncResponseTransformer.toBytes())
                                        .addTransferListener(listener)
                                        .build());
            download.completionFuture().cancel(true);
        }

        Mockito.verify(listener, timeout(1000).times(1)).transferInitiated(ArgumentMatchers.any());
        Mockito.verify(listener, timeout(1000).times(1)).transferFailed(ArgumentMatchers.any());
        Mockito.verify(listener, times(0)).transferComplete(ArgumentMatchers.any());

        tm.close();
        s3Async.close();
    }
}
