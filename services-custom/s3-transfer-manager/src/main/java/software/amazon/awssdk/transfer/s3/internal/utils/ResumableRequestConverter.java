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

package software.amazon.awssdk.transfer.s3.internal.utils;

import static software.amazon.awssdk.core.FileTransformerConfiguration.FailureBehavior.LEAVE;
import static software.amazon.awssdk.core.FileTransformerConfiguration.FileWriteOption.WRITE_TO_POSITION;
import static software.amazon.awssdk.transfer.s3.internal.utils.FileUtils.fileNotModified;

import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.FileTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartDownloadResumeContext;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartDownloadUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;

@SdkInternalApi
public final class ResumableRequestConverter {
    private static final Logger log = Logger.loggerFor(S3TransferManager.class);

    private ResumableRequestConverter() {
    }

    /**
     * Converts a {@link ResumableFileDownload} to {@link DownloadFileRequest} and {@link AsyncResponseTransformer} pair.
     * <p>
     * If before resuming the download the file on disk was modified, or the s3 object was modified, we need to restart the
     * download from the beginning.
     * <p>
     * If the original requests has some individual parts downloaded, we need to make a multipart GET for the remaining parts.
     * <p>
     * Else, we need to make a ranged GET for the remaining bytes.
     */
    public static Pair<DownloadFileRequest, AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>>
        toDownloadFileRequestAndTransformer(ResumableFileDownload resumableFileDownload,
                                        HeadObjectResponse headObjectResponse,
                                        DownloadFileRequest originalDownloadRequest) {

        GetObjectRequest getObjectRequest = originalDownloadRequest.getObjectRequest();
        DownloadFileRequest newDownloadFileRequest;

        if (!canResumeDownload(resumableFileDownload, headObjectResponse)) {
            // modification detected: new download request for the whole object from the beginning
            newDownloadFileRequest = newDownloadFileRequest(originalDownloadRequest, getObjectRequest, headObjectResponse);

            AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer =
                fileAsyncResponseTransformer(newDownloadFileRequest, false);
            return Pair.of(newDownloadFileRequest, responseTransformer);
        }

        if (hasRemainingParts(getObjectRequest)) {
            log.debug(() -> "The paused download was performed with part GET, now resuming download of remaining parts");
            AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer =
                AsyncResponseTransformer.toFile(originalDownloadRequest.destination(),
                                                FileTransformerConfiguration.builder()
                                                                            .fileWriteOption(WRITE_TO_POSITION)
                                                                            .position(0L)
                                                                            .failureBehavior(LEAVE)
                                                                            .build());
            return Pair.of(originalDownloadRequest, responseTransformer);
        }

        log.debug(() -> "The paused download was performed with range GET, now resuming download of remaining bytes.");
        newDownloadFileRequest = resumedDownloadFileRequest(resumableFileDownload,
                                                            originalDownloadRequest,
                                                            getObjectRequest,
                                                            headObjectResponse);
        if (newDownloadFileRequest == null) {
            log.debug(() -> "Could not compute resumed range for the original range '"
                            + getObjectRequest.range()
                            + "'. The SDK will re-download the object from the beginning.");
            newDownloadFileRequest = newDownloadFileRequest(originalDownloadRequest, getObjectRequest, headObjectResponse);
            AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer =
                fileAsyncResponseTransformer(newDownloadFileRequest, false);
            return Pair.of(newDownloadFileRequest, responseTransformer);
        }
        AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer =
            fileAsyncResponseTransformer(newDownloadFileRequest, true);
        return Pair.of(newDownloadFileRequest, responseTransformer);
    }

