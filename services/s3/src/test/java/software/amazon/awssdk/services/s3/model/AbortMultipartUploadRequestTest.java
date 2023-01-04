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

package software.amazon.awssdk.services.s3.model;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

class AbortMultipartUploadRequestTest extends S3RequestTestBase<AbortMultipartUploadRequest> {

    @Override
    AbortMultipartUploadRequest s3RequestWithUploadId(String uploadId) {
        return abortMultipartUploadRequestWithUploadId(uploadId);
    }

    @Override
    AbortMultipartUploadResponse performRequest(S3Client client, AbortMultipartUploadRequest request) {
        return client.abortMultipartUpload(request);
    }

    @Override
    CompletableFuture<AbortMultipartUploadResponse> performRequestAsync(S3AsyncClient client, AbortMultipartUploadRequest request) {
        return client.abortMultipartUpload(request);
    }

    private static AbortMultipartUploadRequest abortMultipartUploadRequestWithUploadId(String uploadId) {
        return AbortMultipartUploadRequest.builder()
                                          .bucket("mybucket")
                                          .key("mykey")
                                          .uploadId(uploadId)
                                          .build();
    }

}