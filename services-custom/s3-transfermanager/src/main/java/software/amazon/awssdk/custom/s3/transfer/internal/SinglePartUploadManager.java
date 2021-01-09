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

package software.amazon.awssdk.custom.s3.transfer.internal;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.custom.s3.transfer.UploadObjectSpecification;
import software.amazon.awssdk.custom.s3.transfer.UploadRequest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@SdkInternalApi
@ThreadSafe
final class SinglePartUploadManager {
    private final S3AsyncClient s3Client;

    SinglePartUploadManager(S3AsyncClient s3Client) {
        this.s3Client = s3Client;
    }

    CompletableFuture<Void> apiRequestUpload(UploadRequest request, TransferRequestBody requestBody) {
        UploadObjectSpecification uploadSpecification = request.uploadSpecification();
        PutObjectRequest putObjectRequest = uploadSpecification.asApiRequest();

        SinglePartUploadContext ctx = SinglePartUploadContext.builder()
                                                             .putObjectRequest(putObjectRequest)
                                                             .uploadRequest(request)
                                                             .build();

        try {
            return s3Client.putObject(putObjectRequest, requestBody.requestBodyForObject(ctx))
                           .thenApply(ignore -> null);
        } catch (Throwable t) {
            return CompletableFutureUtils.failedFuture(t);
        }
    }
}
