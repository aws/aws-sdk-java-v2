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
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.DelegatingS3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

// This is just a temporary class for testing
//TODO: change this
@SdkInternalApi
public class MultipartS3AsyncClient extends DelegatingS3AsyncClient {
    private static final long DEFAULT_PART_SIZE_IN_BYTES = 8L * 1024 * 1024;
    private static final long DEFAULT_THRESHOLD = 8L * 1024 * 1024;

    private static final long DEFAULT_MAX_MEMORY = DEFAULT_PART_SIZE_IN_BYTES * 2;
    private final MultipartUploadHelper mpuHelper;

    public MultipartS3AsyncClient(S3AsyncClient delegate) {
        super(delegate);
        // TODO: pass a config object to the upload helper instead
        mpuHelper = new MultipartUploadHelper(delegate, DEFAULT_PART_SIZE_IN_BYTES, DEFAULT_THRESHOLD, DEFAULT_MAX_MEMORY);
    }

    @Override
    public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest putObjectRequest, AsyncRequestBody requestBody) {
        return mpuHelper.uploadObject(putObjectRequest, requestBody);
    }
}
