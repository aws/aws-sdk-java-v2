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

package software.amazon.awssdk.transfer.s3.internal.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.s3.ResumeToken;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions;
import software.amazon.awssdk.services.s3.internal.crt.S3MetaRequestPauseObservable;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgress;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Note that the download-specific fields of {@link ResumeToken} are populated by CRT's native code and there is no public
 * builder for them, so a download token can only be mocked here rather than constructed.
 */
class CrtFileDownloadTest {

    private static final String ETAG = "\"abc123\"";
    private static final Instant S3_OBJECT_LAST_MODIFIED = Instant.parse("2026-07-15T10:15:30Z");
    private static final String S3_OBJECT_LAST_MODIFIED_HTTP_DATE = "Wed, 15 Jul 2026 10:15:30 GMT";
    private static final long OBJECT_SIZE = 8000L;
    private static final long FILE_CONTENT_LENGTH = 2000L;

    private File file;
    private S3MetaRequestPauseObservable observable;
    private CompletableFuture<CompletedFileDownload> completionFuture;
    private DownloadFileRequest request;

    @BeforeEach
    void setUp() throws IOException {
        file = File.createTempFile("test", UUID.randomUUID().toString());
        Files.write(file.toPath(), RandomStringUtils.randomAlphanumeric((int) FILE_CONTENT_LENGTH)
                                                   .getBytes(StandardCharsets.UTF_8));
        observable = mock(S3MetaRequestPauseObservable.class);
        completionFuture = new CompletableFuture<>();
        request = DownloadFileRequest.builder()
                                     .getObjectRequest(r -> r.bucket("bucket").key("key"))
                                     .destination(file)
                                     .build();
    }

    @AfterEach
    void tearDown() {
        file.delete();
    }

    @Test
    void pause_withDownloadResumeToken_takesObjectMetadataFromToken() {
        ResumeToken token = downloadResumeToken(0, 1500);
        when(observable.pauseAsync()).thenReturn(CompletableFuture.completedFuture(token));

        ResumableFileDownload resumable = fileDownload(null).pause();

        assertThat(resumable.bytesTransferred()).isEqualTo(1500L);
        assertThat(resumable.s3ObjectEtag()).hasValue(ETAG);
        assertThat(resumable.s3ObjectLastModified()).hasValue(S3_OBJECT_LAST_MODIFIED);
        assertThat(resumable.totalSizeInBytes()).hasValue(OBJECT_SIZE);
        assertThat(resumable.fileLastModified()).isEqualTo(Instant.ofEpochMilli(file.lastModified()));
        assertThat(resumable.downloadFileRequest()).isEqualTo(request);
        assertThat(completionFuture).isCancelled();
    }

    @Test
    void pause_withDownloadResumeTokenForRangedDownload_bytesTransferredIsAbsoluteObjectOffset() {
        // A resumed download issues a ranged GET, so CRT reports the gap-free prefix relative to the range start rather than
        // to the start of the object.
        ResumeToken token = downloadResumeToken(2000, 1500);
        when(observable.pauseAsync()).thenReturn(CompletableFuture.completedFuture(token));

        ResumableFileDownload resumable = fileDownload(null).pause();

        assertThat(resumable.bytesTransferred()).isEqualTo(3500L);
    }

    @Test
    void pause_pauseReturnsNoToken_fallsBackToFileLengthAndResumedDownloadMetadata() {
        when(observable.pauseAsync()).thenReturn(CompletableFuture.completedFuture(null));
        ResumableFileDownload resumedDownload = resumedDownload();

        ResumableFileDownload resumable = fileDownload(resumedDownload).pause();

        assertThat(resumable.bytesTransferred()).isEqualTo(FILE_CONTENT_LENGTH);
        assertThat(resumable.s3ObjectEtag()).hasValue(ETAG);
        assertThat(resumable.s3ObjectLastModified()).hasValue(S3_OBJECT_LAST_MODIFIED);
        assertThat(resumable.totalSizeInBytes()).hasValue(OBJECT_SIZE);
    }

