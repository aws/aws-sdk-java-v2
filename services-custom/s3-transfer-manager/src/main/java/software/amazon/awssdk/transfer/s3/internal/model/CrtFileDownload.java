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

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.crt.s3.ResumeToken;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions;
import software.amazon.awssdk.services.s3.internal.crt.S3MetaRequestPauseObservable;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * A {@link FileDownload} for a download whose response body is written to the destination file by CRT rather than by an
 * {@code AsyncResponseTransformer}.
 * <p>
 * Because the response body never passes through the SDK, the response headers are not surfaced until the transfer finishes,
 * so the ETag and last-modified time needed to resume the download cannot be read from the progress snapshot the way
 * {@link DefaultFileDownload} reads them. They are instead taken from the {@link ResumeToken} that CRT hands back when the
 * meta request is paused.
 */
@SdkInternalApi
public final class CrtFileDownload implements FileDownload {
    private static final Logger log = Logger.loggerFor(CrtFileDownload.class);

    /**
     * How long to wait for CRT to quiesce the transfer when pausing. {@link #pause()} is a blocking API, so this bounds how
     * long a caller can be held up by an unresponsive meta request.
     */
    private static final Duration DEFAULT_PAUSE_TIMEOUT = Duration.ofSeconds(10);

    private final Lazy<ResumableFileDownload> resumableFileDownload;
    private final CompletableFuture<CompletedFileDownload> completionFuture;
    private final TransferProgress progress;
    private final S3MetaRequestPauseObservable observable;
    private final Supplier<DownloadFileRequest> requestSupplier;
    private final ResumableFileDownload resumedDownload;
    private final Duration pauseTimeout;

    public CrtFileDownload(CompletableFuture<CompletedFileDownload> completionFuture,
                           TransferProgress progress,
                           S3MetaRequestPauseObservable observable,
                           Supplier<DownloadFileRequest> requestSupplier,
                           ResumableFileDownload resumedDownload) {
        this(completionFuture, progress, observable, requestSupplier, resumedDownload, DEFAULT_PAUSE_TIMEOUT);
    }

    @SdkTestInternalApi
    CrtFileDownload(CompletableFuture<CompletedFileDownload> completionFuture,
                    TransferProgress progress,
                    S3MetaRequestPauseObservable observable,
                    Supplier<DownloadFileRequest> requestSupplier,
                    ResumableFileDownload resumedDownload,
                    Duration pauseTimeout) {
        this.completionFuture = Validate.paramNotNull(completionFuture, "completionFuture");
        this.progress = Validate.paramNotNull(progress, "progress");
        this.observable = Validate.paramNotNull(observable, "observable");
        this.requestSupplier = Validate.paramNotNull(requestSupplier, "requestSupplier");
        this.resumedDownload = resumedDownload;
        this.pauseTimeout = Validate.paramNotNull(pauseTimeout, "pauseTimeout");
        this.resumableFileDownload = new Lazy<>(this::doPause);
    }

    @Override
    public TransferProgress progress() {
        return progress;
    }

    @Override
    public CompletableFuture<CompletedFileDownload> completionFuture() {
        return completionFuture;
    }

    @Override
    public ResumableFileDownload pause() {
        return resumableFileDownload.getValue();
    }

    private ResumableFileDownload doPause() {
        if (completionFuture.isDone() && !completionFuture.isCompletedExceptionally()) {
            log.debug(() -> "The download future was already completed. There will be no CRT ResumeToken returned.");
            return resumableFileDownload(null);
        }

        // The pause must be awaited before the completion future is cancelled: cancelling closes the underlying CRT meta
        // request, and a closed meta request can no longer produce a resume token.
        ResumeToken token = pauseMetaRequest();
        completionFuture.cancel(true);
        return resumableFileDownload(token);
    }

