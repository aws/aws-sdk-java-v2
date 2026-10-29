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
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.headRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.MULTIPART_DOWNLOAD_RESUME_CONTEXT;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartDownloadResumeContext;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;

/**
 * WireMock-based test that validates file content integrity after resuming a download.
 * Verifies that the correct bytes end up on disk after the full resume flow.
 */
class ResumeDownloadWireMockTest {
    // Some tests stub a response with a fixed delay (withFixedDelay) so they can pause the download before the response
    // arrives. WireMock keeps a server thread busy for the full delay, even after the client cancels the request. With
    // WireMock's default of 10 container threads, a few such delayed responses could use up all the threads and make the
    // next test's requests wait until a delay ended. Configuring the server with 50 threads avoids that.
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
                                                         .options(wireMockConfig().dynamicPort().containerThreads(50))
                                                         .configureStaticDsl(true)
                                                         .build();


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

    private static final int PART_SIZE = OBJECT_SIZE / 2;
    private static final String REPLACEMENT_E_TAG = "\"replacement1234567\"";
    private static final String REPLACEMENT_LAST_MODIFIED_HTTP = "Mon, 28 Sep 2026 13:00:00 GMT";
    private static final byte[] REPLACEMENT_OBJECT_CONTENT = buildDeterministicContent(OBJECT_SIZE, 13);

    private static final int FOUR_PARTS = 4;
    private static final int RESPONSE_DELAY_MILLIS = 3_000;

    private S3AsyncClient s3Client;
    private S3TransferManager tm;
    private S3AsyncClient multipartS3Client;
    private S3TransferManager multipartTm;
    private Path destination;

    @BeforeEach
    void setUp() throws IOException {
        WireMockRuntimeInfo wm = wireMock.getRuntimeInfo();
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

        multipartS3Client = S3AsyncClient.builder()
                                         .region(Region.US_EAST_1)
                                         .endpointOverride(URI.create("http://localhost:" + wm.getHttpPort()))
                                         .forcePathStyle(true)
                                         .multipartEnabled(true)
                                         .credentialsProvider(StaticCredentialsProvider.create(
                                             AwsBasicCredentials.create("key", "secret")))
                                         .build();
        multipartTm = S3TransferManager.builder()
                                       .s3Client(multipartS3Client)
                                       .build();
    }