    /**
     * Determines whether a paused download can continue from where it left off, or whether it has to be restarted from the
     * beginning because either the S3 object or the local file was modified while the download was paused. Logs the reason at
     * debug level when a restart is required.
     *
     * @return true if the remaining bytes can be fetched, false if the whole object must be downloaded again
     */
    public static boolean canResumeDownload(ResumableFileDownload resumableFileDownload,
                                            HeadObjectResponse headObjectResponse) {
        DownloadFileRequest downloadRequest = resumableFileDownload.downloadFileRequest();
        Instant lastModified = resumableFileDownload.s3ObjectLastModified().orElse(null);
        String resumableFileDownloadEtag = resumableFileDownload.s3ObjectEtag().orElse(null);

        String s3ObjectEtag = headObjectResponse.eTag();
        boolean etagModified = resumableFileDownloadEtag != null &&
                               !resumableFileDownloadEtag.equals(s3ObjectEtag);

        boolean s3ObjectModified = !headObjectResponse.lastModified().equals(lastModified);
        boolean fileModified = !fileNotModified(resumableFileDownload.bytesTransferred(),
                                                resumableFileDownload.fileLastModified(),
                                                downloadRequest.destination());

        if (fileModified || s3ObjectModified || etagModified) {
            logIfNeeded(downloadRequest, downloadRequest.getObjectRequest(), fileModified, s3ObjectModified, etagModified);
            return false;
        }
        return true;
    }

    /**
     * Builds the {@link DownloadFileRequest} for resuming a paused download that is being written to the destination file by
     * CRT rather than by an {@link AsyncResponseTransformer}. Unlike
     * {@link #toDownloadFileRequestAndTransformer(ResumableFileDownload, HeadObjectResponse, DownloadFileRequest)} there is no
     * transformer to pair the request with, and no part-GET variant to consider, since the CRT-based client always downloads
     * an object with a single meta request.
     *
     * @param restartFromBeginning whether the whole object should be downloaded again rather than only the remaining bytes
     */
    public static DownloadFileRequest toCrtDownloadFileRequest(ResumableFileDownload resumableFileDownload,
                                                              HeadObjectResponse headObjectResponse,
                                                              DownloadFileRequest originalDownloadRequest,
                                                              boolean restartFromBeginning) {
        GetObjectRequest getObjectRequest = originalDownloadRequest.getObjectRequest();
        if (restartFromBeginning) {
            return newDownloadFileRequest(originalDownloadRequest, getObjectRequest, headObjectResponse);
        }

        log.debug(() -> "Resuming the paused download with a range GET for the remaining bytes.");
        DownloadFileRequest resumed = resumedDownloadFileRequest(resumableFileDownload, originalDownloadRequest,
                                                                  getObjectRequest, headObjectResponse);
        if (resumed == null) {
            log.debug(() -> "Could not compute resumed range for the original range '"
                            + getObjectRequest.range()
                            + "'. The SDK will re-download the object from the beginning.");
            return newDownloadFileRequest(originalDownloadRequest, getObjectRequest, headObjectResponse);
        }
        return resumed;
    }

    private static boolean hasRemainingParts(GetObjectRequest getObjectRequest) {
        Optional<MultipartDownloadResumeContext> optCtx = MultipartDownloadUtils.multipartDownloadResumeContext(getObjectRequest);
        if (!optCtx.isPresent()) {
            return false;
        }
        MultipartDownloadResumeContext ctx = optCtx.get();
        if (ctx.response() != null && ctx.response().partsCount() == null) {
            return false;
        }
        return !ctx.completedParts().isEmpty();
    }

    private static AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> fileAsyncResponseTransformer(
        DownloadFileRequest newDownloadFileRequest,
        boolean shouldAppend) {
        FileTransformerConfiguration fileTransformerConfiguration =
            shouldAppend ? FileTransformerConfiguration.defaultCreateOrAppend() :
            FileTransformerConfiguration.defaultCreateOrReplaceExisting();

        return AsyncResponseTransformer.toFile(newDownloadFileRequest.destination(),
                                               fileTransformerConfiguration);
    }