    /**
     * Pauses the underlying CRT meta request and waits for it to quiesce, which for a download to a file means the file has
     * been fully written and closed.
     *
     * @return the resume token, or null if none was available. Without a token the object metadata needed to resume has to
     *         come from elsewhere, and if it is not available the download simply starts over, which is always safe.
     */
    private ResumeToken pauseMetaRequest() {
        try {
            return observable.pauseAsync().get(pauseTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw SdkClientException.create("The thread was interrupted while pausing the download", e);
        } catch (TimeoutException e) {
            log.warn(() -> String.format("Timed out after %s waiting for the download to pause. It will be restarted from "
                                         + "the beginning if resumed.", pauseTimeout));
        } catch (ExecutionException e) {
            log.warn(() -> "Failed to pause the download. It will be restarted from the beginning if resumed.", e.getCause());
        }
        return null;
    }

    private ResumableFileDownload resumableFileDownload(ResumeToken token) {
        DownloadFileRequest request = requestSupplier.get();
        File destination = request.destination().toFile();

        ResumableFileDownload.Builder builder =
            ResumableFileDownload.builder()
                                 .downloadFileRequest(request)
                                 .fileLastModified(Instant.ofEpochMilli(destination.lastModified()));

        if (isDownloadResumeToken(token)) {
            // getContinuesDownloadedBytes() is the length of the gap-free prefix that CRT has downloaded, relative to the
            // start of this request's range, so offsetting it by the range start gives the absolute offset in the object,
            // which is also the number of bytes that should be in the destination file. If parts landed out of order the
            // file may be longer than that, in which case the file-modified check on resume fails and the download
            // correctly starts over rather than appending onto a gap.
            builder.bytesTransferred(token.getObjectRangeStart() + token.getContinuesDownloadedBytes())
                   .s3ObjectEtag(emptyToNull(token.getEtag()))
                   .s3ObjectLastModified(s3ObjectLastModified(token))
                   .totalSizeInBytes(positiveOrNull(token.getObjectSize()));
            return builder.build();
        }

        builder.bytesTransferred(destination.length());
        populateFromResponseOrResumedDownload(builder);
        return builder.build();
    }

    /**
     * Populates the S3 object metadata needed to detect whether the object changed while the download was paused, for the
     * cases where CRT did not give us a resume token: either the transfer already finished, in which case the response is on
     * the progress snapshot, or it never really started, in which case anything we knew came from the
     * {@link ResumableFileDownload} being resumed.
     */
    private void populateFromResponseOrResumedDownload(ResumableFileDownload.Builder builder) {
        TransferProgressSnapshot snapshot = progress.snapshot();
        if (snapshot.sdkResponse().isPresent() && snapshot.sdkResponse().get() instanceof GetObjectResponse) {
            GetObjectResponse response = (GetObjectResponse) snapshot.sdkResponse().get();
            Long contentLength = response.contentLength();
            builder.s3ObjectEtag(response.eTag())
                   .s3ObjectLastModified(response.lastModified())
                   .totalSizeInBytes(contentLength == null ? null : positiveOrNull(contentLength));
        } else if (resumedDownload != null) {
            builder.s3ObjectEtag(resumedDownload.s3ObjectEtag().orElse(null))
                   .s3ObjectLastModified(resumedDownload.s3ObjectLastModified().orElse(null))
                   .totalSizeInBytes(resumedDownload.totalSizeInBytes().isPresent()
                                     ? resumedDownload.totalSizeInBytes().getAsLong()
                                     : null);
        }
    }

    /**
     * CRT's download-specific getters throw if they are read off an upload token, so the token type has to be checked before
     * any of them are used.
     */
    private static boolean isDownloadResumeToken(ResumeToken token) {
        return token != null && token.getType() == S3MetaRequestOptions.MetaRequestType.GET_OBJECT;
    }

    private static Instant s3ObjectLastModified(ResumeToken token) {
        String lastModified = emptyToNull(token.getS3ObjectLastModified());
        if (lastModified == null) {
            return null;
        }
        try {
            return DateUtils.parseRfc1123Date(lastModified);
        } catch (RuntimeException e) {
            // Losing the last-modified time only costs us the ability to resume; it must not fail the pause itself.
            log.warn(() -> "Could not parse the last-modified time reported by CRT. The download will be restarted from the "
                           + "beginning if resumed.", e);
            return null;
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static Long positiveOrNull(long value) {
        return value > 0 ? value : null;
    }

    @Override
    public String toString() {
        return ToString.builder("CrtFileDownload")
                       .add("completionFuture", completionFuture)
                       .add("progress", progress)
                       .add("request", requestSupplier.get())
                       .build();
    }
}