    @Test
    void pause_pauseReturnsNoTokenAndNoResumedDownload_returnsNoObjectMetadata() {
        when(observable.pauseAsync()).thenReturn(CompletableFuture.completedFuture(null));

        ResumableFileDownload resumable = fileDownload(null).pause();

        assertThat(resumable.bytesTransferred()).isEqualTo(FILE_CONTENT_LENGTH);
        assertThat(resumable.s3ObjectEtag()).isEmpty();
        assertThat(resumable.s3ObjectLastModified()).isEmpty();
        assertThat(resumable.totalSizeInBytes()).isEmpty();
    }

    @Test
    void pause_uploadResumeToken_ignoresTokenAndDoesNotThrow() {
        // The download-specific getters throw when read off an upload token, so the token type must be checked first.
        ResumeToken uploadToken = new ResumeToken(new ResumeToken.PutResumeTokenBuilder().withPartSize(8L)
                                                                                        .withTotalNumParts(2L)
                                                                                        .withNumPartsCompleted(1L)
                                                                                        .withUploadId("id"));
        when(observable.pauseAsync()).thenReturn(CompletableFuture.completedFuture(uploadToken));

        ResumableFileDownload resumable = fileDownload(null).pause();

        assertThat(resumable.bytesTransferred()).isEqualTo(FILE_CONTENT_LENGTH);
        assertThat(resumable.s3ObjectEtag()).isEmpty();
    }

    @Test
    void pause_completionFutureAlreadyComplete_doesNotPauseAndUsesResponseFromProgress() {
        GetObjectResponse response = GetObjectResponse.builder()
                                                     .eTag(ETAG)
                                                     .lastModified(S3_OBJECT_LAST_MODIFIED)
                                                     .contentLength(OBJECT_SIZE)
                                                     .build();
        completionFuture.complete(CompletedFileDownload.builder().response(response).build());
        TransferProgress progress = new DefaultTransferProgress(
            DefaultTransferProgressSnapshot.builder()
                                           .transferredBytes(FILE_CONTENT_LENGTH)
                                           .totalBytes(OBJECT_SIZE)
                                           .sdkResponse(response)
                                           .build());

        ResumableFileDownload resumable = new CrtFileDownload(completionFuture, progress, observable, () -> request, null)
            .pause();

        verify(observable, never()).pauseAsync();
        assertThat(resumable.bytesTransferred()).isEqualTo(FILE_CONTENT_LENGTH);
        assertThat(resumable.s3ObjectEtag()).hasValue(ETAG);
        assertThat(resumable.s3ObjectLastModified()).hasValue(S3_OBJECT_LAST_MODIFIED);
        assertThat(resumable.totalSizeInBytes()).hasValue(OBJECT_SIZE);
    }

    @Test
    void pause_completedEmptyObject_omitsTotalSize() {
        // ResumableFileDownload rejects a non-positive totalSizeInBytes, so a zero-length object must not be passed through.
        GetObjectResponse response = GetObjectResponse.builder().eTag(ETAG).contentLength(0L).build();
        completionFuture.complete(CompletedFileDownload.builder().response(response).build());
        TransferProgress progress = new DefaultTransferProgress(
            DefaultTransferProgressSnapshot.builder().transferredBytes(0L).sdkResponse(response).build());

        ResumableFileDownload resumable = new CrtFileDownload(completionFuture, progress, observable, () -> request, null)
            .pause();

        assertThat(resumable.totalSizeInBytes()).isEmpty();
        assertThat(resumable.s3ObjectEtag()).hasValue(ETAG);
    }

    @Test
    void pause_pauseTimesOut_stillReturnsResumableFromFallbackMetadata() {
        // Never completes, so the bounded wait in pause() has to give up rather than block the caller forever.
        when(observable.pauseAsync()).thenReturn(new CompletableFuture<>());

        ResumableFileDownload resumable =
            new CrtFileDownload(completionFuture, progress(), observable, () -> request, resumedDownload(),
                                Duration.ofMillis(50)).pause();

        assertThat(resumable.s3ObjectEtag()).hasValue(ETAG);
        assertThat(resumable.bytesTransferred()).isEqualTo(FILE_CONTENT_LENGTH);
        assertThat(completionFuture).isCancelled();
    }

