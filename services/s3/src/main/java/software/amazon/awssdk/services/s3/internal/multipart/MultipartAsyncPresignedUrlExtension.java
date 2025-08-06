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

package software.amazon.awssdk.services.s3.internal.multipart;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.AsyncPresignedUrlExtension;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.utils.Validate;

/**
 * An {@link AsyncPresignedUrlExtension} that automatically converts presigned URL downloads
 * to multipart downloads.
 */
@SdkInternalApi
public class MultipartAsyncPresignedUrlExtension implements AsyncPresignedUrlExtension {
    private final PresignedUrlDownloadHelper downloadHelper;

    public MultipartAsyncPresignedUrlExtension(
            S3AsyncClient s3AsyncClient,
            AsyncPresignedUrlExtension delegate,
            long bufferSizeInBytes,
            long partSizeInBytes) {
        Validate.paramNotNull(s3AsyncClient, "s3AsyncClient");
        Validate.paramNotNull(delegate, "delegate");
        this.downloadHelper = new PresignedUrlDownloadHelper(
                s3AsyncClient,
                delegate,
                bufferSizeInBytes,
                partSizeInBytes);
    }

    @Override
    public <ReturnT> CompletableFuture<ReturnT> getObject(
            PresignedUrlDownloadRequest presignedUrlDownloadRequest,
            AsyncResponseTransformer<GetObjectResponse, ReturnT> asyncResponseTransformer) {
        Validate.paramNotNull(presignedUrlDownloadRequest, "presignedUrlDownloadRequest");
        Validate.paramNotNull(asyncResponseTransformer, "asyncResponseTransformer");
        return downloadHelper.downloadObject(presignedUrlDownloadRequest, asyncResponseTransformer);
    }
}