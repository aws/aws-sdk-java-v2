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

class ListPartsRequestTest extends S3RequestTestBase<ListPartsRequest> {

    @Override
    ListPartsRequest s3RequestWithUploadId(String uploadId) {
        return listPartsRequestWithUploadId(uploadId);
    }

    @Override
    ListPartsResponse performRequest(S3Client client, ListPartsRequest request) {
        return client.listParts(request);
    }

    @Override
    CompletableFuture<ListPartsResponse> performRequestAsync(S3AsyncClient client, ListPartsRequest request) {
        return client.listParts(request);
    }

    private static ListPartsRequest listPartsRequestWithUploadId(String uploadId) {
        return ListPartsRequest.builder()
                               .bucket("mybucket")
                               .key("mykey")
                               .uploadId(uploadId)
                               .build();
    }
}