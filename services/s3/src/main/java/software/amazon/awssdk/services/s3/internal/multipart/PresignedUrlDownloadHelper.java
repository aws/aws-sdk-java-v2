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
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.AsyncPresignedUrlExtension;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public class PresignedUrlDownloadHelper {
    private static final Logger log = Logger.loggerFor(PresignedUrlDownloadHelper.class);

    private final S3AsyncClient s3AsyncClient;
    private final AsyncPresignedUrlExtension asyncPresignedUrlExtension;
    private final long bufferSizeInBytes;
    private final long configuredPartSizeInBytes;

    public PresignedUrlDownloadHelper(S3AsyncClient s3AsyncClient,
                                      AsyncPresignedUrlExtension asyncPresignedUrlExtension,
                                      long bufferSizeInBytes,
                                      long configuredPartSizeInBytes) {
        this.s3AsyncClient = Validate.paramNotNull(s3AsyncClient, "s3AsyncClient");
        this.asyncPresignedUrlExtension = Validate.paramNotNull(asyncPresignedUrlExtension, "asyncPresignedUrlExtension");
        this.bufferSizeInBytes = Validate.isPositive(bufferSizeInBytes, "bufferSizeInBytes");
        this.configuredPartSizeInBytes = Validate.isPositive(configuredPartSizeInBytes, "configuredPartSizeInBytes");
    }

    public <T> CompletableFuture<T> downloadObject(
        PresignedUrlDownloadRequest presignedRequest,
        AsyncResponseTransformer<GetObjectResponse, T> asyncResponseTransformer) {

        Validate.paramNotNull(presignedRequest, "presignedRequest");
        Validate.paramNotNull(asyncResponseTransformer, "asyncResponseTransformer");

        if (presignedRequest.range() != null) {
            log.debug(() -> "Using single part download because presigned URL request range is included in the request. range = "
                            + presignedRequest.range());
            return asyncPresignedUrlExtension.getObject(presignedRequest, asyncResponseTransformer);
        }

        SplittingTransformerConfiguration splittingConfig = SplittingTransformerConfiguration.builder()
                                                                                             .bufferSizeInBytes(bufferSizeInBytes)
                                                                                             .build();
        AsyncResponseTransformer.SplitResult<GetObjectResponse, T> split =
            asyncResponseTransformer.split(splittingConfig);
        // TODO: PresignedUrlMultipartDownloaderSubscriber needs to be implemented in next PR
        // PresignedUrlMultipartDownloaderSubscriber subscriber =
        //     new PresignedUrlMultipartDownloaderSubscriber(
        //         s3AsyncClient,
        //         presignedRequest,
        //         configuredPartSizeInBytes);
        //
        // split.publisher().subscribe(subscriber);
        // return split.resultFuture();
        throw new UnsupportedOperationException("Multipart presigned URL download not yet implemented - TODO in next PR");
    }
}
