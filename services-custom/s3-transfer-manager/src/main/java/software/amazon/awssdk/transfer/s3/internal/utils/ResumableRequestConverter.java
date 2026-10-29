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
import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.MULTIPART_DOWNLOAD_RESUME_CONTEXT;
import static software.amazon.awssdk.transfer.s3.internal.utils.FileUtils.fileNotModified;

import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
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
     * download from the beginning. The same applies if individual parts had completed but the multipart download state that
     * records them is not available, for example because the {@link ResumableFileDownload} was serialized.
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

        if (shouldRestartDownload(resumableFileDownload, headObjectResponse, getObjectRequest)) {
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
            return Pair.of(attachIfMatch(originalDownloadRequest, resumableFileDownload), responseTransformer);
        }

        return resumeWithRangeGetIfPossible(resumableFileDownload, headObjectResponse, originalDownloadRequest);
    }

    /**
     * Resumes the paused download by fetching the remaining bytes with one range GET, appended to the destination file. That
     * is only correct when the paused download wrote the file in order: a single GET (a non-multipart client, or a user
     * range), or a multipart download paused before its first part completed. Tokens with completed parts are handled
     * before this is called. If a user-specified range cannot be parsed or has already been fully downloaded, the download
     * restarts from the beginning instead.
     */
    private static Pair<DownloadFileRequest, AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>>
        resumeWithRangeGetIfPossible(ResumableFileDownload resumableFileDownload,
                           HeadObjectResponse headObjectResponse,
                           DownloadFileRequest originalDownloadRequest) {
        GetObjectRequest getObjectRequest = originalDownloadRequest.getObjectRequest();
        log.debug(() -> "The paused download was performed with range GET, now resuming download of remaining bytes.");
        DownloadFileRequest newDownloadFileRequest = resumedDownloadFileRequest(resumableFileDownload,
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
        newDownloadFileRequest = attachIfMatch(newDownloadFileRequest, resumableFileDownload);
        AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer =
            fileAsyncResponseTransformer(newDownloadFileRequest, true);
        return Pair.of(newDownloadFileRequest, responseTransformer);
    }

    /**
     * Determines whether a paused download has to be restarted from the beginning rather than continued from where it left
     * off.
     */
    private static boolean shouldRestartDownload(ResumableFileDownload resumableFileDownload,
                                                 HeadObjectResponse headObjectResponse,
                                                 GetObjectRequest getObjectRequest) {
        // partNumber and Range cannot coexist, S3 rejects with 400 InvalidRequest, so a part download is restarted with
        // CREATE_OR_REPLACE.
        return getObjectRequest.partNumber() != null
               || isObjectOrFileModified(resumableFileDownload, headObjectResponse)
               || completedPartsMissingFromResumeContext(resumableFileDownload, getObjectRequest);
    }

    /**
     * Determines whether the S3 object or the local file was modified while the download was paused, in which case the
     * download has to be restarted from the beginning. Logs the reason at debug level.
     *
     * @return true if the whole object must be downloaded again, false if the remaining bytes can be fetched
     */
    public static boolean isObjectOrFileModified(ResumableFileDownload resumableFileDownload,
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
            return true;
        }
        return false;
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

    /**
     * Pins the resumed request to the ETag of the object that was being downloaded before the pause, so S3 rejects the request
     * with 412 rather than serving bytes of a replacement object. Any If-Match on the original request is replaced: the token
     * ETag came from a response that already satisfied it. If the token has no ETag, the request is returned unchanged.
     */
    private static DownloadFileRequest attachIfMatch(DownloadFileRequest downloadRequest,
                                                     ResumableFileDownload resumableFileDownload) {
        String s3ObjectEtag = resumableFileDownload.s3ObjectEtag().orElse(null);
        if (s3ObjectEtag == null) {
            return downloadRequest;
        }
        GetObjectRequest getObjectRequest = downloadRequest.getObjectRequest();
        return downloadRequest.toBuilder()
                              .getObjectRequest(getObjectRequest.toBuilder().ifMatch(s3ObjectEtag).build())
                              .build();
    }

    /**
     * Returns true if the {@link ResumableFileDownload} lists completed parts but the {@link MultipartDownloadResumeContext}
     * on its request does not record them, for example because the token was serialized. The paused download fetched parts
     * that are written at their own offsets, so the file may have gaps, and the download restarts from the beginning.
     */
    private static boolean completedPartsMissingFromResumeContext(ResumableFileDownload resumableFileDownload,
                                                                  GetObjectRequest getObjectRequest) {
        boolean tokenHasCompletedParts = !resumableFileDownload.completedParts().isEmpty();
        boolean resumeContextHasCompletedParts =
            MultipartDownloadUtils.multipartDownloadResumeContext(getObjectRequest)
                                  .map(ctx -> !ctx.completedParts().isEmpty())
                                  .orElse(false);
        if (!tokenHasCompletedParts || resumeContextHasCompletedParts) {
            return false;
        }
        log.debug(() -> "The paused download had completed individual parts, but the resume context that records them is "
                        + "not available, for example because the ResumableFileDownload was serialized. The SDK will download "
                        + "the S3 object from the beginning.");
        return true;
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
     * Returns {@code true} if the original request has a range that cannot be safely resumed, because it cannot be parsed or
     * has already been fully downloaded, so the download has to restart from the beginning.
     */
    public static boolean hasUnresumableRange(DownloadFileRequest downloadRequest, long bytesTransferred,
                                              long contentLength) {
        return computeResumedRange(downloadRequest.getObjectRequest().range(), bytesTransferred, contentLength) == null;
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
                long resumedStart = originalStart + bytesTransferred;
                if (resumedStart > originalEnd) {
                    return null;
                }
                return "bytes=" + resumedStart + "-" + originalEnd;
            }
            // Range was present but could not be parsed (multi-range or malformed).
            // We cannot safely compute the resumed offset, so signal the caller to restart from the beginning.
            return null;
        }
        return "bytes=" + bytesTransferred + "-" + contentLength;
    }

    /**
     * Parses a "bytes=start-end", "bytes=start-", or "bytes=-suffix" range header into [start, end].
     * Open-ended ranges use contentLength - 1 as the end.
     * Suffix ranges are resolved to absolute offsets using contentLength.
     * Returns null for multi-range values or malformed input.
     */
    private static long[] parseRange(String range, long contentLength) {
        if (range == null || !range.startsWith("bytes=")) {
            return null;
        }
        String rangeValue = range.substring("bytes=".length());
        int dashIndex = rangeValue.indexOf('-');
        if (dashIndex < 0) {
            return null;
        }
        if (dashIndex == 0) {
            try {
                long suffixLength = Long.parseLong(rangeValue.substring(1));
                if (suffixLength <= 0) {
                    return null;
                }
                long start = contentLength - suffixLength;
                return new long[]{Math.max(0, start), contentLength - 1};
            } catch (NumberFormatException e) {
                return null;
            }
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

    /**
     * Builds the request for downloading the object from the beginning. It gets a new {@link MultipartDownloadResumeContext},
     * otherwise the multipart client would skip the parts the paused download had completed.
     */
    private static DownloadFileRequest newDownloadFileRequest(DownloadFileRequest originalDownloadRequest,
                                                              GetObjectRequest getObjectRequest,
                                                              HeadObjectResponse headObjectResponse) {
        AwsRequestOverrideConfiguration overrideConfiguration =
            getObjectRequest.overrideConfiguration()
                            .map(AwsRequestOverrideConfiguration::toBuilder)
                            .orElseGet(AwsRequestOverrideConfiguration::builder)
                            .putExecutionAttribute(MULTIPART_DOWNLOAD_RESUME_CONTEXT, new MultipartDownloadResumeContext())
                            .build();
        return originalDownloadRequest.toBuilder()
                                      .getObjectRequest(
                                          getObjectRequest.toBuilder()
                                                          .ifUnmodifiedSince(headObjectResponse.lastModified())
                                                          .overrideConfiguration(overrideConfiguration)
                                                          .build())
                                      .build();
    }
}