    private static void logIfNeeded(DownloadFileRequest downloadRequest,
                                    GetObjectRequest getObjectRequest,
                                    boolean fileModified,
                                    boolean s3ObjectModified,
                                    boolean s3ObjectEtagModified) {
        if (log.logger().isDebugEnabled()) {
            if (s3ObjectModified) {
                log.debug(() -> String.format("The requested object in bucket (%s) with key (%s) "
                                              + "has been modified on Amazon S3 since the last "
                                              + "pause. The SDK will download the S3 object from "
                                              + "the beginning",
                                              getObjectRequest.bucket(), getObjectRequest.key()));
            }

            if (fileModified) {
                log.debug(() -> String.format("The file (%s) has been modified since "
                                              + "the last pause. " +
                                              "The SDK will download the requested object in bucket"
                                              + " (%s) with key (%s) from "
                                              + "the "
                                              + "beginning.",
                                              downloadRequest.destination(),
                                              getObjectRequest.bucket(),
                                              getObjectRequest.key()));
            }
            if (s3ObjectEtagModified) {
                log.debug(() -> String.format("The ETag of the requested object in bucket (%s) with key (%s) "
                                              + "has changed since the last "
                                              + "pause. The SDK will download the S3 object from "
                                              + "the beginning",
                                              getObjectRequest.bucket(), getObjectRequest.key()));
            }

        }
    }

    private static DownloadFileRequest resumedDownloadFileRequest(ResumableFileDownload resumableFileDownload,
                                                                  DownloadFileRequest downloadRequest,
                                                                  GetObjectRequest getObjectRequest,
                                                                  HeadObjectResponse headObjectResponse) {
        long bytesTransferred = resumableFileDownload.bytesTransferred();
        String resumedRange = computeResumedRange(getObjectRequest.range(), bytesTransferred,
                                                  headObjectResponse.contentLength());

        if (resumedRange == null) {
            return null;
        }

        GetObjectRequest newGetObjectRequest =
            getObjectRequest.toBuilder()
                            .ifUnmodifiedSince(headObjectResponse.lastModified())
                            .range(resumedRange)
                            .build();

        return downloadRequest.toBuilder()
                              .getObjectRequest(newGetObjectRequest)
                              .build();
    }

    /**
     * Returns {@code true} if the original request has a range header that cannot be parsed for resume
     */
    public static boolean hasUnparseableRange(DownloadFileRequest downloadRequest, long contentLength) {
        String range = downloadRequest.getObjectRequest().range();
        return range != null && parseRange(range, contentLength) == null;
    }

    /**
     * Computes the Range header for a resumed download. Returns {@code null} if the original range
     * cannot be parsed and the download should restart from the beginning.
     */
    private static String computeResumedRange(String originalRange, long bytesTransferred, long contentLength) {
        if (originalRange != null) {
            long[] parsedRange = parseRange(originalRange, contentLength);
            if (parsedRange != null) {
                long originalStart = parsedRange[0];
                long originalEnd = parsedRange[1];
                return "bytes=" + (originalStart + bytesTransferred) + "-" + originalEnd;
            }
            // Range was present but could not be parsed (suffix range, multi-range, or malformed).
            // We cannot safely compute the resumed offset, so signal the caller to restart from the beginning.
            return null;
        }
        return "bytes=" + bytesTransferred + "-" + contentLength;
    }

    /**
     * Parses a "bytes=start-end" or "bytes=start-" range header into [start, end].
     * Open-ended ranges use contentLength - 1 as the end.
     * Returns null for suffix ranges ("bytes=-500") or malformed values.
     */
    private static long[] parseRange(String range, long contentLength) {
        if (range == null || !range.startsWith("bytes=")) {
            return null;
        }
        String rangeValue = range.substring("bytes=".length());
        int dashIndex = rangeValue.indexOf('-');
        if (dashIndex <= 0) {
            return null;
        }
        try {
            long start = Long.parseLong(rangeValue.substring(0, dashIndex));
            String endPart = rangeValue.substring(dashIndex + 1);
            long end = endPart.isEmpty() ? contentLength - 1 : Long.parseLong(endPart);
            return new long[]{start, end};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static DownloadFileRequest newDownloadFileRequest(DownloadFileRequest originalDownloadRequest,
                                                              GetObjectRequest getObjectRequest,
                                                              HeadObjectResponse headObjectResponse) {
        return originalDownloadRequest.toBuilder()
                                      .getObjectRequest(
                                          getObjectRequest.toBuilder()
                                                          .ifUnmodifiedSince(headObjectResponse.lastModified()).build())
                                      .build();
    }
}
