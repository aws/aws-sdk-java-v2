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
        Instant lastModified = resumableFileDownload.s3ObjectLastModified().orElse(null);
        boolean s3ObjectModified = !headObjectResponse.lastModified().equals(lastModified);

        boolean fileModified = !fileNotModified(resumableFileDownload.bytesTransferred(),
                                                resumableFileDownload.fileLastModified(),
                                                resumableFileDownload.downloadFileRequest().destination());

        if (fileModified || s3ObjectModified) {
            // modification detected: new download request for the whole object from the beginning
            logIfNeeded(originalDownloadRequest, getObjectRequest, fileModified, s3ObjectModified);
            newDownloadFileRequest = newDownloadFileRequest(originalDownloadRequest, getObjectRequest, headObjectResponse);

            AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer =
                fileAsyncResponseTransformer(newDownloadFileRequest, false);
            return Pair.of(newDownloadFileRequest, responseTransformer);
        }

        if (hasRemainingParts(getObjectRequest)) {
            log.debug(() -> "The paused download was performed with part GET, now resuming download of remaining parts");
            Long positionToWriteFrom =
                MultipartDownloadUtils.multipartDownloadResumeContext(originalDownloadRequest.getObjectRequest())
                .map(MultipartDownloadResumeContext::bytesToLastCompletedParts)
                .orElse(0L);
            AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer =
                AsyncResponseTransformer.toFile(originalDownloadRequest.destination(),
                                                FileTransformerConfiguration.builder()
                                                                            .fileWriteOption(WRITE_TO_POSITION)
                                                                            .position(positionToWriteFrom)
                                                                            .failureBehavior(LEAVE)
                                                                            .build());
            return Pair.of(originalDownloadRequest, responseTransformer);
        }

        log.debug(() -> "The paused download was performed with range GET, now resuming download of remaining bytes.");
        newDownloadFileRequest = resumedDownloadFileRequest(resumableFileDownload,
                                                            originalDownloadRequest,
                                                            getObjectRequest,
                                                            headObjectResponse);
        AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer =
            fileAsyncResponseTransformer(newDownloadFileRequest, true);
        return Pair.of(newDownloadFileRequest, responseTransformer);
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
                                    boolean s3ObjectModified) {
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
        }
    }

    private static DownloadFileRequest resumedDownloadFileRequest(ResumableFileDownload resumableFileDownload,
                                                                  DownloadFileRequest downloadRequest,
                                                                  GetObjectRequest getObjectRequest,
                                                                  HeadObjectResponse headObjectResponse) {
        DownloadFileRequest newDownloadFileRequest;
        long bytesTransferred = resumableFileDownload.bytesTransferred();
        GetObjectRequest newGetObjectRequest =
            getObjectRequest.toBuilder()
                            .ifUnmodifiedSince(headObjectResponse.lastModified())
                            .range("bytes=" + bytesTransferred + "-" + headObjectResponse.contentLength())
                            .build();

        newDownloadFileRequest = downloadRequest.toBuilder()
                                                .getObjectRequest(newGetObjectRequest)
                                                .build();
        return newDownloadFileRequest;
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
