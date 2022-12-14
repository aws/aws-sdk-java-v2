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
import org.apache.commons.lang3.RandomStringUtils;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

class UploadPartRequestTest extends S3RequestTestBase<UploadPartRequest> {

    static final RequestBody REQUEST_BODY = RequestBody.fromString(RandomStringUtils.randomAscii(1_000));
    static final AsyncRequestBody ASYNC_REQUEST_BODY = AsyncRequestBody.fromString(RandomStringUtils.randomAscii(1_000));

    @Override
    UploadPartRequest s3RequestWithUploadId(String uploadId) {
        return uploadPartRequestWithUploadId(uploadId);
    }

    @Override
    UploadPartResponse performRequest(S3Client client, UploadPartRequest request) {
        return client.uploadPart(request, REQUEST_BODY);
    }

    @Override
    CompletableFuture<UploadPartResponse> performRequestAsync(S3AsyncClient client, UploadPartRequest request) {
        return client.uploadPart(request, ASYNC_REQUEST_BODY);
    }

    private static UploadPartRequest uploadPartRequestWithUploadId(String uploadId) {
        return UploadPartRequest.builder()
                                .bucket("mybucket")
                                .key("mykey")
                                .uploadId(uploadId)
                                .partNumber(1)
                                .build();
    }

}