    @AfterEach
    void tearDown() {
        tm.close();
        s3Client.close();
        multipartTm.close();
        multipartS3Client.close();
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

    @Test
    void resumeMultipartDownload_remainingPart_shouldSendIfMatchWithOriginalETag() throws IOException {
        Files.write(destination, part(FULL_OBJECT_CONTENT, 1));
        stubHeadObject();
        stubPart(2, E_TAG, LAST_MODIFIED_HTTP, FULL_OBJECT_CONTENT);

        multipartTm.resumeDownloadFile(pausedAfterFirstPart()).completionFuture().join();

        verify(getRequestedFor(urlEqualTo(partUrl(2))).withHeader("If-Match", equalTo(E_TAG)));
        assertThat(readFileBytes()).isEqualTo(FULL_OBJECT_CONTENT);
    }

    @Test
    void resumeMultipartDownload_objectReplacedDuringResume_shouldFailWithoutWritingReplacementBytes() throws IOException {
        Files.write(destination, part(FULL_OBJECT_CONTENT, 1));
        stubHeadObject();
        stubPreconditionFailedOnIfMatch(E_TAG);
        stubPart(2, REPLACEMENT_E_TAG, REPLACEMENT_LAST_MODIFIED_HTTP, REPLACEMENT_OBJECT_CONTENT);

        assertThatThrownBy(() -> multipartTm.resumeDownloadFile(pausedAfterFirstPart()).completionFuture().join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(S3Exception.class)
            .hasMessageContaining("At least one of the pre-conditions you specified did not hold");
        assertThat(readFileBytes()).isEqualTo(part(FULL_OBJECT_CONTENT, 1));
    }

    @Test
    void resumeMultipartDownload_objectReplacedWhilePaused_shouldRestartFromFirstPart() throws IOException {
        Files.write(destination, part(FULL_OBJECT_CONTENT, 1));
        stubHeadObject(REPLACEMENT_E_TAG, REPLACEMENT_LAST_MODIFIED_HTTP);
        stubPreconditionFailedOnIfMatch(E_TAG);
        stubPart(1, REPLACEMENT_E_TAG, REPLACEMENT_LAST_MODIFIED_HTTP, REPLACEMENT_OBJECT_CONTENT);
        stubPart(2, REPLACEMENT_E_TAG, REPLACEMENT_LAST_MODIFIED_HTTP, REPLACEMENT_OBJECT_CONTENT);

        CompletedFileDownload completed = multipartTm.resumeDownloadFile(pausedAfterFirstPart()).completionFuture().join();

        verify(getRequestedFor(urlEqualTo(partUrl(1))));
        verify(getRequestedFor(urlEqualTo(partUrl(2))).withHeader("If-Match", equalTo(REPLACEMENT_E_TAG)));
        verify(0, getRequestedFor(urlMatching(".*partNumber=.*")).withHeader("If-Match", equalTo(E_TAG)));
        assertThat(completed.response().eTag()).isEqualTo(REPLACEMENT_E_TAG);
        assertThat(readFileBytes()).isEqualTo(REPLACEMENT_OBJECT_CONTENT);
    }

    @Test
    void resumeMultipartDownload_localFileModified_shouldRestartFromFirstPart() throws IOException {
        Files.write(destination, new byte[PART_SIZE + 100]);
        stubHeadObject();
        stubPart(1, E_TAG, LAST_MODIFIED_HTTP, FULL_OBJECT_CONTENT);
        stubPart(2, E_TAG, LAST_MODIFIED_HTTP, FULL_OBJECT_CONTENT);

        multipartTm.resumeDownloadFile(pausedAfterFirstPart()).completionFuture().join();

        verify(getRequestedFor(urlEqualTo(partUrl(1))));
        assertThat(readFileBytes()).isEqualTo(FULL_OBJECT_CONTENT);
    }

    @Test
    void resumeMultipartDownload_tokenFromPausedCrtRestart_shouldResumeRemainingBytesByRange() throws IOException {
        S3CrtAsyncClient crtClient = mock(S3CrtAsyncClient.class);
        when(crtClient.headObject(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(HeadObjectResponse.builder()
                                                                            .eTag(E_TAG)
                                                                            .lastModified(LAST_MODIFIED)
                                                                            .contentLength((long) OBJECT_SIZE)
                                                                            .build()));
        when(crtClient.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(new CompletableFuture<>());
        int crtWrittenBytes = PART_SIZE / 2;
        ResumableFileDownload crtToken;
        try (S3TransferManager crtTm = new CrtS3TransferManager(mock(TransferManagerConfiguration.class), crtClient, false)) {
            FileDownload crtDownload = crtTm.resumeDownloadFile(pausedAfterFirstPart());
            Files.write(destination, Arrays.copyOf(FULL_OBJECT_CONTENT, crtWrittenBytes));
            crtToken = crtDownload.pause();
        }
        stubHeadObject();
        stubGetObjectForRange("bytes=" + crtWrittenBytes + "-" + OBJECT_SIZE,
                              Arrays.copyOfRange(FULL_OBJECT_CONTENT, crtWrittenBytes, OBJECT_SIZE));
        stubPart(2, E_TAG, LAST_MODIFIED_HTTP, FULL_OBJECT_CONTENT);

        multipartTm.resumeDownloadFile(crtToken).completionFuture().join();

        verify(0, getRequestedFor(urlMatching(".*partNumber=.*")));
        assertThat(readFileBytes()).isEqualTo(FULL_OBJECT_CONTENT);
    }

    static Stream<Arguments> partsCompletedOutOfOrder() {
        return Stream.of(Arguments.of(Arrays.asList(1, 3)),
                         Arguments.of(Arrays.asList(1, 4)));
    }

    @ParameterizedTest(name = "completed parts {0}")
    @MethodSource("partsCompletedOutOfOrder")
    void resumeMultipartDownload_serializedTokenWithPartsCompletedOutOfOrder_shouldRestartFromFirstPart(
        List<Integer> completedParts) throws Exception {
        ResumableFileDownload deserialized = serializedTokenWithCompletedParts(completedParts);
        stubHeadObject();
        stubFourParts(Collections.emptyList());
        stubRangeGetFrom(deserialized.bytesTransferred());

        multipartTm.resumeDownloadFile(deserialized).completionFuture().join();

        assertThat(firstDifference(readFileBytes(), FULL_OBJECT_CONTENT))
            .as("first offset at which the destination file differs from the object").isEqualTo(-1);
        verify(getRequestedFor(urlEqualTo(partUrl(1))));
        verify(getRequestedFor(urlEqualTo(partUrl(2))));
        verify(getRequestedFor(urlEqualTo(partUrl(3))));
        verify(getRequestedFor(urlEqualTo(partUrl(4))));
        verify(0, getRequestedFor(urlPathEqualTo(URL_PATH)).withHeader("Range", matching(".*")));
    }

    enum RePauseWindow {
        BEFORE_HEAD_RESPONSE,
        BEFORE_FIRST_PART_RESPONSE
    }

    // window, fileTouchedWhilePaused
    static Stream<Arguments> rePauseCases() {
        return Stream.of(Arguments.of(RePauseWindow.BEFORE_HEAD_RESPONSE, false),
                         Arguments.of(RePauseWindow.BEFORE_FIRST_PART_RESPONSE, false),
                         Arguments.of(RePauseWindow.BEFORE_FIRST_PART_RESPONSE, true));
    }

    @ParameterizedTest(name = "re-paused {0}, file touched while paused: {1}")
    @MethodSource("rePauseCases")
    void resumeMultipartDownload_restartRePausedBeforeWritingAnything_shouldStillRestartFromFirstPart(
        RePauseWindow window, boolean fileTouchedWhilePaused) throws Exception {
        ResumableFileDownload deserialized = serializedTokenWithCompletedParts(Arrays.asList(1, 3));
        if (fileTouchedWhilePaused) {
            destination.toFile().setLastModified(destination.toFile().lastModified() + 5_000);
        }
        delayResponseFor(window);
        FileDownload restarted = multipartTm.resumeDownloadFile(deserialized);
        awaitDelayedRequest(window);
        ResumableFileDownload rePaused = restarted.pause();
        ResumableFileDownload secondToken = ResumableFileDownload.fromString(rePaused.serializeToString());
        stubHeadObject();
        stubFourParts(Collections.emptyList());
        stubRangeGetFrom(rePaused.bytesTransferred());
        resetAllRequests();

        multipartTm.resumeDownloadFile(secondToken).completionFuture().join();

        assertThat(firstDifference(readFileBytes(), FULL_OBJECT_CONTENT))
            .as("first offset at which the destination file differs from the object").isEqualTo(-1);
        verify(getRequestedFor(urlEqualTo(partUrl(1))));
        verify(getRequestedFor(urlEqualTo(partUrl(2))));
        verify(getRequestedFor(urlEqualTo(partUrl(3))));
        verify(getRequestedFor(urlEqualTo(partUrl(4))));
        verify(0, getRequestedFor(urlPathEqualTo(URL_PATH)).withHeader("Range", matching(".*")));
    }

    @Test
    void resumeMultipartDownload_inMemoryTokenWithPartsCompletedOutOfOrder_shouldDownloadOnlyRemainingParts() throws Exception {
        ResumableFileDownload paused = inMemoryTokenWithCompletedParts(Arrays.asList(1, 3));
        stubHeadObject();
        stubFourParts(Collections.emptyList());

        multipartTm.resumeDownloadFile(paused).completionFuture().join();

        verify(0, getRequestedFor(urlEqualTo(partUrl(1))));
        verify(getRequestedFor(urlEqualTo(partUrl(2))));
        verify(0, getRequestedFor(urlEqualTo(partUrl(3))));
        verify(getRequestedFor(urlEqualTo(partUrl(4))));
        assertThat(readFileBytes()).isEqualTo(FULL_OBJECT_CONTENT);
    }

    /**
     * The token a paused download of the four-part object leaves after a serialize and deserialize round trip, when
     * {@code completedParts} had completed and the parts in between had not: the destination file holds those parts at
     * their offsets with zeros in between, and the token has no resume context.
     */
    private ResumableFileDownload serializedTokenWithCompletedParts(List<Integer> completedParts) throws IOException {
        ResumableFileDownload token = tokenWithCompletedParts(completedParts, GetObjectRequest.builder()
                                                                                              .bucket(BUCKET)
                                                                                              .key(KEY)
                                                                                              .build());
        return ResumableFileDownload.fromString(token.serializeToString());
    }

    /**
     * The token the same paused download leaves in memory: like {@link #serializedTokenWithCompletedParts}, but its request
     * still carries the resume context that records the completed parts.
     */
    private ResumableFileDownload inMemoryTokenWithCompletedParts(List<Integer> completedParts) throws IOException {
        int partSize = OBJECT_SIZE / FOUR_PARTS;
        MultipartDownloadResumeContext resumeContext =
            new MultipartDownloadResumeContext(completedParts, (long) partSize * completedParts.size());
        resumeContext.totalParts(FOUR_PARTS);
        resumeContext.response(GetObjectResponse.builder()
                                                .eTag(E_TAG)
                                                .lastModified(LAST_MODIFIED)
                                                .partsCount(FOUR_PARTS)
                                                .contentLength((long) partSize)
                                                .contentRange("bytes 0-" + (partSize - 1) + "/" + OBJECT_SIZE)
                                                .build());
        GetObjectRequest getObjectRequest =
            GetObjectRequest.builder()
                            .bucket(BUCKET)
                            .key(KEY)
                            .overrideConfiguration(c -> c.putExecutionAttribute(MULTIPART_DOWNLOAD_RESUME_CONTEXT,
                                                                                resumeContext))
                            .build();
        return tokenWithCompletedParts(completedParts, getObjectRequest);
    }

    private ResumableFileDownload tokenWithCompletedParts(List<Integer> completedParts, GetObjectRequest getObjectRequest)
        throws IOException {
        int partSize = OBJECT_SIZE / FOUR_PARTS;
        byte[] fileContent = new byte[Collections.max(completedParts) * partSize];
        for (int part : completedParts) {
            int start = (part - 1) * partSize;
            System.arraycopy(FULL_OBJECT_CONTENT, start, fileContent, start, partSize);
        }
        Files.write(destination, fileContent);

        return ResumableFileDownload.builder()
                                    .downloadFileRequest(d -> d.getObjectRequest(getObjectRequest).destination(destination))
                                    .bytesTransferred((long) fileContent.length)
                                    .fileLastModified(Instant.ofEpochMilli(destination.toFile().lastModified()))
                                    .s3ObjectLastModified(LAST_MODIFIED)
                                    .s3ObjectEtag(E_TAG)
                                    .totalSizeInBytes((long) OBJECT_SIZE)
                                    .completedParts(completedParts)
                                    .build();
    }

    private void delayResponseFor(RePauseWindow window) {
        if (window == RePauseWindow.BEFORE_HEAD_RESPONSE) {
            stubFor(head(urlPathEqualTo(URL_PATH))
                        .willReturn(aResponse().withStatus(200)
                                               .withFixedDelay(RESPONSE_DELAY_MILLIS)
                                               .withHeader("Content-Length", String.valueOf(OBJECT_SIZE))
                                               .withHeader("ETag", E_TAG)
                                               .withHeader("Last-Modified", LAST_MODIFIED_HTTP)));
            return;
        }
        stubHeadObject();
        stubFourParts(Collections.singletonList(1));
    }

    private void awaitDelayedRequest(RePauseWindow window) throws InterruptedException {
        RequestPatternBuilder delayedRequest = window == RePauseWindow.BEFORE_HEAD_RESPONSE
                                            ? headRequestedFor(urlPathEqualTo(URL_PATH))
                                            : getRequestedFor(urlEqualTo(partUrl(1)));
        long deadline = System.currentTimeMillis() + 10_000;
        while (findAll(delayedRequest).isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }
        assertThat(findAll(delayedRequest)).as("delayed request received for %s", window).isNotEmpty();
    }

    private void stubFourParts(List<Integer> delayedParts) {
        for (int part = 1; part <= FOUR_PARTS; part++) {
            stubPart(part, FOUR_PARTS, E_TAG, LAST_MODIFIED_HTTP, FULL_OBJECT_CONTENT,
                     delayedParts.contains(part) ? RESPONSE_DELAY_MILLIS : 0);
        }
    }

    /**
     * Answers a range GET from {@code start} the way S3 would: with the rest of the object, or 416 if {@code start} is at the
     * end of the object.
     */
    private void stubRangeGetFrom(long start) {
        if (start < OBJECT_SIZE) {
            stubGetObjectForRange("bytes=" + start + "-" + OBJECT_SIZE,
                                  Arrays.copyOfRange(FULL_OBJECT_CONTENT, (int) start, OBJECT_SIZE));
            return;
        }
        stubFor(get(urlPathEqualTo(URL_PATH))
                    .withHeader("Range", matching("bytes=" + start + "-.*"))
                    .willReturn(aResponse().withStatus(416)
                                           .withBody("<Error><Code>InvalidRange</Code>"
                                                     + "<Message>The requested range is not satisfiable</Message></Error>")));
    }

    private ResumableFileDownload pausedAfterFirstPart() {
        MultipartDownloadResumeContext resumeContext =
            new MultipartDownloadResumeContext(Collections.singletonList(1), (long) PART_SIZE);
        resumeContext.totalParts(2);
        resumeContext.response(GetObjectResponse.builder()
                                                .eTag(E_TAG)
                                                .lastModified(LAST_MODIFIED)
                                                .partsCount(2)
                                                .contentLength((long) PART_SIZE)
                                                .contentRange("bytes 0-" + (PART_SIZE - 1) + "/" + OBJECT_SIZE)
                                                .build());

        GetObjectRequest getObjectRequest =
            GetObjectRequest.builder()
                            .bucket(BUCKET)
                            .key(KEY)
                            .overrideConfiguration(c -> c.putExecutionAttribute(MULTIPART_DOWNLOAD_RESUME_CONTEXT,
                                                                                resumeContext))
                            .build();
        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                     .getObjectRequest(getObjectRequest)
                                                                     .destination(destination)
                                                                     .build();
        return ResumableFileDownload.builder()
                                    .downloadFileRequest(downloadFileRequest)
                                    .bytesTransferred((long) PART_SIZE)
                                    .fileLastModified(Instant.ofEpochMilli(destination.toFile().lastModified()))
                                    .s3ObjectLastModified(LAST_MODIFIED)
                                    .s3ObjectEtag(E_TAG)
                                    .totalSizeInBytes((long) OBJECT_SIZE)
                                    .completedParts(Collections.singletonList(1))
                                    .build();
    }

    private void stubPart(int partNumber, String eTag, String lastModifiedHttp, byte[] objectContent) {
        stubPart(partNumber, 2, eTag, lastModifiedHttp, objectContent, 0);
    }

    private void stubPart(int partNumber, int partCount, String eTag, String lastModifiedHttp, byte[] objectContent,
                          int delayMillis) {
        int partSize = objectContent.length / partCount;
        int start = (partNumber - 1) * partSize;
        stubFor(get(urlEqualTo(partUrl(partNumber)))
                    .atPriority(2)
                    .willReturn(aResponse().withStatus(206)
                                           .withFixedDelay(delayMillis)
                                           .withHeader("ETag", eTag)
                                           .withHeader("Last-Modified", lastModifiedHttp)
                                           .withHeader("x-amz-mp-parts-count", String.valueOf(partCount))
                                           .withHeader("Content-Range",
                                                       "bytes " + start + "-" + (start + partSize - 1) + "/" + objectContent.length)
                                           .withHeader("Content-Length", String.valueOf(partSize))
                                           .withBody(Arrays.copyOfRange(objectContent, start, start + partSize))));
    }

    private void stubPreconditionFailedOnIfMatch(String eTag) {
        stubFor(get(urlMatching(".*partNumber=.*"))
                    .atPriority(1)
                    .withHeader("If-Match", equalTo(eTag))
                    .willReturn(aResponse().withStatus(412)
                                           .withBody("<Error><Code>PreconditionFailed</Code>"
                                                     + "<Message>At least one of the pre-conditions you specified did not "
                                                     + "hold</Message></Error>")));
    }

    private static String partUrl(int partNumber) {
        return URL_PATH + "?partNumber=" + partNumber;
    }

    private static byte[] part(byte[] objectContent, int partNumber) {
        int start = (partNumber - 1) * PART_SIZE;
        return Arrays.copyOfRange(objectContent, start, start + PART_SIZE);
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
        stubHeadObject(E_TAG, LAST_MODIFIED_HTTP);
    }

    private void stubHeadObject(String eTag, String lastModifiedHttp) {
        stubFor(head(urlPathEqualTo(URL_PATH))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Length", String.valueOf(OBJECT_SIZE))
                                    .withHeader("Last-Modified", lastModifiedHttp)
                                    .withHeader("ETag", eTag)));
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

    private static int firstDifference(byte[] actual, byte[] expected) {
        int common = Math.min(actual.length, expected.length);
        for (int i = 0; i < common; i++) {
            if (actual[i] != expected[i]) {
                return i;
            }
        }
        return actual.length == expected.length ? -1 : common;
    }

    private byte[] readFileBytes() {
        try {
            return Files.readAllBytes(destination);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read destination file", e);
        }
    }

    private static byte[] buildDeterministicContent(int size) {
        return buildDeterministicContent(size, 251);
    }

    private static byte[] buildDeterministicContent(int size, int modulus) {
        byte[] content = new byte[size];
        for (int i = 0; i < size; i++) {
            content[i] = (byte) (i % modulus);
        }
        return content;
    }
}
