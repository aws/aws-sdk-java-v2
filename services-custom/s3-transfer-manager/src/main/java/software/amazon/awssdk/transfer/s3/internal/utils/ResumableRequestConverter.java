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

import static software.amazon.awssdk.transfer.s3.internal.utils.FileUtils.fileNotModified;

import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.FileTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
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
     */
    public static Pair<DownloadFileRequest, AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>>
            toDownloadFileRequestAndTransformer(ResumableFileDownload resumableFileDownload,
                                                HeadObjectResponse headObjectResponse,
                                                DownloadFileRequest originalDownloadRequest) {

        GetObjectRequest getObjectRequest = originalDownloadRequest.getObjectRequest();
        DownloadFileRequest newDownloadFileRequest;
        boolean shouldAppend;
        Instant lastModified = resumableFileDownload.s3ObjectLastModified().orElse(null);
        boolean s3ObjectNotModified = headObjectResponse.lastModified().equals(lastModified);

        boolean fileNotModified = fileNotModified(resumableFileDownload.bytesTransferred(),
            resumableFileDownload.fileLastModified(), resumableFileDownload.downloadFileRequest().destination());

        if (fileNotModified && s3ObjectNotModified) {
            newDownloadFileRequest = resumedDownloadFileRequest(resumableFileDownload,
                                                                originalDownloadRequest,
                                                                getObjectRequest,
                                                                headObjectResponse);
            shouldAppend = true;
        } else {
            logIfNeeded(originalDownloadRequest, getObjectRequest, fileNotModified, s3ObjectNotModified);
            shouldAppend = false;
            newDownloadFileRequest = newDownloadFileRequest(originalDownloadRequest, getObjectRequest,
                                                            headObjectResponse);
        }

        AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer =
            fileAsyncResponseTransformer(newDownloadFileRequest, shouldAppend);
        return Pair.of(newDownloadFileRequest, responseTransformer);
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
                                    boolean fileNotModified,
                                    boolean s3ObjectNotModified) {
        if (log.logger().isDebugEnabled()) {
            if (!s3ObjectNotModified) {
                log.debug(() -> String.format("The requested object in bucket (%s) with key (%s) "
                                              + "has been modified on Amazon S3 since the last "
                                              + "pause. The SDK will download the S3 object from "
                                              + "the beginning",
                                              getObjectRequest.bucket(), getObjectRequest.key()));
            }

            if (!fileNotModified) {
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