    @Test
    void pause_pauseFails_returnsResumableThatRestartsFromTheBeginning() {
        when(observable.pauseAsync()).thenReturn(CompletableFutureUtils.failedFuture(new CrtRuntimeException(1)));

        ResumableFileDownload resumable = fileDownload(null).pause();

        assertThat(resumable.bytesTransferred()).isEqualTo(FILE_CONTENT_LENGTH);
        assertThat(resumable.s3ObjectEtag()).isEmpty();
        assertThat(completionFuture).isCancelled();
    }

    @Test
    void pause_awaitsTheCrtPauseBeforeCancellingTheTransfer() {
        // Cancelling closes the underlying CRT meta request, and a closed meta request can no longer hand back a resume
        // token, so the pause has to be awaited first.
        ResumeToken token = downloadResumeToken(0, 1500);
        AtomicBoolean alreadyCancelledWhenPaused = new AtomicBoolean(true);
        when(observable.pauseAsync()).thenAnswer(invocation -> {
            alreadyCancelledWhenPaused.set(completionFuture.isCancelled());
            return CompletableFuture.completedFuture(token);
        });

        fileDownload(null).pause();

        assertThat(alreadyCancelledWhenPaused).isFalse();
        assertThat(completionFuture).isCancelled();
    }

    @Test
    void pause_calledTwice_pausesUnderlyingRequestOnlyOnce() {
        ResumeToken token = downloadResumeToken(0, 1500);
        when(observable.pauseAsync()).thenReturn(CompletableFuture.completedFuture(token));
        CrtFileDownload download = fileDownload(null);

        ResumableFileDownload first = download.pause();
        ResumableFileDownload second = download.pause();

        assertThat(first).isSameAs(second);
        verify(observable, times(1)).pauseAsync();
    }

    @Test
    void pause_unparsableLastModifiedFromToken_omitsLastModified() {
        ResumeToken token = downloadResumeToken(0, 1500);
        when(token.getS3ObjectLastModified()).thenReturn("not-a-date");
        when(observable.pauseAsync()).thenReturn(CompletableFuture.completedFuture(token));

        ResumableFileDownload resumable = fileDownload(null).pause();

        assertThat(resumable.s3ObjectLastModified()).isEmpty();
        assertThat(resumable.s3ObjectEtag()).hasValue(ETAG);
    }

    @Test
    void pause_tokenWithUnknownObjectSize_omitsTotalSize() {
        ResumeToken token = downloadResumeToken(0, 0);
        when(token.getObjectSize()).thenReturn(0L);
        when(observable.pauseAsync()).thenReturn(CompletableFuture.completedFuture(token));

        ResumableFileDownload resumable = fileDownload(null).pause();

        assertThat(resumable.totalSizeInBytes()).isEmpty();
        assertThat(resumable.bytesTransferred()).isZero();
    }

    private CrtFileDownload fileDownload(ResumableFileDownload resumedDownload) {
        return new CrtFileDownload(completionFuture, progress(), observable, () -> request, resumedDownload);
    }

    private TransferProgress progress() {
        return new DefaultTransferProgress(DefaultTransferProgressSnapshot.builder().transferredBytes(0L).build());
    }

    private ResumableFileDownload resumedDownload() {
        return ResumableFileDownload.builder()
                                    .downloadFileRequest(request)
                                    .bytesTransferred(FILE_CONTENT_LENGTH)
                                    .fileLastModified(Instant.ofEpochMilli(file.lastModified()))
                                    .s3ObjectEtag(ETAG)
                                    .s3ObjectLastModified(S3_OBJECT_LAST_MODIFIED)
                                    .totalSizeInBytes(OBJECT_SIZE)
                                    .build();
    }

    private static ResumeToken downloadResumeToken(long objectRangeStart, long continuesDownloadedBytes) {
        ResumeToken token = mock(ResumeToken.class);
        when(token.getType()).thenReturn(S3MetaRequestOptions.MetaRequestType.GET_OBJECT);
        when(token.getObjectRangeStart()).thenReturn(objectRangeStart);
        when(token.getContinuesDownloadedBytes()).thenReturn(continuesDownloadedBytes);
        when(token.getEtag()).thenReturn(ETAG);
        when(token.getS3ObjectLastModified()).thenReturn(S3_OBJECT_LAST_MODIFIED_HTTP_DATE);
        when(token.getObjectSize()).thenReturn(OBJECT_SIZE);
        return token;
    }
}
