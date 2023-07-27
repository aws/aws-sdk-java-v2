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
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.DelegatingS3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.utils.Validate;

// This is just a temporary class for testing
//TODO: change this
@SdkInternalApi
public class MultipartS3AsyncClient extends DelegatingS3AsyncClient {

    public static final ApiName USER_AGENT_API_NAME = ApiName.builder().name("hll").version("s3Multipart").build();

    private static final long DEFAULT_MIN_PART_SIZE_IN_BYTES = 8L * 1024 * 1024;
    private static final long DEFAULT_PART_SIZE_IN_BYTES = 8L * 1024 * 1024;
    private static final long DEFAULT_THRESHOLD = 8L * 1024 * 1024;

    private static final long DEFAULT_MAX_MEMORY = DEFAULT_PART_SIZE_IN_BYTES * 2;
    private final UploadObjectHelper mpuHelper;
    private final CopyObjectHelper copyObjectHelper;

    public MultipartS3AsyncClient(S3AsyncClient delegate, MultipartConfiguration multipartConfiguration) {
        super(delegate);
        MultipartConfiguration validConfiguration = Validate.getOrDefault(multipartConfiguration,
                                                                          MultipartConfiguration.builder()::build);
        long minPartSizeInBytes = Validate.getOrDefault(validConfiguration.minimumPartSizeInBytes(),
                                                        () -> DEFAULT_MIN_PART_SIZE_IN_BYTES);
        long threshold = Validate.getOrDefault(validConfiguration.thresholdInBytes(),
                                               () -> DEFAULT_THRESHOLD);
        long maximumMemoryUsageInBytes = Validate.getOrDefault(validConfiguration.maximumMemoryUsageInBytes(),
                                                               () -> computeMaxMemoryUsage(validConfiguration));
        mpuHelper = new UploadObjectHelper(delegate, minPartSizeInBytes, threshold, maximumMemoryUsageInBytes);
        copyObjectHelper = new CopyObjectHelper(delegate, minPartSizeInBytes, threshold);
    }

    private long computeMaxMemoryUsage(MultipartConfiguration multipartConfiguration) {
        return multipartConfiguration.minimumPartSizeInBytes() != null ? multipartConfiguration.minimumPartSizeInBytes() * 2
                                                                       : DEFAULT_MAX_MEMORY;
    }

    @Override
    public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest putObjectRequest, AsyncRequestBody requestBody) {
        return mpuHelper.uploadObject(putObjectRequest, requestBody);
    }

    @Override
    public CompletableFuture<CopyObjectResponse> copyObject(CopyObjectRequest copyObjectRequest) {
        return copyObjectHelper.copyObject(copyObjectRequest);
    }

    @Override
    public void close() {
        delegate().close();
    }
}
