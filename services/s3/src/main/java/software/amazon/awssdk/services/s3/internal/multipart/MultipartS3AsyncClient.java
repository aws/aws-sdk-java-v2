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
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.DelegatingS3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;
import software.amazon.awssdk.utils.Validate;

// This is just a temporary class for testing
//TODO: change this
@SdkInternalApi
public final class MultipartS3AsyncClient extends DelegatingS3AsyncClient {

    private static final long DEFAULT_MIN_PART_SIZE_IN_BYTES = 8L * 1024 * 1024;
    private static final long DEFAULT_THRESHOLD = 8L * 1024 * 1024;

    private static final long DEFAULT_MAX_MEMORY = DEFAULT_MIN_PART_SIZE_IN_BYTES * 2;
    private final MultipartUploadHelper mpuHelper;
    private final CopyObjectHelper copyObjectHelper;
    private final Executor executor;

    public MultipartS3AsyncClient(S3AsyncClient delegate, MultipartConfiguration multipartConfiguration) {
        super(delegate);
        long minPartSizeInBytes = Validate.getOrDefault(multipartConfiguration.minimumPartSizeInBytes(),
                                                        () -> DEFAULT_MIN_PART_SIZE_IN_BYTES);
        long threshold = Validate.getOrDefault(multipartConfiguration.thresholdInBytes(),
                                               () -> DEFAULT_THRESHOLD);
        long maximumMemoryUsageInBytes = Validate.getOrDefault(multipartConfiguration.maximumMemoryUsageInBytes(),
                                                               () -> computeMaxMemoryUsage(multipartConfiguration));
        this.executor = Validate.getOrDefault(multipartConfiguration.executor(), this::defaultExecutor);
        this.mpuHelper = new MultipartUploadHelper(delegate, minPartSizeInBytes, threshold, maximumMemoryUsageInBytes);
        this.copyObjectHelper = new CopyObjectHelper(delegate, minPartSizeInBytes, threshold);
    }

    private long computeMaxMemoryUsage(MultipartConfiguration multipartConfiguration) {
        return multipartConfiguration.minimumPartSizeInBytes() != null ? multipartConfiguration.minimumPartSizeInBytes() * 2
                                                                       : DEFAULT_MAX_MEMORY;
    }

    private Executor defaultExecutor() {
        int maxPoolSize = 100;
        ThreadPoolExecutor defaultExecutor = new ThreadPoolExecutor(0, maxPoolSize,
                                                                    60, TimeUnit.SECONDS,
                                                                    new LinkedBlockingQueue<>(1_000),
                                                                    new ThreadFactoryBuilder()
                                                                        .threadNamePrefix("s3-multipart-async-client").build());
        // Allow idle core threads to time out
        defaultExecutor.allowCoreThreadTimeOut(true);
        return defaultExecutor;
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
